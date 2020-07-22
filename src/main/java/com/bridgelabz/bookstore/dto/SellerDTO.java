package com.bridgelabz.bookstore.dto;

import lombok.Data;

@Data
public class SellerDTO {
    private long userId;
    private String profileUrl;
    private String fullName;
    private int quantity;
}
