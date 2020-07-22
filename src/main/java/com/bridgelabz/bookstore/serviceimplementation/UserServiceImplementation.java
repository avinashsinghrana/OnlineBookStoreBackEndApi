package com.bridgelabz.bookstore.serviceimplementation;

import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import com.bridgelabz.bookstore.dto.*;
import com.bridgelabz.bookstore.model.*;
import com.bridgelabz.bookstore.repository.*;
import com.bridgelabz.bookstore.response.UserAddressDetailsResponse;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.bridgelabz.bookstore.exception.UserException;
import com.bridgelabz.bookstore.exception.UserNotFoundException;
import com.bridgelabz.bookstore.exception.UserVerificationException;
import com.bridgelabz.bookstore.response.EmailObject;
import com.bridgelabz.bookstore.response.Response;
import com.bridgelabz.bookstore.response.UserDetailsResponse;
import com.bridgelabz.bookstore.service.UserService;
import com.bridgelabz.bookstore.utility.JwtGenerator;
import com.bridgelabz.bookstore.utility.RabbitMQSender;
import com.bridgelabz.bookstore.utility.RedisTempl;

import static java.util.stream.Collectors.toList;

@Service
@PropertySource(name = "user", value = {"classpath:response.properties"})
public class UserServiceImplementation implements UserService {
    private static long orderIdGlobal;

    @Autowired
    private SellerRepository sellerRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserDetailsRepository userDetailsRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private Environment environment;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private RabbitMQSender rabbitMQSender;

    @Autowired
    private RedisTempl<Object> redis;

    private String redisKey = "Key";

    private static final long REGISTRATION_EXP = (long) 10800000;
    private static final String VERIFICATION_URL = "http://localhost:8081/user/verify/";
    private static final String RESETPASSWORD_URL = "http://localhost:8081/user/resetpassword/";

    @Override
    public boolean register(RegistrationDto registrationDto) throws UserException {
        UserModel userModel = userRepository.findByEmailId(registrationDto.getEmailId());
        if (userModel != null) {
            return false;
        } else {
            UserModel userDetails = new UserModel();
            BeanUtils.copyProperties(registrationDto, userDetails);
            userDetails.setPassword(bCryptPasswordEncoder.encode(userDetails.getPassword()));
            userRepository.save(userDetails);
            UserModel sendMail = userRepository.findByEmailId(registrationDto.getEmailId());
            String response = VERIFICATION_URL + JwtGenerator.createJWT(sendMail.getUserId(), REGISTRATION_EXP);
            redis.putMap(redisKey, userDetails.getEmailId(), userDetails.getFullName());
            switch (registrationDto.getRoleType()) {
                case SELLER:
                    SellerModel sellerDetails = new SellerModel();
                    sellerDetails.setSellerName(registrationDto.getFullName());
                    sellerDetails.setEmailId(registrationDto.getEmailId());
                    sellerRepository.save(sellerDetails);
                    break;
                case ADMIN:
                    AdminModel adminDetails = new AdminModel();
                    adminDetails.setAdminName(registrationDto.getFullName());
                    adminDetails.setEmailId(registrationDto.getEmailId());
                    adminRepository.save(adminDetails);
                    break;
            }
            if (rabbitMQSender.send(new EmailObject(sendMail.getEmailId(), "Registration Link...", response, "Link for Verification")))
                return true;
        }
        throw new UserException(environment.getProperty("user.invalidcredentials"), HttpStatus.FORBIDDEN);
    }

    @Override
    public boolean verify(String token) {
        long id = JwtGenerator.decodeJWT(token);
        UserModel userInfo = userRepository.findByUserId(id);
        if (id > 0 && userInfo != null) {
            if (!userInfo.isVerified()) {
                userInfo.setVerified(true);
                userInfo.setUpdatedAt(ZonedDateTime.now());
                userRepository.save(userInfo);
                return true;
            }
            throw new UserVerificationException(HttpStatus.CREATED.value(),
                    environment.getProperty("user.already.verified"));
        }
        return false;
    }

    @Override
    public UserDetailsResponse forgetPassword(ForgotPasswordDto userMail) {
        UserModel isIdAvailable = userRepository.findByEmailId(userMail.getEmailId());
        if (isIdAvailable != null && isIdAvailable.isVerified()) {
            String token = JwtGenerator.createJWT(isIdAvailable.getUserId(), REGISTRATION_EXP);
            String response = RESETPASSWORD_URL + token;
            if (rabbitMQSender.send(new EmailObject(isIdAvailable.getEmailId(), "ResetPassword Link...", response, "Link for Reset Password")))
                return new UserDetailsResponse(HttpStatus.OK.value(), "ResetPassword link Successfully", token);
        }
        return new UserDetailsResponse(HttpStatus.OK.value(), "Email ending failed");
    }

