package com.application.controller;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.application.DTO.AuditPlanDTO;
import com.application.DTO.AuditPlanResponseDTO;
import com.application.entities.AuditPlan;
import com.application.service.AuditPlanService;
import com.application.service.AuditPlanServiceImpl;

@RestController
@RequestMapping("/api")
public class AuditPlanController {
	
	private static final Logger logger = LoggerFactory.getLogger(AuditPlanServiceImpl.class);
	
	@Autowired
    private AuditPlanService auditPlanService;

	@PostMapping("/process") // Or "/assignments/generate-plan" to match Streamlit
	public ResponseEntity<?> generateAuditPlan() { // Changed return type to wildcard for flexibility
	    try {
	        // --- CRITICAL FIX: The service now returns a list of DTOs, not entities ---
	        List<AuditPlanResponseDTO> newPlanDTOs = auditPlanService.generateAndSaveAuditPlan();
	        
	        // This response is now safe from lazy loading errors and structured for your Streamlit app.
	        return ResponseEntity.ok(Map.of(
	            "message", "Successfully generated " + newPlanDTOs.size() + " new assignments.",
	            "assignments", newPlanDTOs
	        ));
	        
	    } catch (Exception e) {
	        logger.error("Error during audit plan generation", e); // Use a logger here
	        return ResponseEntity
	                .status(HttpStatus.INTERNAL_SERVER_ERROR)
	                .body(Map.of("error", "An internal server error occurred.", "details", e.getMessage()));
	    }
	}
    
    @PutMapping("/audit-plan/{id}")
    public ResponseEntity<?> updateAuditPlan(
            @PathVariable int id,
            @RequestBody AuditPlanDTO updateDTO) {
        
        try {
            AuditPlanResponseDTO updatedPlan = auditPlanService.updateAuditPlanStatus(id, updateDTO);
            return ResponseEntity.ok(updatedPlan);
        
        } catch (RuntimeException ex) {
            // This will catch the "AuditPlan not found" exception from the service
            logger.error("Error updating AuditPlan with ID {}: {}", id, ex.getMessage());
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND) // Return 404 Not Found
                    .body(Map.of("error", ex.getMessage()));
        
        } catch (Exception e) {
            // A general catch-all for other unexpected errors
            logger.error("An unexpected error occurred while updating AuditPlan ID {}", id, e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header("Error-Message", e.getMessage()) 
                    .build();
        }
    }
    
    @GetMapping("/audit-plans")
    public ResponseEntity<?> findAllAuditPlans() {
        try {
            List<AuditPlanResponseDTO> allPlans = auditPlanService.findAllAuditPlans();
            return ResponseEntity.ok(allPlans);
        
        } catch (Exception e) {
            // A general catch-all for any unexpected errors during fetch
            logger.error("An error occurred while fetching all audit plans", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header("Error-Message", e.getMessage()) 
                    .build();
        }
    }

}
