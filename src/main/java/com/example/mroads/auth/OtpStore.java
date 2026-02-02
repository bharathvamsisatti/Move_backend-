package com.example.mroads.auth;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class OtpStore {

	private final Map<String, String> otpMap = new ConcurrentHashMap<>();
	private final Map<String, Long> expiryMap = new ConcurrentHashMap<>();

	public void save(String email, String otp) {
		otpMap.put(email, otp);
		expiryMap.put(email, System.currentTimeMillis() + (5 * 60 * 1000)); // 5 min
	}

	public boolean verify(String email, String otp) {
		if (!otpMap.containsKey(email))
			return false;
		if (System.currentTimeMillis() > expiryMap.get(email))
			return false;
		return otpMap.get(email).equals(otp);
	}

	public void clear(String email) {
		otpMap.remove(email);
		expiryMap.remove(email);
	}
}
