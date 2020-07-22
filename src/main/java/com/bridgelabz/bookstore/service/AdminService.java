package com.bridgelabz.bookstore.service;

import java.io.IOException;
import java.util.List;

import com.bridgelabz.bookstore.dto.SellerDTO;
import com.bridgelabz.bookstore.exception.UserNotFoundException;
import com.bridgelabz.bookstore.model.BookModel;
import com.bridgelabz.bookstore.model.UserModel;
import com.bridgelabz.bookstore.response.Response;

public interface AdminService {

	List<BookModel> getAllUnVerifiedBooks(String token) throws UserNotFoundException, IOException;

    Response bookVerification(Long bookId, Long sellerId, String token) throws UserNotFoundException, IOException;

    Response bookDisApprove(Long bookId, Long sellerId, String token) throws IOException, UserNotFoundException;

    List<SellerDTO> getSellerListRequestedForApproval() throws IOException;
}