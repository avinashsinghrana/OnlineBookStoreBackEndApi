package com.bridgelabz.bookstore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bridgelabz.bookstore.model.SellerModel;

@Repository
public interface SellerRepository extends JpaRepository<SellerModel,Long> {

    SellerModel findBySellerId(long id);
}
