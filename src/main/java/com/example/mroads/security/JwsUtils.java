package com.example.mroads.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwsUtils {

	@Value("${jwt.secret}")
	private String secret;

	@Value("${jwt.expiration:3600000}")
	private long expirationTime;

	private Key secretKey;

	@PostConstruct
	public void init() {
		this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
	}

	// ✅ UUID AS SUBJECT
	public String generateToken(String userUuid) {
		return Jwts.builder().setSubject(userUuid) // ⭐ UUID HERE
				.setIssuedAt(new Date()).setExpiration(new Date(System.currentTimeMillis() + expirationTime))
				.signWith(secretKey, SignatureAlgorithm.HS256).compact();
	}

	public Key getSecretKey() {
		return secretKey;
	}
}
