package com.bridgelabz.bookstore.ElasticSearch.Service;

import com.bridgelabz.bookstore.model.BookModel;

import java.io.IOException;
import java.util.List;

public interface SellerElasticService {
    void addBookForElasticSearch(BookModel BookModel) throws IOException;

    void updateBookForElasticSearch(BookModel bookModel) throws IOException;

    void deleteBookForElasticSearch(Long bookId) throws IOException;

    List<BookModel> searchByBookName(String bookName) throws IOException;

    List<BookModel> searchByAuthor(String authorName) throws IOException;

    List<BookModel> getAllBooks(String token) throws IOException;

}

