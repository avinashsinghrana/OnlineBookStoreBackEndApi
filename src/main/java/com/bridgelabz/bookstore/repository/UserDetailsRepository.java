package com.bridgelabz.bookstore.repository;

import com.bridgelabz.bookstore.model.UserDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface UserDetailsRepository extends JpaRepository<UserDetails,Long> {
    UserDetails findByAddressAndUserId(String address, long id);

}
