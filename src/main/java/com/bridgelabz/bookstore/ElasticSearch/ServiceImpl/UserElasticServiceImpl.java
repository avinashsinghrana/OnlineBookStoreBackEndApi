package com.bridgelabz.bookstore.ElasticSearch.ServiceImpl;

import com.bridgelabz.bookstore.ElasticSearch.Service.UserElasticService;
import com.bridgelabz.bookstore.model.BookModel;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.SearchHit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

@Service
public class UserElasticServiceImpl implements UserElasticService {

    private final RestHighLevelClient restHighLevelClient;

    @Autowired
    private final ObjectMapper objectMapper;

    @Autowired
    public UserElasticServiceImpl(RestHighLevelClient client, ObjectMapper objectMapper) {
        this.restHighLevelClient = client;
        this.objectMapper = objectMapper;
    }

    @Override
    public void addBookForElasticSearch(BookModel bookModel) throws IOException {
        IndexRequest indexRequest = new IndexRequest("user")
                .source(convertUserElasticSearchModelToMap(bookModel));
        restHighLevelClient.index(indexRequest, RequestOptions.DEFAULT);
    }

    @Override
    public void deleteBookForElasticSearch(long bookId) throws IOException {
        SearchRequest searchRequest = new SearchRequest("user");
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchRequest.source(searchSourceBuilder.query(QueryBuilders.matchQuery("bookId",bookId)));
        SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        String docId = "";
        for (SearchHit hit : searchResponse.getHits()) { docId = hit.getId(); }
        DeleteRequest deleteRequest = new DeleteRequest("user", docId);
        restHighLevelClient.delete(deleteRequest, RequestOptions.DEFAULT);
    }

    @Override
    public void updateBookForElasticSearch(BookModel book) throws IOException {
        SearchRequest searchRequest = new SearchRequest("user");
        QueryBuilder matchQueryBuilder = QueryBuilders.matchQuery("bookId", book.getBookId());
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchRequest.source(searchSourceBuilder.query(matchQueryBuilder));
        SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        String docId = "";
        for (SearchHit hit : searchResponse.getHits()) {
            docId = hit.getId();
        }
        UpdateRequest updateRequest = new UpdateRequest("user", docId)
                .doc(convertUserElasticSearchModelToMap(book), XContentType.JSON);
        restHighLevelClient.update(updateRequest, RequestOptions.DEFAULT);
    }

    private Map<String, Object> convertUserElasticSearchModelToMap(BookModel bookModel) {
        @SuppressWarnings("unchecked")
        Map<String, Object> obj = objectMapper.convertValue(bookModel, Map.class);
        return obj;
    }

    @Override
    public List<BookModel> searchByBookName(String title) throws IOException {
        SearchRequest searchRequest = new SearchRequest("user");
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchRequest.source(searchSourceBuilder.query(QueryBuilders
                .matchQuery("bookName",title)));
        SearchResponse searchResponse = restHighLevelClient.search(searchRequest,RequestOptions.DEFAULT);
        return listConverter(searchResponse.getHits());
    }

    @Override
    public List<BookModel> searchByAuthor(String authorName) throws IOException {
        SearchRequest searchRequest = new SearchRequest("user");
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchRequest.source(searchSourceBuilder.query(QueryBuilders
                .matchQuery("authorName",authorName)
                .operator(Operator.AND)));
        SearchResponse searchResponse = restHighLevelClient.search(searchRequest,RequestOptions.DEFAULT);
        return listConverter(searchResponse.getHits());
    }

    @Override
    public List<BookModel> sortAscendingByPrice() throws IOException {
        SearchRequest searchRequest = new SearchRequest("user");
        SearchResponse searchResponse = restHighLevelClient.search(searchRequest,RequestOptions.DEFAULT);
        List<BookModel> bookModels = listConverter(searchResponse.getHits());
        bookModels.sort(Comparator.comparing(BookModel::getPrice));
        return bookModels;
    }

    @Override
    public List<BookModel> sortDescendingByPrice() throws IOException {
        SearchRequest searchRequest = new SearchRequest("user");
        SearchResponse searchResponse = restHighLevelClient.search(searchRequest,RequestOptions.DEFAULT);
        List<BookModel> bookModels = listConverter(searchResponse.getHits());
        Comparator<BookModel> comparator = Comparator.comparing(BookModel::getPrice);
        bookModels.sort(comparator.reversed());
        return bookModels;
    }

    @Override
    public List<BookModel> getAllBook() throws IOException {
        SearchRequest searchRequest = new SearchRequest("user");
        SearchResponse searchResponse = restHighLevelClient.search(searchRequest,RequestOptions.DEFAULT);
        return listConverter(searchResponse.getHits());
    }

    private List<BookModel> listConverter(SearchHits search) {
        List<BookModel> userBook = new ArrayList<>();
        for (SearchHit hit : search.getHits()){
            userBook.add(objectMapper.convertValue(hit.getSourceAsMap(), BookModel.class));
        }
        return userBook;
    }
}
