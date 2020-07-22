package com.bridgelabz.bookstore.repository;


import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bridgelabz.bookstore.model.CartModel;

import java.util.List;

@Repository
@Transactional
public interface CartRepository extends JpaRepository<CartModel, Long> {
	CartModel findByBookId(Long book_id);

	void deleteAllByBookId(Long bookId);

	void deleteAllByUserId(long userId);

	void deleteByUserIdAndBookId(long id, Long bookId);

	CartModel findByUserIdAndBookId(long id, Long bookId);

	boolean existsByUserIdAndBookId(long id, Long bookId);

	void deleteAllByUserIdAndBookId(long id, Long bookId);

	List<CartModel> findAllByUserIdAndBookId(long id, Long bookId);

	List<CartModel> findAllByUserId(long userId);

    List<CartModel> findByIpAddress(String ipAddress);

	CartModel findByIpAddressAndBookId(String ipAddress, Long bookId);
}
