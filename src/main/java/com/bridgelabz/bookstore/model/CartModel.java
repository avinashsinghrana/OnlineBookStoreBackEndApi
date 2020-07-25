package com.bridgelabz.bookstore.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Data
@NoArgsConstructor
@Entity
@Table(name = "Cart")
public class CartModel {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;
	private String bookName;
	private String authorName;
	private String bookImgUrl;
	private Long bookId;
	private long quantity;
	private long actualQuantity;
	private double Price;
    private boolean isInWishList;
	private long userId;
	private String ipAddress;

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		CartModel cartModel = (CartModel) o;
		return userId == cartModel.userId &&
				Objects.equals(bookName, cartModel.bookName) &&
				Objects.equals(authorName, cartModel.authorName) &&
				Objects.equals(bookId, cartModel.bookId);
	}
}