package com.bridgelabz.bookstore.ElasticSearch.Service;

import com.bridgelabz.bookstore.model.BookModel;
import com.bridgelabz.bookstore.response.Response;

import java.io.IOException;
import java.util.List;

public interface UserElasticService {

    List<BookModel> searchByBookName(String tittle) throws IOException;

    List<BookModel> searchByAuthor(String tittle) throws IOException;

    List<BookModel> sortAscendingByPrice() throws IOException;

    List<BookModel> sortDescendingByPrice() throws IOException;

    List<BookModel> getAllBook() throws IOException;

    void addBookForElasticSearch(BookModel bookModel) throws IOException;

    void deleteBookForElasticSearch(long bookId) throws IOException;

    void updateBookForElasticSearch(BookModel book) throws IOException;
}
