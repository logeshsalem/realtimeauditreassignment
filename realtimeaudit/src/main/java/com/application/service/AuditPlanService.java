package com.application.service;

import java.util.List;

import com.application.DTO.AssignmentRequestDTO;
import com.application.DTO.AssignmentResponseDTO;
import com.application.DTO.AuditPlanDTO;
import com.application.DTO.AuditPlanResponseDTO;
import com.application.entities.AuditPlan;
import com.application.entities.Auditors;
import com.application.entities.Store;

public interface AuditPlanService {
	
	AssignmentResponseDTO getAssignment(AssignmentRequestDTO request);
	
	List<AuditPlanResponseDTO> generateAndSaveAuditPlan();
	
	AssignmentRequestDTO mapEntitiesToRequestDTO(List<Auditors> auditors, List<Store> stores);
	
	List<AuditPlan> processAndSaveAuditPlans(AssignmentResponseDTO prediction);
	
	AuditPlan reassignStore(Store storeToReassign, List<Auditors> candidateAuditors);
	
	AuditPlanResponseDTO updateAuditPlanStatus(int auditPlanId, AuditPlanDTO updateDTO);
	
	List<AuditPlanResponseDTO> findAllAuditPlans();
	

}
