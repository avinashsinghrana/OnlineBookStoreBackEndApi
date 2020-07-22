package com.bridgelabz.bookstore.ElasticSearch.ServiceImpl;

import com.bridgelabz.bookstore.ElasticSearch.Service.SellerElasticService;
import com.bridgelabz.bookstore.model.BookModel;
import com.bridgelabz.bookstore.model.SellerModel;
import com.bridgelabz.bookstore.model.UserModel;
import com.bridgelabz.bookstore.repository.SellerRepository;
import com.bridgelabz.bookstore.repository.UserRepository;
import com.bridgelabz.bookstore.response.Response;
import com.bridgelabz.bookstore.utility.JwtGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.tomcat.jni.User;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.Requests;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.IndexService;
import org.elasticsearch.index.IndexSettings;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SellerElasticServiceImpl implements SellerElasticService {

    @Autowired
    private UserRepository userRepository;

    private final RestHighLevelClient sellerClient;

    private final ObjectMapper objectMapper;

    public SellerElasticServiceImpl(RestHighLevelClient client, ObjectMapper objectMapper) {
        this.sellerClient = client;
        this.objectMapper = objectMapper;
    }

    @Override
    public void addBookForElasticSearch(BookModel bookModel) throws IOException {
        IndexRequest indexRequest = new IndexRequest("seller")
                .source(convertUserElasticSearchModelToMap(bookModel), XContentType.JSON);
        sellerClient.index(indexRequest, RequestOptions.DEFAULT);
    }

    private Map<String, Object> convertUserElasticSearchModelToMap(BookModel bookModel) {
        @SuppressWarnings("unchecked")
        Map<String, Object> obj = objectMapper.convertValue(bookModel, Map.class);
        System.out.println("obj.values() = " + obj.values());
        return obj;
    }

    @Override
    public void updateBookForElasticSearch(BookModel bookModel) throws IOException {
        SearchRequest searchRequest = new SearchRequest("seller");
        QueryBuilder matchQueryBuilder = QueryBuilders.matchQuery("bookId", bookModel.getBookId());
        // getting request to call RestHighLevelClient
        // for above query , we need index id of that index.... we get that index id by calling
        // getDocumentId() : locally defined method structure which return Id of string type
        UpdateRequest updateRequest = new UpdateRequest("seller", getDocumentId(searchRequest, matchQueryBuilder))
                .doc(convertUserElasticSearchModelToMap(bookModel), XContentType.JSON);
        sellerClient.update(updateRequest, RequestOptions.DEFAULT);
    }

    // to get document Id for update and delete inside elasticsearch node
    private String getDocumentId(SearchRequest searchRequest, QueryBuilder matchQueryBuilder) throws IOException {
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchRequest.source(searchSourceBuilder.query(matchQueryBuilder));
        SearchResponse searchResponse = sellerClient.search(searchRequest, RequestOptions.DEFAULT);
        String docId = "";
        for (SearchHit hit : searchResponse.getHits()) {
            docId = hit.getId();
        }
        return docId;
    }

    @Override
    public void deleteBookForElasticSearch(Long bookId) throws IOException {
        SearchRequest searchRequest = new SearchRequest("seller");
        QueryBuilder matchQueryBuilder = QueryBuilders.matchQuery("bookId", bookId);
        DeleteRequest deleteRequest = new DeleteRequest("seller", getDocumentId(searchRequest, matchQueryBuilder));
        sellerClient.delete(deleteRequest, RequestOptions.DEFAULT);
    }

    @Override
    public List<BookModel> searchByBookName(String bookName) throws IOException {
        SearchRequest searchRequest = new SearchRequest("seller");
        QueryBuilder matchQueryBuilder = QueryBuilders.matchQuery("bookName", bookName);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchRequest.source(searchSourceBuilder.query(matchQueryBuilder));
        SearchResponse searchResponse = sellerClient.search(searchRequest, RequestOptions.DEFAULT);
        return listConverter(searchResponse.getHits());
    }

    @Override
    public List<BookModel> searchByAuthor(String authorName) throws IOException {
        SearchRequest searchRequest = new SearchRequest("seller");
        QueryBuilder matchQueryBuilder = QueryBuilders.matchQuery("authorName", authorName);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchRequest.source(searchSourceBuilder.query(matchQueryBuilder));
        SearchResponse searchResponse = sellerClient.search(searchRequest, RequestOptions.DEFAULT);
        return listConverter(searchResponse.getHits());
    }

    @Override
    public List<BookModel> getAllBooks(String token) throws IOException {
        long id = JwtGenerator.decodeJWT(token);
        UserModel seller = userRepository.findByUserId(id);
        SearchRequest searchRequest = new SearchRequest("seller");
        QueryBuilder matchQueryBuilder = QueryBuilders.matchQuery("sellerId", seller.getUserId());
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchRequest.source(searchSourceBuilder.query(matchQueryBuilder));
        SearchResponse searchResponse = sellerClient.search(searchRequest, RequestOptions.DEFAULT);
        return listConverter(searchResponse.getHits());
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
}
