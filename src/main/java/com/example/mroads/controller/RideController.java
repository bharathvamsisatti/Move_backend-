package com.example.mroads.controller;

import com.example.mroads.entity.Booking;
import com.example.mroads.entity.Ride;
import com.example.mroads.service.RideService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/rides")
@RequiredArgsConstructor
public class RideController {

    private final RideService rideService;

    // ===================== ADD RIDE =====================
    @PostMapping("/add")
    public ResponseEntity<Ride> addRide(@Valid @RequestBody Ride ride) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String userUuid = auth.getName();
        ride.setCreatedByUuid(userUuid);

        return ResponseEntity.ok(rideService.addRide(ride));
    }

    // ===================== ADD RIDE WITH IMAGE =====================
    @PostMapping(value = "/add-with-image", consumes = "multipart/form-data")
    public ResponseEntity<?> addRideWithImage(
            @RequestPart("ride") @Valid Ride ride,
            @RequestPart(value = "image", required = false) MultipartFile image) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not authenticated");
        }

        String userUuid = auth.getName();
        ride.setCreatedByUuid(userUuid);

        try {
            Ride savedRide = rideService.addRideWithImage(ride, image);
            return ResponseEntity.ok(savedRide);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to save image: " + e.getMessage());
        }
    }

    // ===================== SEARCH RIDES =====================
    @GetMapping("/search")
    public ResponseEntity<List<Ride>> searchRides(
            @RequestParam String departure,
            @RequestParam String destination,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate departureDate) {

        return ResponseEntity.ok(
                rideService.searchRides(departure, destination, departureDate)
        );
    }

    // ===================== VIEW RIDE =====================
    @GetMapping("/{id}")
    public ResponseEntity<?> getRide(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(rideService.findRideById(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Ride not found");
        }
    }

    // ===================== BOOK RIDE =====================
    @PostMapping("/book/{rideId}")
    public ResponseEntity<?> bookRide(
            @PathVariable Long rideId,
            @Valid @RequestBody Booking bookingDetails) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not authenticated");
        }

        String userUuid = auth.getName();

        try {
            Booking booking = rideService.processBooking(rideId, bookingDetails, userUuid);
            return ResponseEntity.ok(booking);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    // ===================== UPCOMING RIDES =====================
    @GetMapping("/upcoming")
    public ResponseEntity<List<Ride>> getUpcomingRides() {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String userUuid = auth.getName();
        return ResponseEntity.ok(
                rideService.getUpcomingRidesForDashboard(userUuid)
        );
    }

    // ===================== PAST RIDES =====================
    @GetMapping("/past")
    public ResponseEntity<List<Ride>> getPastRides() {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String userUuid = auth.getName();
        return ResponseEntity.ok(
                rideService.getPastRidesForDashboard(userUuid)
        );
    }

    // ===================== MY BOOKINGS =====================
    @GetMapping("/my/bookings")
    public ResponseEntity<List<Booking>> getMyBookings() {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String userUuid = auth.getName();
        return ResponseEntity.ok(
                rideService.getBookingsForUser(userUuid)
        );
    }

    // ===================== BOOKINGS FOR A RIDE (OWNER) =====================
    @GetMapping("/{id}/bookings")
    public ResponseEntity<?> getBookingsForRide(@PathVariable Long id) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String userUuid = auth.getName();
        Ride ride = rideService.findRideById(id);

        if (!userUuid.equals(ride.getCreatedByUuid())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Not authorized");
        }

        return ResponseEntity.ok(
                rideService.getBookingsForRide(id)
        );
    }

    // ===================== DELETE RIDE =====================
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteRide(@PathVariable Long id) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String userUuid = auth.getName();

        try {
            rideService.deleteRideIfOwner(id, userUuid);
            return ResponseEntity.ok("Ride deleted");
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    // ===================== MY OFFERED RIDES =====================
    @GetMapping("/my/offered")
    public ResponseEntity<List<Ride>> getMyOfferedRides() {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String userUuid = auth.getName();
        return ResponseEntity.ok(
                rideService.getAllRidesOfferedByUser(userUuid)
        );
    }

    // ===================== CANCEL BOOKING =====================
    @PutMapping("/booking/{bookingId}/cancel")
    public ResponseEntity<?> cancelBooking(@PathVariable Long bookingId) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String userUuid = auth.getName();

        try {
            rideService.cancelBooking(bookingId, userUuid);
            return ResponseEntity.ok("Booking cancelled");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }
}
