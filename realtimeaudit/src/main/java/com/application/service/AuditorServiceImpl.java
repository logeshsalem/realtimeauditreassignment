package com.application.service;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.application.entities.AuditPlan;
import com.application.entities.Auditors;
import com.application.entities.Auditors.AvailabilityStatus;
import com.application.repository.AuditPlanRepository;
import com.application.repository.AuditorRepository;

@Service
public class AuditorServiceImpl implements AuditorService {

	private static final Logger logger = LoggerFactory.getLogger(AuditPlanServiceImpl.class);
	
	private final AuditorRepository auditorRepository;
	
	private final AuditPlanRepository auditPlanRepository;
	
	private final AuditPlanService auditPlanService;
	
	@Autowired
	public AuditorServiceImpl(AuditorRepository auditorRepository, AuditPlanRepository auditPlanRepository, AuditPlanService auditPlanService) {
		this.auditorRepository = auditorRepository;
		this.auditPlanRepository = auditPlanRepository;
		this.auditPlanService = auditPlanService;
	}

	@Override
	public Auditors saveAuditors(Auditors auditors) {
		
		if(auditors.getAvailabilityStatus()==null) {
			auditors.setAvailabilityStatus(Auditors.AvailabilityStatus.AVAILABLE);
		}
		
		if(auditors.getName()==null || auditors.getName().trim().isEmpty()) 
			throw new IllegalArgumentException("Auditor name must be provided."); {
			
		}
		
		return auditorRepository.save(auditors);
	}

	@Override
	public List<Auditors> findAllAuditors() {
		return auditorRepository.findAll();
	}

	@Override
	public List<Auditors> findAvailableAuditors() {		
		return auditorRepository.findByAvailabilityStatus(AvailabilityStatus.AVAILABLE);
	}

	@Override
	public Optional<Auditors> findByAuditorsId(int id) {
		return auditorRepository.findById(id);
	}

	
	@Override
	public Auditors updateAvailabilityStatus(int id, AvailabilityStatus newStatus) {
		logger.info("Attempting to update status for auditor ID: {} to {}", id, newStatus);
	    
	    // 1. Fetch the auditor from the database. This is the object we will work with.
	    Auditors auditorToUpdate = auditorRepository.findById(id)
	            .orElseThrow(() -> new RuntimeException("Auditor not found with id: " + id));

	    // 2. Get the status BEFORE making any changes. This is crucial for our trigger condition.
	    Auditors.AvailabilityStatus oldStatus = auditorToUpdate.getAvailabilityStatus();
	    
	    logger.info("CHECKPOINT 1: Checking reassignment condition. Old Status = {}, New Status = {}", oldStatus, newStatus);

	    // 3. Set the new status on the entity object.
	    auditorToUpdate.setAvailabilityStatus(newStatus);

	    // 4. Save the updated entity. The 'save' method returns the persisted entity, which we store in a new variable.
	    Auditors savedAuditor = auditorRepository.save(auditorToUpdate);

	    // --- TRIGGER REASSIGNMENT LOGIC ---
	    // The main gatekeeper condition.
	    if (oldStatus == Auditors.AvailabilityStatus.AVAILABLE && newStatus != Auditors.AvailabilityStatus.AVAILABLE) {
	        
	        logger.info("CHECKPOINT 2: Condition MET. Auditor status changed from AVAILABLE. Triggering reassignment process.");

	        // Find all assignments using the SAVED auditor entity.
	        List<AuditPlan> affectedAssignments = auditPlanRepository.findByAuditors(savedAuditor);
	        logger.info("CHECKPOINT 3: Found {} affected assignments for this auditor.", affectedAssignments.size());

	        if (!affectedAssignments.isEmpty()) {
	            // Find all other auditors who are available to take over the work.
	            List<Auditors> candidateAuditors = auditorRepository.findByIdNotAndAvailabilityStatus(id, Auditors.AvailabilityStatus.AVAILABLE);
	            logger.info("CHECKPOINT 4: Found {} available replacement candidates.", candidateAuditors.size());
	            
	            int reassignmentCounter = 0;
	            for (AuditPlan assignment : affectedAssignments) {
	                reassignmentCounter++;
	                logger.info("--> Processing reassignment {} of {}: Store ID {}", reassignmentCounter, affectedAssignments.size(), assignment.getStore().getId());
	                
	                // Call the service that handles the Python API communication.
	                auditPlanService.reassignStore(assignment.getStore(), candidateAuditors);
	            }
	            logger.info("CHECKPOINT 5: Reassignment loop finished.");

	        } else {
	             logger.warn("No active assignments found for auditor ID: {}. Nothing to reassign.", id);
	        }
	    } else {
	        logger.warn("CHECKPOINT 2: Condition NOT MET. No reassignment will be triggered.");
	    }

	    // 5. Return the final, saved state of the auditor.
	    return savedAuditor;
	}
}
