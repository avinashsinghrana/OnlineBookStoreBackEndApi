package com.bridgelabz.bookstore.serviceimplementation;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import com.bridgelabz.bookstore.ElasticSearch.Service.AdminElasticService;
import com.bridgelabz.bookstore.ElasticSearch.Service.SellerElasticService;
import com.bridgelabz.bookstore.ElasticSearch.Service.UserElasticService;
import org.elasticsearch.action.update.UpdateRequest;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.bridgelabz.bookstore.dto.BookDto;
import com.bridgelabz.bookstore.dto.UpdateBookDto;
import com.bridgelabz.bookstore.exception.UserException;
import com.bridgelabz.bookstore.model.BookModel;
import com.bridgelabz.bookstore.repository.BookRepository;
import com.bridgelabz.bookstore.repository.UserRepository;
import com.bridgelabz.bookstore.response.Response;
import com.bridgelabz.bookstore.service.SellerService;
import com.bridgelabz.bookstore.utility.JwtGenerator;

@Service
public class SellerServiceImplementation implements SellerService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private SellerElasticService sellerElasticService;

    @Autowired
    private AdminElasticService adminElasticService;

    @Autowired
    private UserElasticService userElasticService;

    @Autowired
    private AmazonS3ClientServiceImpl amazonS3Client;

    @Autowired
    private Environment environment;

    @Override
    public Response addBook(BookDto newBook, String token) throws UserException, IOException {
        Long id = JwtGenerator.decodeJWT(token);
        String role = String.valueOf(userRepository.findByUserId(id).getRoleType());
        if (role.equals("SELLER")) {
            BookModel book = new BookModel();
            BeanUtils.copyProperties(newBook, book);
            book.setSellerId(id);
            ZoneId zid = ZoneId.systemDefault();
            book.setUpdatedDateAndTime(ZonedDateTime.now(zid));
            bookRepository.save(book);
            sellerElasticService.addBookForElasticSearch(book);
            adminElasticService.addBookDetails(book);
            return new Response(environment.getProperty("book.verification.status"), HttpStatus.OK.value(), book);

        } else {
            throw new UserException(environment.getProperty("book.unauthorised.status"));
        }
    }

    @Override
    public Response updateBook(UpdateBookDto updateBookDto, String token, Long bookId) throws IOException {
        UpdateBookDto newBook = new UpdateBookDto();
        BeanUtils.copyProperties(updateBookDto, newBook);
        long id = JwtGenerator.decodeJWT(token);
        String role = String.valueOf(userRepository.findByUserId(id).getRoleType());
        if (role.equals("SELLER")) {
            BookModel book = bookRepository.findByBookId(bookId);
            if (newBook.getBookName() == null && newBook.getAuthorName() == null && newBook.getBookDetails() == null
                    && newBook.getPrice() == 0 && newBook.getQuantity() != 0) {
                if (newBook.getBookName() == null) newBook.setBookName(book.getBookName());
                if (newBook.getAuthorName() == null) newBook.setAuthorName(book.getAuthorName());
                if (newBook.getBookDetails() == null) newBook.setBookDetails(book.getBookDetails());
                if (newBook.getPrice() == 0) newBook.setPrice(book.getPrice());
                BeanUtils.copyProperties(newBook, book);
                ZoneId zid = ZoneId.systemDefault();
                book.setUpdatedDateAndTime(ZonedDateTime.now(zid));
                book.setVerfied(true);
                book.setDisapproved(false);
                bookRepository.save(book);
                System.out.println("inside quantity change only");
                sellerElasticService.updateBookForElasticSearch(book);
                adminElasticService.updateBookForElasticSearch(book);
                userElasticService.updateBookForElasticSearch(book);
                return new Response(HttpStatus.OK.value(), "Book updated Successfully");
            } else {
                if (newBook.getBookName() == null) newBook.setBookName(book.getBookName());
                if (newBook.getAuthorName() == null) newBook.setAuthorName(book.getAuthorName());
                if (newBook.getBookDetails() == null) newBook.setBookDetails(book.getBookDetails());
                if (newBook.getPrice() == 0) newBook.setPrice(book.getPrice());
                if (newBook.getQuantity() == 0) newBook.setQuantity(book.getQuantity());
                BeanUtils.copyProperties(newBook, book);
                ZoneId zid = ZoneId.systemDefault();
                book.setUpdatedDateAndTime(ZonedDateTime.now(zid));
                book.setVerfied(false);
                book.setDisapproved(false);
                book.setForApproval(false);
                bookRepository.save(book);
                sellerElasticService.updateBookForElasticSearch(book);
                adminElasticService.updateBookForElasticSearch(book);
                userElasticService.deleteBookForElasticSearch(bookId);
                System.out.println("inside whole change into book object");
                return new Response(HttpStatus.OK.value(), "Book updated Successfully, Need to Verify");
            }
        }
        return new Response(HttpStatus.OK.value(), "Unauthorized Access !!");
    }

    @Override
    public Response deleteBook(String token, Long bookId) throws IOException {
        long id = JwtGenerator.decodeJWT(token);
        String role = String.valueOf(userRepository.findByUserId(id).getRoleType());
        if (role.equals("SELLER")) {
            sellerElasticService.deleteBookForElasticSearch(bookId);
            userElasticService.deleteBookForElasticSearch(bookId);
            adminElasticService.deleteBookForElasticSearch(bookId);
            bookRepository.deleteByBookId(bookId);
            return new Response(HttpStatus.OK.value(), "Book deleted Successfully ");
        }
        return new Response(HttpStatus.OK.value(), "Unauthorized Access !!");
    }

    @Override
    public Response applyForApproval(long bookId, String token) throws IOException {
        long id = JwtGenerator.decodeJWT(token);
        BookModel book = bookRepository.findByBookId(bookId);
        book.setForApproval(true);
        bookRepository.save(book);
        sellerElasticService.updateBookForElasticSearch(book);
        List<BookModel> response = adminElasticService.getUnverifiedBooks();
        if (response == null || response.isEmpty()) adminElasticService.addBookDetails(book);
        else if (response.stream().anyMatch(v -> v.getBookId() == bookId)) adminElasticService.updateBookForElasticSearch(book);
        else adminElasticService.addBookDetails(book);
        return new Response(HttpStatus.OK.value(), "Goes to admin for verification");
    }
}
