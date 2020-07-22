package com.bridgelabz.bookstore.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

@Data
@Component
@NoArgsConstructor
@AllArgsConstructor
public class UserDetailsDTO {
    private String fullName;
    private String phoneNumber;
    private long pinCode;
    private String locality;
    private String address;
    private String city;
    private String state;
    private String landMark;
    private String locationType;
}
