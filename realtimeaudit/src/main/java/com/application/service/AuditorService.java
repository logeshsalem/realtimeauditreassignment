package com.application.service;

import java.util.List;
import java.util.Optional;

import com.application.entities.Auditors;
import com.application.entities.Auditors.AvailabilityStatus;

public interface AuditorService {
		
	Auditors saveAuditors(Auditors auditors);
	List<Auditors> findAllAuditors();
	List<Auditors> findAvailableAuditors();
	Optional<Auditors> findByAuditorsId(int id);
	Auditors updateAvailabilityStatus(int id, AvailabilityStatus newStatus);
	
}
