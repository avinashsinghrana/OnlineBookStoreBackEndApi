package com.bridgelabz.bookstore.model;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Data
@Entity
public class OrderPlaced {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;
    private long userId;
    private long bookId;
    private long orderId;
    private int quantity;
    private double price;
    private ZoneId zid = ZoneId.systemDefault();
    private ZonedDateTime orderDate =  ZonedDateTime.now(zid);
}