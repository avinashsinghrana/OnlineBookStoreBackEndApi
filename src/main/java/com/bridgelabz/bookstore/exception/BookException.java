package com.bridgelabz.bookstore.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
public class BookException extends Exception {
    private String message;
    HttpStatus status;
    LocalDateTime time;

    public BookException(String message,HttpStatus status) {
        super(message);
        this.message = message;
        this.status=status;
    }
}
