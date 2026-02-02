package com.example.mroads.auth;

import lombok.Data;

@Data
public class VerifyOtpRequest {
	private String email;
	private String otp;
}
