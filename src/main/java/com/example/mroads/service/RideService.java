package com.example.mroads.service;

import com.example.mroads.entity.Booking;
import com.example.mroads.entity.Ride;
import com.example.mroads.repository.BookingRepository;
import com.example.mroads.repository.RideRepository;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

@Service
@RequiredArgsConstructor
public class RideService {

	private final RideRepository rideRepository;
	private final BookingRepository bookingRepository;

	public Ride addRide(Ride ride) {
		if (ride.getAvailableSeats() > ride.getTotalSeats()) {
			throw new IllegalArgumentException("Available seats cannot exceed total seats");
		}

		if (ride.getCreatedByUuid() == null || ride.getCreatedByUuid().isBlank()) {
			throw new IllegalArgumentException("User not identified. Please login again.");
		}

		return rideRepository.save(ride);
	}

	// ---------- JSON + Image (/add-with-image) ----------

	public Ride addRideWithImage(Ride ride, MultipartFile image) throws IOException {

		if (ride.getAvailableSeats() > ride.getTotalSeats()) {
			throw new IllegalArgumentException("Available seats cannot exceed total seats");
		}

		if (ride.getCreatedByUuid() == null || ride.getCreatedByUuid().isBlank()) {
			throw new IllegalArgumentException("User not identified. Please login again.");
		}

		String plateNumber = null;

		if (ride.getVehicleNumber() != null && !ride.getVehicleNumber().isBlank()) {
			plateNumber = ride.getVehicleNumber().trim().toUpperCase();
		}

		if (image != null && !image.isEmpty()) {
			String imagePath = saveImageLocally(image);
			ride.setVehicleImagePath(imagePath);

			String ocrNumber = extractVehicleNumberFromImage(imagePath);

			if ((plateNumber == null || plateNumber.isBlank()) && !"OCR_FAIL".equalsIgnoreCase(ocrNumber)) {
				plateNumber = ocrNumber;
			}
		}

		if (plateNumber == null || plateNumber.isBlank()) {
			throw new IllegalArgumentException("Unable to detect vehicle number. Please enter manually.");
		}

		ride.setVehicleNumber(plateNumber);

		return rideRepository.save(ride);
	}

	// ---------- Search ----------
	public List<Ride> searchRides(String departure, String destination, LocalDate date) {

		Specification<Ride> spec = (root, query, cb) -> {
			List<Predicate> predicates = new ArrayList<>();

			if (departure != null && !departure.isBlank()) {
				predicates.add(cb.like(cb.lower(root.get("departureLocation")), "%" + departure.toLowerCase() + "%"));
			}

			if (destination != null && !destination.isBlank()) {
				predicates
						.add(cb.like(cb.lower(root.get("destinationLocation")), "%" + destination.toLowerCase() + "%"));
			}

			if (date != null) {
				predicates.add(cb.equal(root.get("departureDate"), date));
			}

			predicates.add(cb.greaterThan(root.get("availableSeats"), 0));

			return cb.and(predicates.toArray(new Predicate[0]));
		};

		return rideRepository.findAll(spec);
	}

	// ---------- Booking ----------
	@Transactional
	public Booking processBooking(Long rideId, Booking bookingDetails, String bookedByUuid) {

	    Ride ride = rideRepository.findById(rideId)
	            .orElseThrow(() -> new IllegalArgumentException("Ride not found"));

	    // 🚫 BLOCK PAST RIDES (DATE)
	    if (ride.getDepartureDate().isBefore(LocalDate.now())) {
	        throw new IllegalStateException("Cannot book a past ride");
	    }

	    // 🚫 BLOCK SAME-DAY PAST TIME
	    if (
	        ride.getDepartureDate().isEqual(LocalDate.now()) &&
	        ride.getDepartureTime().isBefore(LocalTime.now())
	    ) {
	        throw new IllegalStateException("Ride departure time already passed");
	    }

	    int seatsToBook = bookingDetails.getSeatsBooked();

	    if (seatsToBook <= 0) {
	        throw new IllegalArgumentException("Invalid number of seats");
	    }

	    if (ride.getAvailableSeats() < seatsToBook) {
	        throw new IllegalStateException("Not enough seats available");
	    }

	    // 🔗 attach booking -> ride
	    bookingDetails.setRide(ride);

	    // 🔐 store who booked (UUID from JWT)
	    bookingDetails.setBookedByUuid(bookedByUuid);

	    bookingDetails.setBookTime(LocalDateTime.now());
	    bookingDetails.setFinalPrice(ride.getPricePerSeat() * seatsToBook);
	    bookingDetails.setStatus("CONFIRMED");

	    // 🔻 reduce available seats
	    ride.setAvailableSeats(ride.getAvailableSeats() - seatsToBook);
	    rideRepository.save(ride);

	    return bookingRepository.save(bookingDetails);
	}