    @Override
    public boolean resetPassword(ResetPasswordDto resetPassword, String token) throws UserNotFoundException {
        if (resetPassword.getNewPassword().equals(resetPassword.getConfirmPassword())) {
            long id = JwtGenerator.decodeJWT(token);
            UserModel isIdAvailable = userRepository.findByUserId(id);
            if (isIdAvailable != null) {
                isIdAvailable.setPassword(bCryptPasswordEncoder.encode((resetPassword.getNewPassword())));
                userRepository.save(isIdAvailable);
                redis.putMap(redisKey, resetPassword.getNewPassword(), token);
                return true;
            }
            throw new UserNotFoundException(environment.getProperty("user.not.exist"));
        }
        return false;
    }

    @Override
    public Response login(LoginDto loginDTO) throws UserNotFoundException, UserException {
        UserModel userCheck = userRepository.findByEmailId(loginDTO.getEmailId());
        if (userCheck == null) {
            throw new UserNotFoundException("user.not.exist");
        }
        if (bCryptPasswordEncoder.matches(loginDTO.getPassword(), userCheck.getPassword())) {
            String token = JwtGenerator.createJWT(userCheck.getUserId(), REGISTRATION_EXP);
            redis.putMap(redisKey, userCheck.getEmailId(), userCheck.getPassword());
            String roleType = String.valueOf(userCheck.getRoleType());
            userCheck.setUserStatus(true);
            userRepository.save(userCheck);
            return new Response(userCheck.getFullName(), HttpStatus.OK.value(), token, roleType);
        }
        throw new UserException(environment.getProperty("user.invalid.credential"));
    }

    @Override
    public Response addToCart(Long bookId, String token) {
        long id = JwtGenerator.decodeJWT(token);
        CartModel cartData = cartRepository.findByUserIdAndBookId(id, bookId);
        if (cartData != null && !cartData.isInWishList()) {
            return new Response(HttpStatus.OK.value(), "Book already added to cart");
        } else if (cartData != null && cartData.isInWishList()) {
            cartData.setInWishList(false);
            cartRepository.save(cartData);
            return new Response(HttpStatus.OK.value(), "Book added to cart successfully,Removed From wishlist");
        } else {
            BookModel bookModel = bookRepository.findByBookId(bookId);
            CartModel cartModel = new CartModel();
            BeanUtils.copyProperties(bookModel, cartModel);
            cartModel.setQuantity(1);
            cartModel.setIpAddress("ASSIGNED WITH USER");
            cartModel.setUserId(id);
            cartModel.setInWishList(false);
            cartRepository.save(cartModel);
            return new Response(environment.getProperty("book.added.to.cart.successfully"), HttpStatus.OK.value(), cartModel);
        }
    }

    @Override
    public Response addToWishList(Long bookId, String token) {
        long id = JwtGenerator.decodeJWT(token);
        CartModel cartData = cartRepository.findByUserIdAndBookId(id, bookId);
        if (cartData != null && cartData.isInWishList()) {
            return new Response(HttpStatus.OK.value(), "Book already present in wishlist");
        } else if (cartData != null && !cartData.isInWishList()) {
            return new Response(HttpStatus.OK.value(), "Book already added to Cart");
        } else {
            BookModel bookModel = bookRepository.findByBookId(bookId);
            CartModel cartModel = new CartModel();
            BeanUtils.copyProperties(bookModel, cartModel);
            cartModel.setIpAddress("ASSIGNED WITH USER");
            cartModel.setQuantity(1);
            cartModel.setUserId(JwtGenerator.decodeJWT(token));
            cartModel.setInWishList(true);
            cartRepository.save(cartModel);
            return new Response(HttpStatus.OK.value(), "Book added to WishList");
        }
    }

    @Override
    public Response deleteFromWishlist(Long bookId, String token) {
        long id = JwtGenerator.decodeJWT(token);
        cartRepository.deleteByUserIdAndBookId(id, bookId);
        return new Response(HttpStatus.OK.value(), "Removed SuccessFully from WishKart");
    }

