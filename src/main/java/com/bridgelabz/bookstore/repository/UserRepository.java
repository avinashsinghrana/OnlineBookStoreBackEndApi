package com.bridgelabz.bookstore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.bridgelabz.bookstore.model.UserModel;

@Repository
public interface UserRepository extends JpaRepository<UserModel, Long> {
	UserModel findByUserId(long userId);
	UserModel findByEmailId(String emailId);
}
