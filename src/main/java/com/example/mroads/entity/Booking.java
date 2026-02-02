package com.example.mroads.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Booking {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotBlank(message = "Passenger name is required")
	private String passengerName;

	@Pattern(regexp = "^\\+?[0-9. ()-]{7,13}$", message = "Invalid phone number format")
	@NotBlank(message = "Phone number is required")
	private String phoneNumber;

	@Min(value = 1, message = "Must book at least one seat")
	private int seatsBooked;

	private Double finalPrice;

	// 🔹 When booking was made
	@JsonFormat(pattern = "dd-MM-yyyy HH:mm")
	private LocalDateTime bookTime;

	// 🔹 Link to ride — hidden from frontend JSON to avoid recursion
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ride_id")
	@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })

	private Ride ride;

	// 🔹 UUID of the user who made this booking (set from JWT)
	@Column(name = "booked_by_uuid", updatable = false)
	private String bookedByUuid;

	@Column(nullable = false)
	private String status; // CONFIRMED, CANCELLED

}
