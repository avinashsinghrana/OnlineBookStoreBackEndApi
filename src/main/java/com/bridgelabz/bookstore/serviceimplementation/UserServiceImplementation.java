package com.bridgelabz.bookstore.serviceimplementation;

import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
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
    public static String orderIdGlobal;

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
        if (JwtGenerator.decodeJWT(token) != -1L) {
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
                cartModel.setIpAddress("NotLocalUser");
                cartModel.setUserId(id);
                cartModel.setActualQuantity(bookModel.getQuantity());
                cartModel.setInWishList(false);
                cartRepository.save(cartModel);
                return new Response(HttpStatus.OK.value(), environment.getProperty("book.added.to.cart.successfully"), getCartListStatus(token).size());
            }
        } else {
            CartModel cartData = cartRepository.findByIpAddressAndBookId(token, bookId);
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
                cartModel.setIpAddress(token);
                cartModel.setUserId(-1);
                cartModel.setActualQuantity(bookModel.getQuantity());
                cartModel.setInWishList(false);
                cartRepository.save(cartModel);
                return new Response(HttpStatus.OK.value(), environment.getProperty("book.added.to.cart.successfully"), getCartListStatus(token).size());
            }
        }
    }

    @Override
    public Response addToWishList(Long bookId, String token) {
        if (JwtGenerator.decodeJWT(token) != -1L) {
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
                cartModel.setIpAddress("NotLocalUser");
                cartModel.setQuantity(1);
                cartModel.setUserId(JwtGenerator.decodeJWT(token));
                cartModel.setInWishList(true);
                cartRepository.save(cartModel);
                return new Response(HttpStatus.OK.value(), "Book added to WishList", getAllItemFromWishList(token).size());
            }
        } else {
            CartModel cartData = cartRepository.findByIpAddressAndBookId(token, bookId);
            if (cartData != null && cartData.isInWishList()) {
                return new Response(HttpStatus.OK.value(), "Book already present in wishlist");
            } else if (cartData != null && !cartData.isInWishList()) {
                return new Response(HttpStatus.OK.value(), "Book already added to Cart");
            } else {
                BookModel bookModel = bookRepository.findByBookId(bookId);
                CartModel cartModel = new CartModel();
                BeanUtils.copyProperties(bookModel, cartModel);
                cartModel.setIpAddress(token);
                cartModel.setQuantity(1);
                cartModel.setUserId(-1);
                cartModel.setInWishList(true);
                cartRepository.save(cartModel);
                return new Response(HttpStatus.OK.value(), "Book added to WishList", getAllItemFromWishList(token).size());
            }
        }
    }

    @Override
    public Response deleteFromWishlist(Long bookId, String token) {
        if (JwtGenerator.decodeJWT(token) != -1L) {
            long id = JwtGenerator.decodeJWT(token);
            cartRepository.deleteByUserIdAndBookId(id, bookId);
        } else {
            cartRepository.deleteByIpAddressAndBookId(token, bookId);
        }
        return new Response(HttpStatus.OK.value(), "Removed SuccessFully from WishKart");
    }

    @Override
    public Response addFromWishlistToCart(Long bookId, String token) {
        BookModel bookModel = bookRepository.findByBookId(bookId);
        if (JwtGenerator.decodeJWT(token) != -1) {
            long id = JwtGenerator.decodeJWT(token);
            CartModel cartModel = cartRepository.findByUserIdAndBookId(id, bookId);
            String message = wishToCart(cartModel, bookModel);
            return new Response(HttpStatus.OK.value(), message, getCartListStatus(token).size());
        } else {
            CartModel cartModel = cartRepository.findByIpAddressAndBookId(token, bookId);
            String message = wishToCart(cartModel, bookModel);
            return new Response(HttpStatus.OK.value(), message, getCartListStatus(token).size());
        }
    }

    private String wishToCart(CartModel cartModel, BookModel bookModel) {
        if (cartModel.isInWishList()) {
            cartModel.setInWishList(false);
            cartModel.setActualQuantity(bookModel.getQuantity());
            cartRepository.save(cartModel);
            return "Added SuccessFully To addToKart from wishlist";
        }
        return "Already present in cart, ready to checkout";
    }

    @Override
    public Response addMoreItems(Long bookId, String token) {
        if (JwtGenerator.decodeJWT(token) != -1L) {
            long id = JwtGenerator.decodeJWT(token);
            CartModel cartModel = cartRepository.findByUserIdAndBookId(id, bookId);
            increaseQuantity(cartModel, bookId);
            return new Response(environment.getProperty("book.added.to.cart.successfully"), HttpStatus.OK.value(), cartModel);
        } else {
            CartModel cartModel = cartRepository.findByIpAddressAndBookId(token, bookId);
            increaseQuantity(cartModel, bookId);
            return new Response(environment.getProperty("book.added.to.cart.successfully"), HttpStatus.OK.value(), cartModel);
        }
    }

    private void increaseQuantity(CartModel cartModel, Long bookId) {
        BookModel bookModel = bookRepository.findByBookId(bookId);
        if (cartModel.getQuantity() > 0) {
            cartModel.setQuantity(cartModel.getQuantity() + 1);
            cartModel.setPrice(bookModel.getPrice() * cartModel.getQuantity());
            cartRepository.save(cartModel);
        }
    }

    @Override
    public Response removeItem(Long bookId, String token) {
        if (JwtGenerator.decodeJWT(token) == -1) {
            long id = JwtGenerator.decodeJWT(token);
            CartModel cartModel = cartRepository.findByUserIdAndBookId(id, bookId);
            String message = removeQuantity(cartModel, bookId);
            return new Response(HttpStatus.OK.value(), message, getCartListStatus(token).size());
        } else {
            CartModel cartModel = cartRepository.findByIpAddressAndBookId(token, bookId);
            String message = removeQuantity(cartModel, bookId);
            return new Response(HttpStatus.OK.value(), message, getCartListStatus(token).size());
        }
    }

    private String removeQuantity(CartModel cartModel, Long bookId) {
        BookModel bookModel = bookRepository.findByBookId(bookId);
        if (cartModel.getQuantity() > 0) {
            cartModel.setQuantity(cartModel.getQuantity() - 1);
            cartModel.setPrice(bookModel.getPrice() * cartModel.getQuantity());
            cartRepository.save(cartModel);
        }
        return environment.getProperty("one.quantity.removed.success");
    }

    @Override
    public Response removeAllItem(Long bookId, String token) {
        if (JwtGenerator.decodeJWT(token) != 1L) {
            long id = JwtGenerator.decodeJWT(token);
            cartRepository.deleteByUserIdAndBookId(id, bookId);
        } else {
            cartRepository.deleteByIpAddressAndBookId(token, bookId);
        }
        return new Response(HttpStatus.OK.value(), environment.getProperty("quantity.removed.success"));
    }

    @Override
    public List<CartModel> getAllItemFromCart(String token) {
        if (JwtGenerator.decodeJWT(token) != -1L) {
            Long id = JwtGenerator.decodeJWT(token);
            List<CartModel> items = cartRepository.findAllByUserId(id).stream().filter(c -> !c.isInWishList()).collect(Collectors.toList());
            if (items.isEmpty())
                return new ArrayList<>();
            return items;
        } else {
            List<CartModel> items = cartRepository.findAllByIpAddress(token).stream().filter(c -> !c.isInWishList()).collect(Collectors.toList());
            if (items.isEmpty())
                return new ArrayList<>();
            return items;
        }
    }

    @Override
    public List<CartModel> getAllItemFromWishList(String token) {
        if (JwtGenerator.decodeJWT(token) != -1L) {
            Long id = JwtGenerator.decodeJWT(token);
            List<CartModel> items = cartRepository.findAllByUserId(id).stream().filter(CartModel::isInWishList).collect(Collectors.toList());
            if (items.isEmpty())
                return new ArrayList<>();
            return items;
        } else {
            System.out.println("Inside IpAddress Part");
            List<CartModel> items = cartRepository.findAllByIpAddress(token).stream().filter(CartModel::isInWishList).collect(Collectors.toList());
            if (items.isEmpty())
                return new ArrayList<>();
            return items;
        }
    }

    @Override
    public List<Long> getWishListStatus(String token) {
        if (JwtGenerator.decodeJWT(token) != -1L) {
            List<CartModel> allItemFromCart = getAllItemFromCart(token);
            return allItemFromCart.stream().map(CartModel::getBookId).collect(Collectors.toList());
        } else {
            List<CartModel> allItemFromCart = getAllItemFromCart(token);
            return allItemFromCart.stream().map(CartModel::getBookId).collect(Collectors.toList());
        }
    }

    @Override
    public List<Long> getCartListStatus(String token) {
        if (JwtGenerator.decodeJWT(token) != -1L) {
            List<CartModel> allItemFromCart = getAllItemFromWishList(token);
            return allItemFromCart.stream().map(CartModel::getBookId).collect(Collectors.toList());
        } else {
            List<CartModel> allItemFromCart = getAllItemFromWishList(token);
            return allItemFromCart.stream().map(CartModel::getBookId).collect(Collectors.toList());
        }
    }

    @Override
    public void setProfilePic(String imageUrl, String token) {
        long id = JwtGenerator.decodeJWT(token);
        UserModel user = userRepository.findByUserId(id);
        user.setProfileUrl(imageUrl);
        userRepository.save(user);
    }

    @Override
    public Response assignToCartAndWishList(String ipAddress, String token) {
        long id = JwtGenerator.decodeJWT(token);
        List<CartModel> cartModelsByIpAddress = cartRepository.findAllByIpAddress(ipAddress);
        List<CartModel> cartModelsByUser = cartRepository.findAllByUserId(id);
        if (!cartModelsByIpAddress.isEmpty()) {
            for (CartModel cartModelByIP : cartModelsByIpAddress) {
                cartModelByIP.setUserId(id);
                boolean status = cartModelsByUser.stream().noneMatch(v -> v.equals(cartModelByIP));
                if (status){
                    cartModelByIP.setIpAddress("NotLocalUser");
                    cartRepository.save(cartModelByIP);
                } else {
                    List<CartModel> cartModelByUSER = cartModelsByUser.stream().filter(p -> p.equals(cartModelByIP)).collect(toList());
                    BeanUtils.copyProperties(cartModelByIP, cartModelByUSER.get(0));
                    cartModelByUSER.get(0).setIpAddress("NotLocalUser");
                    cartRepository.save(cartModelByUSER.get(0));
                }
            }
            return new Response(HttpStatus.OK.value(), "Book Assigned to Cart And Wishlist");
        }
        return new Response(HttpStatus.OK.value(), "Nothing found with ipAddress");
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
        long id = userDetailsRepository.findByAddressAndUserId(userDetail.getAddress(), userId).getSequenceNo();
        return new Response(environment.getProperty("user.details.added"), HttpStatus.OK.value(), id);
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
    public Response removeAll(String token, Long id) {
        long userId = JwtGenerator.decodeJWT(token);
        orderIdGlobal = addToOrderPlaced(token, id);
        cartRepository.findAllByUserId(userId).stream().filter(c -> !c.isInWishList()).forEach(v -> cartRepository.delete(v));
        return new Response(HttpStatus.OK.value(), environment.getProperty("quantity.removed.success"), 0);
    }


//    ==============  Order Placed ================== //

    private String addToOrderPlaced(String token, Long id) {
        long userId = JwtGenerator.decodeJWT(token);
        UserModel userModel = userRepository.findByUserId(userId);
        UserDetails userDetails = userDetailsRepository.findById(id).get();
        List<CartModel> allItemFromCart = getAllItemFromCart(token);
        String orderId = generateOrderId();
        StringBuilder message =
                new StringBuilder(
                        "Hi, " + userModel.getFullName() + "\n\n" +
                                "Your Order is Successfully Placed.\n " +
                                "your order Id is :" + orderId + "\n\n" +
                                "Shipping Address : " + userDetails.getFullName() + "\n" +
                                "" + userDetails.getAddress().substring(0, 30) + "-\n" +
                                userDetails.getAddress().substring(30) + "\n" +
                                userDetails.getCity() + " -" + userDetails.getPinCode() + "\n" + "\n" +
                                "Order Summary");
        long totalPrice = 0;
        for (CartModel cartModel : allItemFromCart) {
            BookModel bookModel = bookRepository.findByBookId(cartModel.getBookId());
            bookModel.setQuantity((int) (bookModel.getQuantity() - cartModel.getQuantity()));
            bookRepository.save(bookModel);
            OrderPlaced order = new OrderPlaced();
            BeanUtils.copyProperties(cartModel, order);
            order.setOrderId(orderId);
            order.setQuantity((int) cartModel.getQuantity());
            orderRepository.save(order);
            totalPrice += cartModel.getPrice();
            String bookOrder =
                    "\n-------------------------------------------------------------------\n" +
                            "Book Name : " + bookModel.getBookName() + "\n" +
                            "Book Price : Rs." + bookModel.getPrice() + "\n" +
                            "Quantity : " + cartModel.getQuantity() + "\n" +
                            "Total Price : Rs." + cartModel.getPrice();
            message.append(bookOrder);
        }
        message.append("\n-------------------------------------------------------------------\n" + "Overall Amount Received : Rs.")
                .append(totalPrice).append("\n\n")
                .append("Thank You for Shopping With Us !!\n\n\n")
                .append(
                        "regards\n" +
                                "Online Book Store Team, Bangalore\n" +
                                "Contact Us : +91-9771971429");
        redis.putMap(redisKey, userModel.getEmailId(), userModel.getFullName());
        rabbitMQSender.send(new EmailObject(userModel.getEmailId(), "Online Book Order Confirmation ", message.toString(), "Order Confirmation Mail"));
        return orderId;
    }

    @Override
    public String getOrderId() {
        return orderIdGlobal;
    }

    public String generateOrderId() {
        long random = (long) ((Math.random() * 111L) + 99999999L);
        String pattern = "yyyyMMddHHmmSSmmYYYY";

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
        Date time = new Date();
        return "OBS" + simpleDateFormat.format(time) + random;
    }
}
