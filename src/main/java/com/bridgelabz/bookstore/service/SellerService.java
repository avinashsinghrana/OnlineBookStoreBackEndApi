package com.bridgelabz.bookstore.service;

import org.springframework.web.multipart.MultipartFile;

import com.bridgelabz.bookstore.dto.BookDto;
import com.bridgelabz.bookstore.dto.UpdateBookDto;
import com.bridgelabz.bookstore.exception.UserException;
import com.bridgelabz.bookstore.response.Response;

import java.io.IOException;

public interface SellerService {

	Response addBook(BookDto newBook,String token) throws UserException, IOException;

	Response updateBook(UpdateBookDto newBook, String token,Long BookId) throws UserException, IOException;

	Response deleteBook(String token, Long bookId) throws IOException;

	Response applyForApproval(long bookId, String token) throws IOException;
}
