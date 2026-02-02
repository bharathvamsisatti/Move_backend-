package com.example.mroads.repository;

import com.example.mroads.entity.Booking;
import com.example.mroads.entity.Ride;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface RideRepository extends JpaRepository<Ride, Long>, JpaSpecificationExecutor<Ride> {
	List<Ride> findByDepartureDateGreaterThanEqualOrderByDepartureDateAscDepartureTimeAsc(LocalDate date);

	List<Ride> findByDepartureDateLessThanOrderByDepartureDateDescDepartureTimeDesc(LocalDate date);

	List<Ride> findByCreatedByUuidAndDepartureDateGreaterThanEqualOrderByDepartureDateAscDepartureTimeAsc(
			String createdByUuid, LocalDate date);

	List<Ride> findByCreatedByUuidAndDepartureDateLessThanOrderByDepartureDateDescDepartureTimeDesc(
			String createdByUuid, LocalDate date);

	List<Ride> findByCreatedByUuidOrderByDepartureDateDescDepartureTimeDesc(String createdByUuid);

}
