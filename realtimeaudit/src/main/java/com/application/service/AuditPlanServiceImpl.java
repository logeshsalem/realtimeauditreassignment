package com.application.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import com.application.DTO.AssignmentRequestDTO;
import com.application.DTO.AssignmentResponseDTO;
import com.application.DTO.AuditPlanDTO;
import com.application.DTO.AuditPlanResponseDTO;
import com.application.DTO.AuditorDTO;
import com.application.DTO.StoreDTO;
import com.application.entities.AuditPlan;
import com.application.entities.Auditors;
import com.application.entities.Store;
import com.application.repository.AuditPlanRepository;
import com.application.repository.AuditorRepository;
import com.application.repository.StoreRepository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class AuditPlanServiceImpl implements AuditPlanService {
	 
	@Value("${python.api.url}")
	private String pythonApiUrl;
	
	@Value("${python.api.key}")
	private String pythonApiKey;
	 
	private static final Logger logger = LoggerFactory.getLogger(AuditPlanServiceImpl.class);
	 
	@Autowired
	private RestTemplate restTemplate;
	 
	@Autowired
	private AuditorRepository auditorRepository;
	 
	@Autowired
	private StoreRepository storeRepository;
	 
	@Autowired
	private AuditPlanRepository auditPlanRepository;
	 
	// This method is for debugging and can be called by other methods. It is correct.
	@Override
	public AssignmentResponseDTO getAssignment(AssignmentRequestDTO request) {
	    ObjectMapper objectMapper = new ObjectMapper();
	    try {
	        String jsonPayload = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(request);
	        System.out.println("==================== SENDING TO PYTHON ====================");
	        System.out.println(jsonPayload);
	        System.out.println("=========================================================");
	    } catch (JsonProcessingException e) {
	        logger.error("Error converting request DTO to JSON", e);
	    }
	    
	    org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
	    headers.set("Content-Type", "application/json");
	    headers.set("X-API-Key", pythonApiKey);
	    
	    HttpEntity<AssignmentRequestDTO> entity = new HttpEntity<>(request, headers);
	    String fullUrl = pythonApiUrl + "/api/process-assignments";

	    ResponseEntity<AssignmentResponseDTO> response = restTemplate.exchange(
	        fullUrl, HttpMethod.POST, entity, AssignmentResponseDTO.class
	    );

	    try {
	        String receivedJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(response.getBody());
	        System.out.println("================== RECEIVED FROM PYTHON ===================");
	        System.out.println(receivedJson);
	        System.out.println("=========================================================");
	    } catch (JsonProcessingException e) {
	        logger.error("Error converting response DTO to JSON for printing", e);
	    }
	    return response.getBody();
	}

    // This is the main method for generating new plans. It is now correct.
	@Override
    @Transactional
    public List<AuditPlanResponseDTO> generateAndSaveAuditPlan() {
        logger.info("--- Starting AI-Informed, Java-Enforced audit plan generation ---");
        
        List<Auditors> unassignedAuditors = auditorRepository.findAvailableAndUnassignedAuditors();
        List<Store> unassignedStores = storeRepository.findOpenAndUnassignedStores();
        
        if (unassignedStores.isEmpty() || unassignedAuditors.isEmpty()) {
            logger.warn("No unassigned stores or available auditors to plan. Process finished.");
            return new ArrayList<>();
        }

        AssignmentRequestDTO requestDto = mapEntitiesToRequestDTO(unassignedAuditors, unassignedStores);
        AssignmentResponseDTO predictionResponse = getAssignment(requestDto);
        
        // --- FIX 1: Call the correct processing method ---
        List<AuditPlan> savedAssignments = processAndSaveAuditPlans(predictionResponse);
        
        logger.info("Finished processing. Saved {} new, validated assignments.", savedAssignments.size());

        return savedAssignments.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // This method is for processing the AI response. It is now correct.
	@Override
	public List<AuditPlan> processAndSaveAuditPlans(AssignmentResponseDTO prediction) {
        List<AuditPlan> savedAssignments = new ArrayList<>();
        Set<Long> assignedAuditorIds = new HashSet<>();

        if (prediction == null || prediction.getData() == null || prediction.getData().getStores() == null) {
            logger.warn("Prediction data is null or empty. Skipping assignment processing.");
            return savedAssignments;
        }

        List<Map<String, Object>> storeResults = (List<Map<String, Object>>) (List<?>) prediction.getData().getStores();
        logger.info("Received {} assignment recommendations from AI. Now validating...", storeResults.size());

        for (Map<String, Object> storeResult : storeResults) {
            Object storeIdObj = storeResult.get("store_id");
            // --- FIX 2: Corrected the typo in the JSON key ---
            Object auditorIdObj = storeResult.get("assigned_auditor_id"); 
            
            logger.debug("Processing recommendation: Store ID = {}, Auditor ID = {}", storeIdObj, auditorIdObj);

            if (auditorIdObj != null && storeIdObj != null) {
                // --- FIX 3: Consistently use Long for all IDs ---
                int storeId = (int) ((Number) storeIdObj).longValue();
                int auditorId = (int) ((Number) auditorIdObj).longValue();

                if (assignedAuditorIds.contains(auditorId)) {
                    logger.warn("RULE VIOLATION: AI recommended assigning Auditor ID {} again. Ignoring.", auditorId);
                    continue;
                }
                
                Optional<Auditors> auditorOptional = auditorRepository.findById(auditorId);
                Optional<Store> storeOptional = storeRepository.findById(storeId);

                if (auditorOptional.isPresent() && storeOptional.isPresent()) {
                    Auditors auditorEntity = auditorOptional.get();
                    Store storeEntity = storeOptional.get();
                    
                    AuditPlan plan = new AuditPlan();
                    plan.setAuditors(auditorEntity);
                    plan.setStore(storeEntity);
                    plan.setAuditStatus(AuditPlan.AuditStatus.PLANNED);
                    plan.setAuditPriority(AuditPlan.AuditPriority.MEDIUM);

                    savedAssignments.add(auditPlanRepository.save(plan));
                    assignedAuditorIds.add((long) auditorId);
                    logger.info("VALID ASSIGNMENT: Saved plan for Store ID {} with Auditor ID {}.", storeId, auditorId);
                    
                } else {
                    logger.warn("Could not create assignment. Auditor/Store not found in DB for IDs: Auditor={}, Store={}", auditorId, storeId);
                }
            }
        }
        return savedAssignments;
    }

	// This mapping method is correct.
	@Override
	public AssignmentRequestDTO mapEntitiesToRequestDTO(List<Auditors> auditors, List<Store> stores) {
		List<AuditorDTO> auditorDTOs = auditors.stream().map(auditor -> {
			AuditorDTO dto = new AuditorDTO();
			dto.setAuditorId(auditor.getId());
			dto.setLatitude(auditor.getHomeLat());
			dto.setLongitude(auditor.getHomeLon());
			
			if (auditor.getAvailabilityStatus() == Auditors.AvailabilityStatus.AVAILABLE) {
	            dto.setAvailabilityStatus("Available");
	        } else {
	            dto.setAvailabilityStatus("Unavailable");
	        }
			return dto;
		}).collect(Collectors.toList());
		
		 List<StoreDTO> storeDtos = stores.stream().map(store -> {
	            StoreDTO dto = new StoreDTO();
	            dto.setStoreId(store.getId()); 
	            dto.setLatitude(store.getLocationLat());
	            dto.setLongitude(store.getLocationLon());
	            
	            if (store.getStoreStatus() == Store.StoreStatus.OPEN) {
	                dto.setStoreStatus("Open");
	            } else {
	                dto.setStoreStatus("Closed");
	            }
	            return dto;
	        }).collect(Collectors.toList());
		 
		 AssignmentRequestDTO requestDTO = new AssignmentRequestDTO();
		 requestDTO.setAuditors(auditorDTOs);
		 requestDTO.setStores(storeDtos);
		 return requestDTO;
	}
    
    // This helper method is correct.
	public AuditPlanResponseDTO convertToDTO(AuditPlan plan) {
	    AuditPlanResponseDTO dto = new AuditPlanResponseDTO();
	    dto.setAuditId(plan.getId()); 
	    dto.setAuditStatus(plan.getAuditStatus().name());
	    dto.setAuditPriority(plan.getAuditPriority().name());
	    
	    if (plan.getAuditors() != null) {
	        dto.setAuditorId(plan.getAuditors().getId());
	        dto.setAuditorName(plan.getAuditors().getName());
	    }
	    if (plan.getStore() != null) {
	        dto.setStoreId(plan.getStore().getId());
	        dto.setStoreName(plan.getStore().getName());
	    }
	    return dto;
	}
    
    // This reassignment logic is now correct.
	@Override
	public AuditPlan reassignStore(Store storeToReassign, List<Auditors> candidateAuditors) {
		logger.info("Attempting to reassign store ID: {}. Found {} candidate auditors.", storeToReassign.getId(), candidateAuditors.size());
        if (candidateAuditors.isEmpty()) {
            logger.warn("No available auditors to reassign store ID: {}. Un-assigning.", storeToReassign.getId());
            auditPlanRepository.findByStore(storeToReassign).ifPresent(auditPlanRepository::delete);
            return null;
        }

        AssignmentRequestDTO requestDTO = mapSingleStoreToRequestDTO(storeToReassign, candidateAuditors);
        AssignmentResponseDTO prediction = getAssignment(requestDTO);

        if (prediction != null && prediction.getData() != null && prediction.getData().getStores() != null) {
            List<Map<String, Object>> storeResults = (List<Map<String, Object>>) (List<?>) prediction.getData().getStores();
            
            if (!storeResults.isEmpty()) {
                Map<String, Object> result = storeResults.get(0);
                Object newAuditorIdObj = result.get("assigned_auditor_id");

                if (newAuditorIdObj != null) {
                    // --- FIX 3: Consistently use Long for all IDs ---
                    int newAuditorId = (int) ((Number) newAuditorIdObj).longValue();
                    Optional<Auditors> newAuditorOptional = auditorRepository.findById(newAuditorId);
                    
                    if (newAuditorOptional.isPresent()) {
                        Optional<AuditPlan> existingPlanOptional = auditPlanRepository.findByStore(storeToReassign);
                        if (existingPlanOptional.isPresent()) {
                            AuditPlan planToUpdate = existingPlanOptional.get();
                            planToUpdate.setAuditors(newAuditorOptional.get());
                            logger.info("Successfully reassigned store ID: {} to new auditor ID: {}", storeToReassign.getId(), newAuditorId);
                            return auditPlanRepository.save(planToUpdate);
                        }
                    }
                }
            }
        }
        
        logger.warn("AI did not assign a new auditor for store ID: {}. Un-assigning.", storeToReassign.getId());
        auditPlanRepository.findByStore(storeToReassign).ifPresent(auditPlanRepository::delete);
        return null;
	}
	
    // This helper method is now correct.
	private AssignmentRequestDTO mapSingleStoreToRequestDTO(Store store, List<Auditors> auditors) {
        List<AuditorDTO> auditorDTOs = auditors.stream().map(auditor -> {
           AuditorDTO dto = new AuditorDTO();
           dto.setAuditorId(auditor.getId());
           dto.setLatitude(auditor.getHomeLat());
           dto.setLongitude(auditor.getHomeLon());
           dto.setAvailabilityStatus("Available");
			return dto;
		}).collect(Collectors.toList());
		
		StoreDTO storeDto = new StoreDTO();
		storeDto.setStoreId(store.getId());
		storeDto.setLatitude(store.getLocationLat());
		storeDto.setLongitude(store.getLocationLon());
		storeDto.setStoreStatus("Open");

		AssignmentRequestDTO requestDTO = new AssignmentRequestDTO();
		requestDTO.setAuditors(auditorDTOs);
		requestDTO.setStores(List.of(storeDto));
		return requestDTO;
   }
    
	
	// This method is correct.
	@Override
	public List<AuditPlanResponseDTO> findAllAuditPlans() {
        logger.info("Fetching all audit plans from the database.");
        List<AuditPlan> plansFromDb = auditPlanRepository.findAll();
        return plansFromDb.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
	}
    
    // The haversineDistance method is no longer used by the main flow and can be removed
    // if you are only using the AI-based approach. I've left it here in case you need it.
    private double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Earth radius in kilometers
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

	
	
	

	@Override
	public AuditPlanResponseDTO updateAuditPlanStatus(int auditPlanId, AuditPlanDTO updateDTO) {
	    AuditPlan existingPlan = auditPlanRepository.findById(auditPlanId)
	            .orElseThrow(() -> new RuntimeException("AuditPlan not found with ID: " + auditPlanId));

	    if (updateDTO.getAuditStatus() != null) {
	        existingPlan.setAuditStatus(updateDTO.getAuditStatus());
	    }
	    if (updateDTO.getAuditPriority() != null) {
	        existingPlan.setAuditPriority(updateDTO.getAuditPriority());
	    }

	    // Save the entity as before
	    AuditPlan savedPlan = auditPlanRepository.save(existingPlan);

	    // --- NEW: Convert the saved entity to a DTO before returning ---
	    // This happens WHILE THE TRANSACTION IS STILL OPEN, so lazy loading works.
	    return convertToDTO(savedPlan);
	}

	// Add this private helper method to your service class
	

	

	

	
}