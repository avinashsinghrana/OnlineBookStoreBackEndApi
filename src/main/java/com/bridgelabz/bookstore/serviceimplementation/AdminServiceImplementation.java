package com.bridgelabz.bookstore.serviceimplementation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.bridgelabz.bookstore.ElasticSearch.Service.AdminElasticService;
import com.bridgelabz.bookstore.ElasticSearch.Service.SellerElasticService;
import com.bridgelabz.bookstore.ElasticSearch.Service.UserElasticService;
import com.bridgelabz.bookstore.dto.SellerDTO;
import com.bridgelabz.bookstore.model.SellerModel;
import com.bridgelabz.bookstore.model.UserModel;
import com.bridgelabz.bookstore.response.EmailObject;
import com.bridgelabz.bookstore.utility.RabbitMQSender;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.bridgelabz.bookstore.exception.UserNotFoundException;
import com.bridgelabz.bookstore.model.BookModel;
import com.bridgelabz.bookstore.repository.BookRepository;
import com.bridgelabz.bookstore.repository.UserRepository;
import com.bridgelabz.bookstore.response.Response;
import com.bridgelabz.bookstore.service.AdminService;
import com.bridgelabz.bookstore.utility.JwtGenerator;

@Service
public class AdminServiceImplementation implements AdminService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private AdminElasticService adminElasticService;

    @Autowired
    private UserElasticService userElasticService;

    @Autowired
    private SellerElasticService sellerElasticService;

    @Autowired
    private Environment environment;

    @Autowired
    private RabbitMQSender rabbitMQSender;

    @Override
    public List<BookModel> getAllUnVerifiedBooks(String token) throws UserNotFoundException, IOException {
        long id = JwtGenerator.decodeJWT(token);
        String role = String.valueOf(userRepository.findByUserId(id).getRoleType());
        if (role.equals("ADMIN")) {
            return adminElasticService.getUnverifiedBooks();
        } else {
            throw new UserNotFoundException("Not Authorized");
        }
    }

    @Override
    public Response bookVerification(Long bookId, Long sellerId, String token) throws UserNotFoundException, IOException {
        long id = JwtGenerator.decodeJWT(token);
        String role = String.valueOf(userRepository.findByUserId(id).getRoleType());
        if (role.equals("ADMIN")) {
            BookModel book = bookRepository.findByBookId(bookId);
            UserModel seller = userRepository.findByUserId(sellerId);
            book.setVerfied(true);
            book.setDisapproved(false);
            bookRepository.save(book);
            String message =
                    "ONLINE BOOK STORE" +
                    "=================\n\n" +
                    "Hello " + seller.getFullName() + ",\n\n" +
                    "Sorry to Inform that your request for Book Approval got Rejected.\n" +
                    "-----------------------------------------------------------------" +
                    "-----------------------------------------------------------------\n" +
                    "Book Details : " +
                    "--------------" +
                    "Book Name : " + book.getBookName() + "\n" +
                    "Author Name: " + book.getAuthorName() + "\n" +
                    "Book Price : " + book.getPrice() + "\n" +
                    "----------------------------------------------------------------" +
                    "\n\n" +
                    "Have a great Experience with us !!" +
                    "\n\n\n\n" +
                    "Thank you,\n" +
                    "Online Book Store Team, Bangalore\n";
            rabbitMQSender.send(new EmailObject(seller.getEmailId(), "Book Approved Response" +
                    book.getBookName(), message, "Congrats for Approval"));
            sellerElasticService.updateBookForElasticSearch(book);
            userElasticService.addBookForElasticSearch(book);
            adminElasticService.updateBookForElasticSearch(book);
            return new Response(environment.getProperty("book.verified.successfull"), HttpStatus.OK.value(), book);
        } else {
            throw new UserNotFoundException("Not Authorized");
        }
    }

    @Override
    public Response bookDisApprove(Long bookId, Long sellerId, String token) throws IOException, UserNotFoundException {
        long id = JwtGenerator.decodeJWT(token);
        String role = String.valueOf(userRepository.findByUserId(id).getRoleType());
        if (role.equals("ADMIN")) {
            BookModel book = bookRepository.findByBookId(bookId);
            UserModel seller = userRepository.findByUserId(sellerId);
            String message =
                    "ONLINE BOOK STORE" +
                    "=================\n\n" +
                    "Hello " + seller.getFullName() + ",\n\n" +
                    "Congratulation to Inform that your request for Book Approval got Approved.\n" +
                    "-----------------------------------------------------------------" +
                    "-----------------------------------------------------------------\n" +
                    "Book Details : " +
                    "--------------" +
                    "Book Name : " + book.getBookName() + "\n" +
                    "Author Name: " + book.getAuthorName() + "\n" +
                    "Book Price : " + book.getPrice() + "\n" +
                    "-----------------------------------------------------------------" +
                    "Description of Rejection : \n" +
                    "--------------------------" +
                    "Your Request for approval has been rejected because it doesn't fulfilled\n" +
                    "Terms & Conditions of company policies.\n" +
                    "------------------------------------------------------------------------" +
                    "\n\n" +
                    "You can again apply for Approval." +
                    "\n\n\n\n" +
                    "Thank you,\n" +
                    "Online Book Store Team, Bangalore\n";
            rabbitMQSender.send(new EmailObject(seller.getEmailId(), "Response for " +
                    book.getBookName(), message, "Book Approval Response"));
            book.setDisapproved(true);
            book.setVerfied(false);
            bookRepository.save(book);
            sellerElasticService.updateBookForElasticSearch(book);
            adminElasticService.updateBookForElasticSearch(book);
            return new Response(environment.getProperty("book.verified.successfull"), HttpStatus.OK.value(), book);
        } else {
            throw new UserNotFoundException("Not Authorized");
        }
    }

    @Override
    public List<SellerDTO> getSellerListRequestedForApproval() throws IOException {
        Set<Long> sellerIdList = adminElasticService.getUnverifiedBooks().stream().map(BookModel::getSellerId).collect(Collectors.toSet());
        List<SellerDTO> sellerDetails = new ArrayList<>();
        for (Long sellerId : sellerIdList) {
            UserModel user = userRepository.findByUserId(sellerId);
            SellerDTO sellerDTO = new SellerDTO();
            BeanUtils.copyProperties(user, sellerDTO);
            sellerDTO.setQuantity(adminElasticService.searchBookElasticSearch(sellerId).size());
            sellerDetails.add(sellerDTO);
        }
        return sellerDetails;
    }
}