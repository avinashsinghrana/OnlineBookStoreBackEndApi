package com.bridgelabz.bookstore.ElasticSearch.Service;

import com.bridgelabz.bookstore.model.BookModel;

import java.io.IOException;
import java.util.List;

public interface AdminElasticService {

    void addBookDetails(BookModel bookModel) throws IOException;

    List<BookModel> getUnverifiedBooks() throws IOException;

    void deleteBookForElasticSearch(Long bookId) throws IOException;

    void updateBookForElasticSearch(BookModel book) throws IOException;

    List<BookModel> searchBookElasticSearch(long sellerId) throws IOException;

}
