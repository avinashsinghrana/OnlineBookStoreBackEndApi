package com.bridgelabz.bookstore.service;

import com.bridgelabz.bookstore.dto.*;
import com.bridgelabz.bookstore.exception.BookException;
import com.bridgelabz.bookstore.model.BookModel;
import com.bridgelabz.bookstore.model.CartModel;
import com.bridgelabz.bookstore.response.UserAddressDetailsResponse;
import org.springframework.stereotype.Component;

import com.bridgelabz.bookstore.exception.UserException;
import com.bridgelabz.bookstore.exception.UserNotFoundException;
import com.bridgelabz.bookstore.response.Response;
import com.bridgelabz.bookstore.response.UserDetailsResponse;

import java.util.List;

@Component
public interface UserService {

    boolean register(RegistrationDto registrationDto) throws UserException;

    boolean verify(String token);

    UserDetailsResponse forgetPassword(ForgotPasswordDto emailId);

    boolean resetPassword(ResetPasswordDto resetPassword, String token) throws UserNotFoundException;

    Response login(LoginDto logindto) throws UserNotFoundException, UserException;


    Response addToCart(Long bookId, String token) throws BookException;

    Response addMoreItems(Long bookId, String token) throws BookException;

    Response removeItem(Long bookId, String token) throws BookException;

    Response removeAllItem(Long bookId, String token);

    List<CartModel> getAllItemFromCart(String token) throws BookException;

    BookModel getBookDetails(Long bookId) throws UserException;

    /// to get user details to place order
    UserAddressDetailsResponse getUserDetails(long userId);

    // add new user details
    Response addUserDetails(UserDetailsDTO userDetail, String token);

    // update existing user details
    Response deleteUserDetails(UserDetailsDTO userDetail, long userId);


    Response removeAll(String token);

    long getOrderId();

    Response addToWishList(Long bookId, String token);

    Response deleteFromWishlist(Long bookId, String token);

    Response addFromWishlistToCart(Long bookId, String token);

    List<CartModel> getAllItemFromWishList(String token) throws BookException;

    List<Long> getWishListStatus(String token);

    List<Long> getCartListStatus(String token);

    String setProfilePic(String imageUrl, String token);

    Response addToCartWithoutLogin(Long bookId, String ipAddress);

    Response addFromWishlistToCartWithoutLogin(Long bookId, String ipAddress);

    Response deleteFromWishlistWithoutLogin(Long bookId, String ipAddress);

    Response addToWishListWithoutLogin(Long bookId, String ipAddress);

    Response addMoreItemsWithoutLogin(Long bookId, String ipAddress);

    Response removeItemWithoutLogin(Long bookId, String ipAddress);

    Response removeAllItemWithoutLogin(Long bookId, String ipAddress);

    List<BookModel> getAllItemFromWishListWithoutLogin(String ipAddress);

    List<BookModel> getAllItemFromCartListWithoutLogin(String ipAddress);

}
