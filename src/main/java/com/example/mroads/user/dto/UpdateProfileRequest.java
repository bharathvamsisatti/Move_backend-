package com.example.mroads.user.dto;

import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateProfileRequest {

	@Size(min = 2, max = 60, message = "Name must be between 2 and 60 characters")
	private String name;

	@Past(message = "Date of birth must be in the past")
	private LocalDate dateOfBirth;

	@Pattern(regexp = "^\\+?[0-9]{10,13}$", message = "Alternate phone number is invalid")
	private String alternatePhone;
}
