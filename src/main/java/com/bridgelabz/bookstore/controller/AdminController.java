package com.bridgelabz.bookstore.controller;

import java.io.IOException;
import java.util.List;

import com.bridgelabz.bookstore.ElasticSearch.Service.AdminElasticService;
import com.bridgelabz.bookstore.dto.SellerDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.bridgelabz.bookstore.model.BookModel;
import com.bridgelabz.bookstore.response.Response;
import com.bridgelabz.bookstore.service.AdminService;

@RestController
@RequestMapping("/admin")
public class AdminController {

	@Autowired
	private AdminService adminService;

	@Autowired
	private AdminElasticService adminElasticService;

	@GetMapping("/getBooksForVerification")
	public ResponseEntity<Response> getAllUnverifiedBooks(@RequestParam long sellerId) throws IOException {
		List<BookModel> bookModels = adminElasticService.searchBookElasticSearch(sellerId);
		return ResponseEntity.status(HttpStatus.OK).body(new Response("all unVerified Book", HttpStatus.OK.value(),bookModels));
	}

	@PutMapping("/bookVerification/{sellerId}/{bookId}/{token}")
	public ResponseEntity<Response> bookApproved(@PathVariable("bookId") Long bookId,
													 @PathVariable("sellerId") Long sellerId, @PathVariable("token") String token) throws Exception {
		
		Response verifiedBook =adminService.bookVerification(bookId,sellerId,token);
		return ResponseEntity.status(HttpStatus.OK).body(verifiedBook);
	}

	@PutMapping("/bookDisApprove/{sellerId}/{bookId}/{token}")
	public ResponseEntity<Response> bookDisapproved(@PathVariable("bookId") Long bookId,
													 @PathVariable("sellerId") Long sellerId, @PathVariable("token") String token) throws Exception {

		Response verifiedBook =adminService.bookDisApprove(bookId,sellerId,token);
		return ResponseEntity.status(HttpStatus.OK).body(verifiedBook);
	}

	@GetMapping("/getSellerList")
	public Response getSellerListRequestedForApproval() throws IOException {
		return new Response("All Seller List requested for Approval" ,HttpStatus.OK.value(),adminService.getSellerListRequestedForApproval());
	}

}