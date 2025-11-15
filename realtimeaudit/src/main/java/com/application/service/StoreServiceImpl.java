package com.application.service;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.entities.Auditors;
import com.application.entities.Store;
import com.application.entities.Store.StoreStatus;
import com.application.repository.AuditPlanRepository;
import com.application.repository.StoreRepository;

@Service
public class StoreServiceImpl implements StoreService {
	
	private final StoreRepository storeRepository;
	private final AuditPlanRepository auditPlanRepository;
	
	private static final Logger logger = LoggerFactory.getLogger(AuditPlanServiceImpl.class);
	
	@Autowired
	public StoreServiceImpl(StoreRepository storeRepository, AuditPlanRepository auditPlanRepository) {
		this.storeRepository = storeRepository;
		this.auditPlanRepository = auditPlanRepository;
	}
	
	@Override
	public Store saveStore(Store store) {
		return storeRepository.save(store);
	}

	@Override
	public List<Store> findAllStores() {
		return storeRepository.findAll();
	}

	@Override
	public List<Store> findByStoreStatus() {
		return storeRepository.findByStoreStatus(StoreStatus.OPEN);
	}

	@Override
    @Transactional // Ensures all database operations succeed or fail together
    public Store updateStoreStatus(int storeId, Store.StoreStatus newStatus) {
        // 1. Find the store to update.
        Store storeToUpdate = storeRepository.findById(storeId)
                .orElseThrow(() -> new RuntimeException("Store not found with ID: " + storeId));

        // 2. Set the new status and save the store.
        storeToUpdate.setStoreStatus(newStatus);
        Store updatedStore = storeRepository.save(storeToUpdate);
        logger.info("Successfully updated status for Store ID {} to {}", storeId, newStatus);

        // --- CORE LOGIC: Un-assign if the store is now CLOSED ---
        if (newStatus == Store.StoreStatus.CLOSED) {
            logger.info("Store ID {} was closed. Checking for an existing audit plan to remove...", storeId);
            
            // 3. Find the associated audit plan.
            auditPlanRepository.findByStore(updatedStore).ifPresent(auditPlan -> {
                // ifPresent executes this block only if an audit plan was found.
                
                Auditors assignedAuditor = auditPlan.getAuditors();

                logger.info("Found AuditPlan ID {}. Un-assigning from Auditor ID {}.", auditPlan.getId(), assignedAuditor.getId());

                

                // 5. Delete the audit plan entry.
                auditPlanRepository.delete(auditPlan);
                logger.info("Successfully deleted AuditPlan ID {}.", auditPlan.getId());
            });
        }

        return updatedStore;
    }

	@Override
	public Optional<Store> findStoreById(int targetStoreId) {
		return storeRepository.findById(targetStoreId);
	}
	
	

}
