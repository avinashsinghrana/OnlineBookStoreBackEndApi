package com.bridgelabz.bookstore.repository;

import com.bridgelabz.bookstore.model.OrderPlaced;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<OrderPlaced, Long> {
    boolean existsByOrderId(long orderId);
}