    @Override
    public Response addFromWishlistToCart(Long bookId, String token) {
        long id = JwtGenerator.decodeJWT(token);
        CartModel cartModel = cartRepository.findByUserIdAndBookId(id, bookId);
        if (cartModel.isInWishList()) {
            cartModel.setInWishList(false);
            cartRepository.save(cartModel);
            return new Response(HttpStatus.OK.value(), "Added SuccessFully To addToKart from wishlist");
        }
        return new Response(HttpStatus.OK.value(), "Already present in cart, ready to checkout");
    }

    @Override
    public Response addMoreItems(Long bookId, String token) {
        long id = JwtGenerator.decodeJWT(token);
        CartModel cartModel = cartRepository.findByUserIdAndBookId(id, bookId);
        BookModel bookModel = bookRepository.findByBookId(bookId);
        if (cartModel.getQuantity() > 0) {
            cartModel.setQuantity(cartModel.getQuantity() + 1);
            cartModel.setPrice(bookModel.getPrice() * cartModel.getQuantity());
            cartRepository.save(cartModel);
        }
        return new Response(environment.getProperty("book.added.to.cart.successfully"), HttpStatus.OK.value(), cartModel);
    }

    @Override
    public Response removeItem(Long bookId, String token) {
        long id = JwtGenerator.decodeJWT(token);
        CartModel cartModel = cartRepository.findByUserIdAndBookId(id, bookId);
        BookModel bookModel = bookRepository.findByBookId(bookId);
        if (cartModel.getQuantity() > 0) {
            cartModel.setQuantity(cartModel.getQuantity() - 1);
            cartModel.setPrice(bookModel.getPrice() * cartModel.getQuantity());
            cartRepository.save(cartModel);
        }
        return new Response(environment.getProperty("one.quantity.removed.success"), HttpStatus.OK.value(), cartModel);
    }

    @Override
    public Response removeAllItem(Long bookId, String token) {
        long id = JwtGenerator.decodeJWT(token);
        cartRepository.deleteByUserIdAndBookId(id, bookId);
        return new Response(HttpStatus.OK.value(), environment.getProperty("quantity.removed.success"));
    }

    @Override
    public List<CartModel> getAllItemFromCart(String token) {
        Long id = JwtGenerator.decodeJWT(token);
        List<CartModel> items = cartRepository.findAllByUserId(id).stream().filter(c -> !c.isInWishList()).collect(Collectors.toList());
        if (items.isEmpty())
            return new ArrayList<>();
        return items;
    }

    @Override
    public List<CartModel> getAllItemFromWishList(String token) {
        Long id = JwtGenerator.decodeJWT(token);
        List<CartModel> items = cartRepository.findAllByUserId(id).stream().filter(CartModel::isInWishList).collect(Collectors.toList());
        if (items.isEmpty())
            return new ArrayList<>();
        return items;
    }

    @Override
    public List<Long> getWishListStatus(String token) {
        List<CartModel> allItemFromCart = getAllItemFromCart(token);
        return allItemFromCart.stream().map(CartModel::getBookId).collect(Collectors.toList());
    }

    @Override
    public List<Long> getCartListStatus(String token) {
        List<CartModel> allItemFromCart = getAllItemFromWishList(token);
        return allItemFromCart.stream().map(CartModel::getBookId).collect(Collectors.toList());
    }

    @Override
    public String setProfilePic(String imageUrl, String token) {
        long id = JwtGenerator.decodeJWT(token);
        UserModel user = userRepository.findByUserId(id);
        user.setProfileUrl(imageUrl);
        userRepository.save(user);
        return imageUrl;
    }


    @Override
    public BookModel getBookDetails(Long bookId) {
        return bookRepository.getBookDetail(bookId);
    }

    /************************ user details ****************************/
    @Override
    public UserAddressDetailsResponse getUserDetails(long userId) {
        UserModel user = userRepository.findByUserId(userId);
        List<UserDetailsDTO> allDetailsByUser = user.getListOfUserDetails().stream().map(this::mapData).collect(toList());
        if (allDetailsByUser.isEmpty())
            return new UserAddressDetailsResponse(HttpStatus.OK.value(), environment.getProperty("user.details.nonAvailable"));
        return new UserAddressDetailsResponse(HttpStatus.OK.value(), environment.getProperty("user.details.available"), allDetailsByUser);
    }

    private UserDetailsDTO mapData(UserDetails details) {
        UserDetailsDTO userDto = new UserDetailsDTO();
        BeanUtils.copyProperties(details, userDto);
        return userDto;
    }

