package com.application.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.application.entities.AuditPlan;
import com.application.entities.Auditors;
import com.application.entities.Store;

@Repository
public interface AuditPlanRepository extends JpaRepository<AuditPlan, Integer>{
	
	List<AuditPlan> findByAuditors(Auditors auditor);
    
    Optional<AuditPlan> findByStore(Store store);
    
    

}
