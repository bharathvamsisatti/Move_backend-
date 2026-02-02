package com.example.mroads.user;

import java.time.LocalDate;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String userName;

	@Column(unique = true)
	private String email;

	private String password;

	private String provider; // LOCAL or GOOGLE
	private String providerId; // Google ID

	// 🔥 NEW — unique user identifier (assigned once)
	@Column(unique = true, updatable = false)
	private String userUuid;

	// 🔽 NEW PROFILE FIELDS
	private String profileImagePath; // profile photo
	private LocalDate dateOfBirth; // birthday
	private String alternatePhoneNumber;
}