    @Override
    public Response addUserDetails(UserDetailsDTO userDetail, String token) {
        long userId = JwtGenerator.decodeJWT(token);
        UserDetails userDetailsDAO = new UserDetails();
        BeanUtils.copyProperties(userDetail, userDetailsDAO);
        UserModel user = userRepository.findByUserId(userId);
        userDetailsDAO.setUserId(userId);
        user.addUserDetails(userDetailsDAO);
        userRepository.save(user);
        userDetailsDAO.setUser(user);
        userDetailsRepository.save(userDetailsDAO);
        return new Response(HttpStatus.OK.value(), environment.getProperty("user.details.added"));
    }

    @Override
    public Response deleteUserDetails(UserDetailsDTO userDetail, long userId) {
        UserModel userModel = userRepository.findByUserId(userId);
        UserDetails userDetailsDAO = userDetailsRepository.findByAddressAndUserId(userDetail.getAddress(), userId);
        userModel.removeUserDetails(userDetailsDAO);
        userDetailsRepository.delete(userDetailsDAO);
        userRepository.save(userModel);
        return new Response(HttpStatus.OK.value(), environment.getProperty("user.details.deleted"));
    }

    @Override
    public Response removeAll(String token) {
        long userId = JwtGenerator.decodeJWT(token);
        orderIdGlobal = addToOrderPlaced(token);
        cartRepository.findAllByUserId(userId).stream().filter(c -> !c.isInWishList()).forEach(v -> cartRepository.delete(v));
        return new Response(HttpStatus.OK.value(), environment.getProperty("quantity.removed.success"));
    }


//    ==============  Order Placed ================== //

    private long addToOrderPlaced(String token) {
        long userId = JwtGenerator.decodeJWT(token);
        UserModel userModel = userRepository.findByUserId(userId);
        List<CartModel> allItemFromCart = getAllItemFromCart(token);
        long orderId = generateOrderId();
        StringBuilder message =
                new StringBuilder(
                        "Hi, " + userModel.getFullName() + "\n\n" +
                        "Your Order is Successfully Placed.\n " +
                        "<b style='color:blue;'>your order Id is :</b> " + orderIdGlobal + "\n\n\n" +
                        "Order Details");
        for (CartModel cartModel : allItemFromCart) {
            BookModel bookModel = bookRepository.findByBookId(cartModel.getBookId());
            bookModel.setQuantity((int) cartModel.getQuantity());
            bookRepository.save(bookModel);
            OrderPlaced order = new OrderPlaced();
            BeanUtils.copyProperties(cartModel, order);
            order.setOrderId(orderId);
            order.setQuantity((int) cartModel.getQuantity());
            orderRepository.save(order);
            String bookOrder =
            "\n-------------------------------------------------------------------\n" +
            "Book Name : " + bookModel.getBookName()+"\n" +
            "Book Price : " + bookModel.getPrice()+"\n" +
            "Quantity : " + cartModel.getQuantity()+"\n" +
            "Total Price : " + cartModel.getPrice()+"\n";
            message.append(bookOrder);
        }
        message.append("-------------------------------------------------------------------\n\n\n\n\n");
        message.append("Thank You for Shopping With Us !!\n\n\n");
        message.append(
                "regards\n"+
                "Online Book Store Team, Bangalore\n"+
                "Contact Us : +91-9771971429");

        redis.putMap(redisKey, userModel.getEmailId(), userModel.getFullName());
        rabbitMQSender.send(new EmailObject(userModel.getEmailId(), "Online Book Order Confirmation ", message.toString(), "Order Confirmation Mail"));
        return orderId;
    }

    @Override
    public long getOrderId() {
        return orderIdGlobal;
    }

    public long generateOrderId() {
        long orderId = (long) ((Math.random() * 11111L) + 999999L);
        String pattern = "yyyyMMddHHmmSSmmSSyyyy";

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
        Date time = new Date();
        String date = simpleDateFormat.format(time) + orderId;
        return Long.parseLong(date);
    }

    // ========================  without login add to cart and wishlist ========================== //
    @Override
    public Response addToCartWithoutLogin(Long bookId, String ipAddress) {
        CartModel cartData = cartRepository.findByIpAddressAndBookId(ipAddress, bookId);
        if (cartData != null && !cartData.isInWishList()) {
            return new Response(HttpStatus.OK.value(), "Book already added to cart (still not assigned)");
        } else if (cartData != null && cartData.isInWishList()) {
            cartData.setInWishList(false);
            cartRepository.save(cartData);
            return new Response(HttpStatus.OK.value(), "Book added to cart successfully,Removed From wishlist (without Login)");
        } else {
            CartModel cart = new CartModel();
            BookModel book = bookRepository.findByBookId(bookId);
            BeanUtils.copyProperties(book, cart);
            cart.setQuantity(1);
            cart.setIpAddress(ipAddress);
            cartRepository.save(cart);
            return new Response(HttpStatus.OK.value(), "Book Added to Cart Added On the reference of IpAddress");
        }
    }

