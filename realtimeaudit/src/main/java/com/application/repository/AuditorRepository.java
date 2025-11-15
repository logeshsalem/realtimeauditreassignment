package com.application.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.application.entities.Auditors;
import com.application.entities.Auditors.AvailabilityStatus;

@Repository
public interface AuditorRepository extends JpaRepository<Auditors, Integer> {
	
	List<Auditors> findByAvailabilityStatus(Auditors.AvailabilityStatus status);

	Optional<Auditors> findByName(String assignedAuditorName);
	
	List<Auditors> findByIdNotAndAvailabilityStatus(int id, Auditors.AvailabilityStatus status);
	
	 @Query("SELECT a FROM Auditors a WHERE a.availabilityStatus = 'AVAILABLE' AND a.id NOT IN (SELECT ap.auditors.id FROM AuditPlan ap)")
	    List<Auditors> findAvailableAndUnassignedAuditors();
	 
	 //List<Auditors> findByAvailabilityStatus1(Auditors.AvailabilityStatus status);

}
