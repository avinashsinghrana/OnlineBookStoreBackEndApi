package com.bridgelabz.bookstore.controller;

import java.io.IOException;
import java.util.List;

import javax.swing.text.html.HTML;
import javax.validation.Valid;

import com.bridgelabz.bookstore.ElasticSearch.Service.UserElasticService;
import com.bridgelabz.bookstore.dto.*;
import com.bridgelabz.bookstore.exception.BookException;
import com.bridgelabz.bookstore.response.UserAddressDetailsResponse;
import com.bridgelabz.bookstore.serviceimplementation.AmazonS3ClientServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;

import com.bridgelabz.bookstore.exception.UserException;
import com.bridgelabz.bookstore.exception.UserNotFoundException;
import com.bridgelabz.bookstore.model.BookModel;
import com.bridgelabz.bookstore.response.Response;
import com.bridgelabz.bookstore.response.UserDetailsResponse;
import com.bridgelabz.bookstore.service.UserService;

import io.swagger.annotations.ApiOperation;
import org.springframework.web.servlet.ModelAndView;

@RestController
@RequestMapping("/user")
@CrossOrigin(allowedHeaders = "*", origins = "*")
@PropertySource(name = "user", value = {"classpath:response.properties"})
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private Environment environment;

    @Autowired
    private UserElasticService userElasticService;

    @Autowired
    private AmazonS3ClientServiceImpl amazonS3ClientService;


    @PostMapping("/register")
    public ResponseEntity<Response> register(@RequestBody @Valid RegistrationDto registrationDto, BindingResult result) throws UserException {

        if (result.hasErrors())
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new Response(result.getAllErrors().get(0).getDefaultMessage(), HttpStatus.BAD_REQUEST.value(), "Invalid Credentials"));

        if (userService.register(registrationDto))
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new Response(HttpStatus.OK.value(), environment.getProperty("user.register.successful")));
        return ResponseEntity.status(HttpStatus.OK)
                .body(new Response(HttpStatus.BAD_REQUEST.value(), environment.getProperty("user.register.unsuccessful")));
    }

    @GetMapping("/verify/{token}")
    public ModelAndView userVerification(@PathVariable("token") String token) {
        ModelAndView modelAndView = new ModelAndView();
        if (userService.verify(token)) {
//            return ResponseEntity.status(HttpStatus.OK)
//                    .body(new Response(HttpStatus.OK.value(), environment.getProperty("user.verified.successful")));

            modelAndView.setViewName("verifyResponse");
            return modelAndView;
        }
//        return ResponseEntity.status(HttpStatus.OK).body(new Response(HttpStatus.BAD_REQUEST.value(), environment.getProperty("user.verified.unsuccessfull")));.
        else{
            modelAndView.setViewName("Unverified");
            return modelAndView;
        }
    }

    @PostMapping("/forgotpassword")
    public ResponseEntity<UserDetailsResponse> forgotPassword(@RequestBody @Valid ForgotPasswordDto emailId) {

        UserDetailsResponse response = userService.forgetPassword(emailId);
        return new ResponseEntity<UserDetailsResponse>(response, HttpStatus.OK);
    }

    @PutMapping("/resetPassword/{token}")
    public ResponseEntity<Response> resetPassword(@RequestBody @Valid ResetPasswordDto resetPassword,
                                                  @RequestParam("token") String token) throws UserNotFoundException {

        if (userService.resetPassword(resetPassword, token))
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new Response(HttpStatus.OK.value(), environment.getProperty("user.resetPassword.successful")));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new Response(HttpStatus.BAD_REQUEST.value(), environment.getProperty("user.resetPassword.failed")));
    }

    @ApiOperation(value = "To login")
    @PostMapping("/login")
    @CrossOrigin(origins = "http://localhost:4200")
    public ResponseEntity<Response> login(@RequestBody LoginDto loginDTO) throws UserNotFoundException, UserException {
        Response response = userService.login(loginDTO);
        return new ResponseEntity<Response>(response, HttpStatus.OK);

    }

    @ApiOperation(value = "Add Books to Cart")
    @PostMapping("/AddToCart")
    public ResponseEntity<Response> AddToCart(@RequestParam Long bookId, @RequestParam("token") String token) throws BookException {
        Response response = userService.addToCart(bookId, token);
        return new ResponseEntity<Response>(response, HttpStatus.OK);

    }

    @ApiOperation(value = "Adding More Items To Cart")
    @PostMapping("/addMoreItems")
    @CrossOrigin(origins = "http://localhost:4200")
    public ResponseEntity<Response> addMoreItems(@RequestParam Long bookId, @RequestParam String token) throws BookException {
        Response response = userService.addMoreItems(bookId, token);
        return new ResponseEntity<Response>(response, HttpStatus.OK);
    }

    @ApiOperation(value = "Remove Items from Cart")
    @DeleteMapping("/removeFromCart")
    @CrossOrigin(origins = "http://localhost:4200")
    public ResponseEntity<Response> removeFromCart(@RequestParam Long bookId, @RequestParam String token) throws BookException {
        Response response = userService.removeItem(bookId, token);
        return new ResponseEntity<Response>(response, HttpStatus.OK);
    }

    @ApiOperation(value = "Remove All Individual Items from Cart")
    @DeleteMapping("/removeBookFromCart")
    @CrossOrigin(origins = "http://localhost:4200")
    public ResponseEntity<Response> removeAllFromCart(@RequestParam Long bookId, @RequestParam String token) {
        Response response = userService.removeAllItem(bookId, token);
        return new ResponseEntity<Response>(response, HttpStatus.OK);
    }

    @ApiOperation(value = "Get All Items from Cart")
    @GetMapping("/getAllFromCart")
    @CrossOrigin(origins = "http://localhost:4200")
    public Response getAllItemsFromCart(@RequestParam String token) throws BookException {
        return new Response("all list of cart", HttpStatus.OK.value(), userService.getAllItemFromCart(token));
    }

    @ApiOperation(value = "Search By Book Name")
    @PostMapping("/search")
    @CrossOrigin(origins = "http://localhost:3000")
    public Response searchByBookName(@RequestParam String bookName) throws IOException {
        return new Response("search by name data", HttpStatus.OK.value(), userElasticService.searchByBookName(bookName));
    }

    @ApiOperation(value = "Request Book in Ascending order")
    @GetMapping("/getBooksByPriceAsc")
    public ResponseEntity<Response> sortBookByPriceAsc() throws IOException {
        List<BookModel> sortBookByPriceAsc = userElasticService.sortAscendingByPrice();
        if (!sortBookByPriceAsc.isEmpty())
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new Response(environment.getProperty("user.bookDisplayed.lowToHigh"), HttpStatus.OK.value(), sortBookByPriceAsc));
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new Response(HttpStatus.NOT_FOUND.value(), environment.getProperty("user.bookDisplayed.failed")));
    }

    @ApiOperation(value = "Request Book in Descending order")
    @GetMapping("/getBooksByPriceDesc")
    public ResponseEntity<Response> sortBookByPriceDesc() throws IOException {
        List<BookModel> sortBookByPriceDesc = userElasticService.sortDescendingByPrice();
        if (!sortBookByPriceDesc.isEmpty())
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new Response(environment.getProperty("user.bookDisplayed.highToLow"), HttpStatus.OK.value(), sortBookByPriceDesc));
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new Response(HttpStatus.NOT_FOUND.value(), environment.getProperty("user.bookDisplayed.failed")));
    }

    @ApiOperation(value = "Obtain User Address Details")
    @GetMapping("/getUserDetails")
    public ResponseEntity<UserAddressDetailsResponse> getUserDetails(@RequestParam long id) {
        return ResponseEntity.status(HttpStatus.OK).body(userService.getUserDetails(id));
    }

    @PostMapping("/addUserDetails")
    public ResponseEntity<Response> addUserDetails(@RequestBody UserDetailsDTO userDetailsDTO, @RequestHeader("token") String token) {
        return ResponseEntity.status(HttpStatus.OK).body(userService.addUserDetails(userDetailsDTO, token));
    }

    @DeleteMapping("/deleteUserDetails/")
    public ResponseEntity<Response> deleteUserDetails(@RequestBody UserDetailsDTO userDetailsDTO, @RequestParam long userId) {
        return ResponseEntity.status(HttpStatus.OK).body(userService.deleteUserDetails(userDetailsDTO, userId));
    }

    @GetMapping("/getallBooks")
    public ResponseEntity<Response> getAllBooks() throws IOException, UserException {
        List<BookModel> book = userElasticService.getAllBook();
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(new Response("Getting all the books which are verified", 200, book));
    }

    @GetMapping("/getbookdetails/{bookId}")
    public ResponseEntity<Response> getBookDetails(@PathVariable Long bookId) throws UserException {
        BookModel book = userService.getBookDetails(bookId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(new Response("Getting book details", 200, book));
    }

    @PostMapping("/uploadFile")
    public ResponseEntity<Response> uploadFile(@RequestParam("file") MultipartFile file) {
        String url = amazonS3ClientService.uploadFile(file);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(new Response("Uploaded successfully", 200, url));
    }

    @DeleteMapping("/deleteFile")
    public String deleteFile(@RequestPart(value = "url") String fileUrl) {
        return amazonS3ClientService.deleteFileFromS3Bucket(fileUrl);
    }

    @GetMapping("/searchByAuthorName")
    public ResponseEntity<Response> searchByAuthorName(@RequestParam("authorName") String authorName) throws IOException {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(new Response("Result is based on search by name", 200, userElasticService.searchByAuthor(authorName)));
    }

    @ApiOperation(value = "Clear Cart while Checkout")
    @DeleteMapping("/removeAll")
    public Response removeAll(@RequestParam String token, @RequestParam Long id) {
        return userService.removeAll(token, id);
    }

    /* OrderIDGeneratorMethod */
    @GetMapping("/orderId")
    public Response getOrderId() {
        return new Response(HttpStatus.OK.value(),"Order ID",userService.getOrderId());
    }

    @PostMapping("/addToWishlist")
    public Response addToWishList(@RequestParam Long bookId, @RequestParam String token) {
        return userService.addToWishList(bookId, token);
    }

    @DeleteMapping("/deleteFromWishlist")
    public Response deleteFromWishlist(@RequestParam Long bookId, @RequestParam String token) {
        return userService.deleteFromWishlist(bookId, token);
    }

    @PutMapping("/addFromWishlistToCart")
    public Response addFromWishlistToCart(@RequestParam Long bookId, @RequestParam String token) {
        return userService.addFromWishlistToCart(bookId, token);
    }

    @ApiOperation(value = "Get all WishList Book")
    @GetMapping("/getWishListBooks")
    public ResponseEntity<Response> getWishListBooks(@RequestParam String token) throws BookException {
        return ResponseEntity.status(HttpStatus.OK).body(new Response("Wish List Status Update", HttpStatus.OK.value(), userService.getAllItemFromWishList(token)));
    }

    @GetMapping("/wishListStatus")
    public ResponseEntity<Response> getWishListStatus(@RequestParam String token) {
        return ResponseEntity.status(HttpStatus.OK).body(new Response("Wish List Status Update", HttpStatus.OK.value(), userService.getWishListStatus(token)));
    }

    @GetMapping("/cartListStatus")
    public ResponseEntity<Response> getCartListStatus(@RequestParam String token) {
        return ResponseEntity.status(HttpStatus.OK).body(new Response("Cart List Status Update", HttpStatus.OK.value(), userService.getCartListStatus(token)));
    }

    @PutMapping(value = "/addImg")
    public ResponseEntity<Response> addImageToProfile(@RequestParam("file") MultipartFile file, @RequestHeader String token) {
        String url = amazonS3ClientService.uploadFile(file);
        userService.setProfilePic(url,token);
        return ResponseEntity.status(HttpStatus.OK).body(new Response(HttpStatus.OK.value(), "Image Uploaded Successfully"));
    }

    @PutMapping(value = "/assignCart")
    public ResponseEntity<Response> assignCart(@RequestParam String ipAddress, @RequestParam String token) {
        Response response = userService.assignToCartAndWishList(ipAddress, token);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}