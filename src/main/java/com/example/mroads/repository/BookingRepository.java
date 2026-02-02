package com.example.mroads.repository;

import com.example.mroads.entity.Booking;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

	List<Booking> findByBookedByUuid(String bookedByUuid);

	List<Booking> findByRideId(Long rideId);
}
