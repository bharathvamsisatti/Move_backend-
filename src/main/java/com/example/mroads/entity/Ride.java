package com.example.mroads.entity;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Ride {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotBlank(message = "Driver name is required")
	private String driverName;

	@Pattern(regexp = "^\\+?[0-9. ()-]{7,13}$", message = "Invalid phone number format")
	@NotBlank(message = "Phone number is required")
	private String phoneNumber;

	// 🔹 vehicle number (can be set manually OR from image OCR)
	// @NotBlank(message = "Vehicle number is required")
	private String vehicleNumber;

	@Min(value = 1, message = "Total seats must be at least 1")
	private int totalSeats;

	@Min(value = 0, message = "Available seats cannot be negative")
	private int availableSeats;

	@Min(value = 0, message = "Price per seat cannot be negative")
	private double pricePerSeat;

	@NotBlank(message = "Departure location is required")
	private String departureLocation;

	@NotBlank(message = "Destination location is required")
	private String destinationLocation;

	@NotNull(message = "Departure date is required")
	@FutureOrPresent(message = "Departure date must be today or in the future")
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
	private LocalDate departureDate;

	@NotNull(message = "Departure time is required")
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
	private LocalTime departureTime;

	// 🔹 distance between from & to (km) – from RouteMap
	private Double distanceKm;

	// 🔹 GPS: departure point
	private Double fromLatitude;
	private Double fromLongitude;

	// 🔹 GPS: destination point
	private Double toLatitude;
	private Double toLongitude;

	// 🔹 where we saved the uploaded vehicle image (path or URL)
	private String vehicleImagePath;

	private String createdByUuid;

}
