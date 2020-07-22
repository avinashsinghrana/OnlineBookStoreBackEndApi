package com.bridgelabz.bookstore.controller;

import com.bridgelabz.bookstore.ElasticSearch.Service.SellerElasticService;
import com.bridgelabz.bookstore.model.BookModel;
import com.bridgelabz.bookstore.repository.BookRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.bridgelabz.bookstore.dto.BookDto;
import com.bridgelabz.bookstore.dto.UpdateBookDto;
import com.bridgelabz.bookstore.exception.UserException;
import com.bridgelabz.bookstore.response.Response;
import com.bridgelabz.bookstore.service.SellerService;
import com.bridgelabz.bookstore.serviceimplementation.AmazonS3ClientServiceImpl;

import io.swagger.annotations.Api;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/sellers")
@Api(value = "Seller Controller to perform CRUD operations on book")
public class SellerController {

    @Autowired
    BookRepository bookRepository;

    @Autowired
    private SellerService sellerService;

    @Autowired
    private Environment environment;

    @Autowired
    private SellerElasticService sellerElasticService;

    @Autowired
    private AmazonS3ClientServiceImpl amazonS3Client;

    @PostMapping(value = "/addBook")
    public ResponseEntity<Response> addBook(@RequestBody BookDto newBook,
                                            @RequestHeader("token") String token) throws UserException, IOException {
        Response addedBook = sellerService.addBook(newBook, token);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(new Response("book Added SuccessFully", HttpStatus.OK.value(), addedBook));
    }

    @GetMapping(value = "/getUnverifiedBooks")
    public ResponseEntity<Response> getAllBooks(@RequestHeader("token") String token) throws IOException {
        List<BookModel> book = sellerElasticService.getAllBooks(token);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(new Response("Getting all the books which are unverified", 200, book));
    }

    @PutMapping(value = "/applyForApproval")
    public Response applyForApproval(@RequestParam long bookId, @RequestParam("token") String token) throws IOException {
        return sellerService.applyForApproval(bookId, token);
    }

    @PostMapping(value = "/addImg", headers = "Accept=application/json")
    public ResponseEntity<Response> addImage(@RequestPart MultipartFile multipartFile) {
        String imgUrl = amazonS3Client.uploadFile(multipartFile);
        return ResponseEntity.status(HttpStatus.OK).body(new Response(HttpStatus.OK.value(), imgUrl));
    }

    @PutMapping(value = "/updateBook", headers = "Accept=application/json")
    public Response updateBook(@RequestBody UpdateBookDto newBook, @RequestHeader("token") String token,
                                               Long bookId) throws UserException, IOException {
        return sellerService.updateBook(newBook, token, bookId);
    }

    @DeleteMapping(value = "/DeleteBook", headers = "Accept=application/json")
    public Response deleteBook(@RequestHeader("token") String token, @RequestParam Long bookId) throws IOException {
        return sellerService.deleteBook(token, bookId);

    }

    @GetMapping(value = "/searchbyBookName")
    public ResponseEntity<Response> searchByBookName(@RequestParam String bookName) throws IOException {
        List<BookModel> bookModels = sellerElasticService.searchByBookName(bookName);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(new Response("all books similar with name", 200, bookModels));
    }

    @GetMapping(value = "/searchbyAuthorName")
    public ResponseEntity<Response> searchByAuthorName(@RequestParam String authorName) throws IOException {
        List<BookModel> bookModels = sellerElasticService.searchByAuthor(authorName);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(new Response("all books similar to Author", 200, bookModels));
    }

}

