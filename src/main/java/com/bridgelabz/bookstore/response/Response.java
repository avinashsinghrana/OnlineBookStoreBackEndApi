package com.bridgelabz.bookstore.response;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Response {
	
	private int status;
	private String message;
	private Object data;
	private LocalDateTime time;
	private String roleType;
	private String imgUrl;
	private int size;

	public Response(int status, String message, String imgUrl) {
		this.status = status;
		this.message = message;
		this.imgUrl = imgUrl;
	}

	public Response(int status, String message) {
		this.status = status;
		this.message = message;
	}
	
	public Response(String message,int status, Object data) {
		this.message = message;
		this.status = status;
		this.data = data;
	}

	public Response(String message,int status, Object data, String roleType) {
		this.message = message;
		this.status = status;
		this.data = data;
		this.roleType = roleType;
	}

    public Response(int status, String message, int size) {
		this.message = message;
		this.status = status;
		this.size = size;
    }
}

