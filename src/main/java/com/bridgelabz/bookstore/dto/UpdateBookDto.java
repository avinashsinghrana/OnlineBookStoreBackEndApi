package com.bridgelabz.bookstore.dto;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import org.springframework.stereotype.Component;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Component
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateBookDto {

    private String bookName;

    private int quantity;

    private double price;

    private String authorName;

    private String bookDetails;

}