    @Override
    public Response addFromWishlistToCartWithoutLogin(Long bookId, String ipAddress) {
        CartModel cart = cartRepository.findByIpAddressAndBookId(ipAddress, bookId);
        cart.setInWishList(false);
        return new Response(HttpStatus.OK.value(), "Book Added from wishlist to Cart Added On the reference of IpAddress");
    }

    @Override
    public Response deleteFromWishlistWithoutLogin(Long bookId, String ipAddress) {
        CartModel cart = cartRepository.findByIpAddressAndBookId(ipAddress, bookId);
        cartRepository.delete(cart);
        return new Response(HttpStatus.OK.value(), "Book Deleted from Cart On the reference of IpAddress");
    }

    @Override
    public Response addToWishListWithoutLogin(Long bookId, String ipAddress) {
        CartModel cartData = cartRepository.findByIpAddressAndBookId(ipAddress, bookId);
        if (cartData != null && cartData.isInWishList()) {
            return new Response(HttpStatus.OK.value(), "Book already added to WishList (still not assigned)");
        } else if (cartData != null && !cartData.isInWishList()) {
            cartData.setInWishList(true);
            cartRepository.save(cartData);
            return new Response(HttpStatus.OK.value(), "Book added to WishList successfully (without Login)");
        } else {
            CartModel cart = new CartModel();
            BookModel book = bookRepository.findByBookId(bookId);
            BeanUtils.copyProperties(book, cart);
            cart.setQuantity(1);
            cart.setInWishList(true);
            cart.setIpAddress(ipAddress);
            cartRepository.save(cart);
            return new Response(HttpStatus.OK.value(), "Book Added to Cart Added On the reference of IpAddress");
        }
    }

    @Override
    public Response addMoreItemsWithoutLogin(Long bookId, String ipAddress) {
        CartModel cart = cartRepository.findByIpAddressAndBookId(ipAddress, bookId);
        BookModel bookModel = bookRepository.findByBookId(bookId);
        if (cart.getQuantity() > 0) {
            cart.setQuantity(cart.getQuantity() + 1);
            cart.setPrice(bookModel.getPrice() * cart.getQuantity());
            cartRepository.save(cart);
        }
        return new Response(HttpStatus.OK.value(), "Quantity increased into Cart On the reference of IpAddress");
    }

    @Override
    public Response removeItemWithoutLogin(Long bookId, String ipAddress) {
        CartModel cart = cartRepository.findByIpAddressAndBookId(ipAddress, bookId);
        BookModel bookModel = bookRepository.findByBookId(bookId);
        if (cart.getQuantity() > 0) {
            cart.setQuantity(cart.getQuantity() - 1);
            cart.setPrice(bookModel.getPrice() * cart.getQuantity());
            cartRepository.save(cart);
        }
        return new Response(HttpStatus.OK.value(), "Quantity increased into Cart On the reference of IpAddress");
    }

    @Override
    public Response removeAllItemWithoutLogin(Long bookId, String ipAddress) {
        CartModel cart = cartRepository.findByIpAddressAndBookId(ipAddress, bookId);
        cartRepository.delete(cart);
        return new Response(HttpStatus.OK.value(), "Removed from wishlist On the reference of IpAddress");
    }

    @Override
    public List<BookModel> getAllItemFromWishListWithoutLogin(String ipAddress) {
        List<CartModel> allItemFromCartWithoutLogin = cartRepository.findByIpAddress(ipAddress);
        List<Long> collect = allItemFromCartWithoutLogin.stream().filter(CartModel::isInWishList).map(CartModel::getBookId).collect(toList());
        return collect.stream().map(v -> bookRepository.findByBookId(v)).collect(Collectors.toList());
    }

    @Override
    public List<BookModel> getAllItemFromCartListWithoutLogin(String ipAddress) {
        List<CartModel> allItemFromCartWithoutLogin = cartRepository.findByIpAddress(ipAddress);
        List<Long> collect = allItemFromCartWithoutLogin.stream().filter(v -> !v.isInWishList()).map(CartModel::getBookId).collect(toList());
        return collect.stream().map(v -> bookRepository.findByBookId(v)).collect(Collectors.toList());
    }
}
