package com.bridgelabz.bookstore.model;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.elasticsearch.annotations.Document;


@Entity
@Table(name = "Book")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BookModel {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long bookId;

	@NotBlank(message= "BookName is mandatory")
	private String bookName;

//	@Min(1)
	private Integer quantity;

	private Long sellerId;

//	@Min(1)
	private Double price;

	@Size(min=2,max=30)
	private String authorName;

	private ZoneId zid = ZoneId.systemDefault();

	private ZonedDateTime createdDateAndTime = ZonedDateTime.now(zid);

	private ZonedDateTime UpdatedDateAndTime;

	private boolean isDisapproved;
	private boolean forApproval;
	@Column
	@NotNull
	private String bookDetails;
	
	@Column(nullable = false)
	private boolean isVerfied;

//	@ManyToMany(cascade = CascadeType.ALL)
//	@JoinColumn(name = "user_id")
//	private List<UserModel> users;
	
//	@ManyToMany(cascade = CascadeType.ALL)
//	@JoinColumn(name = "seller_id")
//	private List<SellerModel> sellers;
	
	@Column
	private String bookImgUrl;
	
		
}