	// ---------- Helpers ----------

	private String saveImageLocally(MultipartFile file) throws IOException {
		Path uploadDir = Paths.get("uploads");
		if (!Files.exists(uploadDir)) {
			Files.createDirectories(uploadDir);
		}

		String originalName = file.getOriginalFilename();
		String ext = "";
		if (originalName != null && originalName.contains(".")) {
			ext = originalName.substring(originalName.lastIndexOf('.'));
		}

		String newFileName = UUID.randomUUID() + ext;
		Path targetPath = uploadDir.resolve(newFileName).normalize();

		Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

		// stored path on disk – you can convert this to a URL later
		return targetPath.toString();
	}

	private String extractVehicleNumberFromImage(String imagePath) {
		try {
			ITesseract tess = new Tesseract();
			tess.setDatapath("C:/Program Files/Tesseract-OCR/tessdata");
			tess.setLanguage("eng");

			String text = tess.doOCR(new File(imagePath));

			// Extract using regex: AA00AA0000
			Pattern pattern = Pattern.compile("[A-Z]{2}[0-9]{2}[A-Z]{1,2}[0-9]{4}");
			Matcher matcher = pattern.matcher(text.replaceAll("[^A-Za-z0-9]", ""));

			if (matcher.find()) {
				return matcher.group().toUpperCase();
			}
		} catch (Exception e) {
			System.err.println("OCR Error: " + e.getMessage());
		}

		return "OCR_FAIL";

	}

	public List<Ride> getUpcomingRidesForDashboard() {
		LocalDate today = LocalDate.now();
		return rideRepository.findByDepartureDateGreaterThanEqualOrderByDepartureDateAscDepartureTimeAsc(today);
	}

	public List<Ride> getPastRidesForDashboard() {
		LocalDate today = LocalDate.now();
		return rideRepository.findByDepartureDateLessThanOrderByDepartureDateDescDepartureTimeDesc(today);
	}

	public List<Ride> getUpcomingRidesForDashboard(String userUuid) {
		LocalDate today = LocalDate.now();
		return rideRepository
				.findByCreatedByUuidAndDepartureDateGreaterThanEqualOrderByDepartureDateAscDepartureTimeAsc(userUuid,
						today);
	}

	public List<Ride> getPastRidesForDashboard(String userUuid) {
		LocalDate today = LocalDate.now();
		return rideRepository
				.findByCreatedByUuidAndDepartureDateLessThanOrderByDepartureDateDescDepartureTimeDesc(userUuid, today);
	}

	// bookings made by a user (passenger)
	public List<Booking> getBookingsForUser(String userUuid) {
		return bookingRepository.findByBookedByUuid(userUuid);
	}

	// bookings for a specific ride (owner can view)
	public List<Booking> getBookingsForRide(Long rideId) {
		return bookingRepository.findByRideId(rideId);
	}

	// delete ride if the requesting user is owner
	@Transactional
	public void deleteRideIfOwner(Long rideId, String userUuid) {
		Ride ride = rideRepository.findById(rideId).orElseThrow(() -> new IllegalArgumentException("Ride not found"));

		if (!userUuid.equals(ride.getCreatedByUuid())) {
			throw new IllegalStateException("Not authorized to delete this ride");
		}

		// optional: delete bookings first (cascade could be configured)
		bookingRepository.findByRideId(rideId).forEach(bookingRepository::delete);

		rideRepository.deleteById(rideId);
	}

	/**
	 * Find ride by id (throws when not found) — used by controllers for ownership
	 * checks.
	 */
	public Ride findRideById(Long rideId) {
		return rideRepository.findById(rideId).orElseThrow(() -> new IllegalArgumentException("Ride not found"));
	}

	public List<Ride> getAllRidesOfferedByUser(String userUuid) {
		return rideRepository.findByCreatedByUuidOrderByDepartureDateDescDepartureTimeDesc(userUuid);
	}

	// ---------- Cancel booking (status-based, NOT delete) ----------
	@Transactional
	public void cancelBooking(Long bookingId, String userUuid) {

		Booking booking = bookingRepository.findById(bookingId)
				.orElseThrow(() -> new IllegalArgumentException("Booking not found"));

		// only booking owner can cancel
		if (!userUuid.equals(booking.getBookedByUuid())) {
			throw new IllegalStateException("Not authorized to cancel this booking");
		}

		// already cancelled → no-op
		if ("CANCELLED".equalsIgnoreCase(booking.getStatus())) {
			return;
		}

		Ride ride = booking.getRide();

		// restore seats
		ride.setAvailableSeats(ride.getAvailableSeats() + booking.getSeatsBooked());

		booking.setStatus("CANCELLED");

		rideRepository.save(ride);
		bookingRepository.save(booking);
	}

}
