package com.application.DTO;

import com.application.entities.AuditPlan.AuditPriority;
import com.application.entities.AuditPlan.AuditStatus;
import com.application.entities.Auditors;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// A simple, clean object to represent the API response.
@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuditPlanResponseDTO {

    
	
	private int auditId;
    private String auditStatus;
    private String auditPriority;    
   
    
    // You can include simplified details from the related entities
    private int auditorId;
    private String auditorName;
    private int storeId;
    private String storeName;

    // Getters and Setters for all fields...
    
    
   
}