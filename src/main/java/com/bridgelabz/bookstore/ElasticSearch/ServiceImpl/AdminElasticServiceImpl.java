package com.bridgelabz.bookstore.ElasticSearch.ServiceImpl;

import com.bridgelabz.bookstore.ElasticSearch.Service.AdminElasticService;
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
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AdminElasticServiceImpl implements AdminElasticService {

    private final RestHighLevelClient adminClient;

    private final ObjectMapper objectMapper;

    public AdminElasticServiceImpl(RestHighLevelClient client, ObjectMapper objectMapper) {
        this.adminClient = client;
        this.objectMapper = objectMapper;
    }


    @Override
    public void addBookDetails(BookModel bookModel) throws IOException {
        IndexRequest indexRequest = new IndexRequest("admin")
                .source(convertUserElasticSearchModelToMap(bookModel), XContentType.JSON);
        adminClient.index(indexRequest, RequestOptions.DEFAULT);
    }

    private Map<String, Object> convertUserElasticSearchModelToMap(BookModel bookModel) {
        @SuppressWarnings("unchecked")
        Map<String, Object> obj = objectMapper.convertValue(bookModel, Map.class);
        System.out.println("obj.values() = " + obj.values());
        return obj;
    }

    private List<BookModel> listConverter(SearchHits search) {
        List<BookModel> userBook = new ArrayList<>();
        for (SearchHit hit : search.getHits()) {
            BookModel bookModel = objectMapper.convertValue(hit.getSourceAsMap(), BookModel.class);
            userBook.add(bookModel);
        }
        System.out.println("userBook = " + userBook.toString());
        return userBook;
    }

    @Override
    public List<BookModel> getUnverifiedBooks() throws IOException {
        SearchRequest searchRequest = new SearchRequest("admin");
        SearchResponse searchResponse = adminClient.search(searchRequest, RequestOptions.DEFAULT);
        List<BookModel> bookModels = listConverter(searchResponse.getHits());
        return bookModels.stream().filter(BookModel::isForApproval).collect(Collectors.toList());
    }

    @Override
    public void deleteBookForElasticSearch(Long bookId) throws IOException {
        SearchRequest searchRequest = new SearchRequest("admin");
        QueryBuilder matchQueryBuilder = QueryBuilders.matchQuery("bookId", bookId);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchRequest.source(searchSourceBuilder.query(matchQueryBuilder));
        SearchResponse searchResponse = adminClient.search(searchRequest, RequestOptions.DEFAULT);
        String docId = "";
        for (SearchHit hit : searchResponse.getHits()) {
            docId = hit.getId();
        }
        DeleteRequest deleteRequest = new DeleteRequest("admin", docId);
        adminClient.delete(deleteRequest, RequestOptions.DEFAULT);
    }

    @Override
    public void updateBookForElasticSearch(BookModel book) throws IOException {
        SearchRequest searchRequest = new SearchRequest("admin");
        QueryBuilder matchQueryBuilder = QueryBuilders.matchQuery("bookId", book.getBookId());
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchRequest.source(searchSourceBuilder.query(matchQueryBuilder));
        SearchResponse searchResponse = adminClient.search(searchRequest, RequestOptions.DEFAULT);
        String docId = "";
        for (SearchHit hit : searchResponse.getHits()) {
            docId = hit.getId();
        }
        UpdateRequest updateRequest = new UpdateRequest("admin", docId)
                .doc(convertUserElasticSearchModelToMap(book), XContentType.JSON);
        adminClient.update(updateRequest, RequestOptions.DEFAULT);
    }

    @Override
    public List<BookModel> searchBookElasticSearch(long sellerId) throws IOException {
        SearchRequest searchRequest = new SearchRequest("admin");
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchRequest.source(searchSourceBuilder.query(QueryBuilders
                .matchQuery("sellerId",sellerId)
                .operator(Operator.AND)));
        SearchResponse searchResponse = adminClient.search(searchRequest,RequestOptions.DEFAULT);
        return listConverter(searchResponse.getHits());
    }

}
