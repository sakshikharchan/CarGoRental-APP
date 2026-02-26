package com.example.demo.DTOs;

import lombok.Data;

@Data
public class RegisterRequest {

	 private String fullName;
	    private String email;
	    private String password;
	    private String phone;
	    private String address;
	    private String roleName; // ROLE_ADMIN, ROLE_VENDOR, ROLE_CUSTOMER, ROLE_DRIVER

}
