package com.application.DTO; // Or your DTO package

import com.application.entities.AuditPlan; // Import your entity with the enums

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuditPlanDTO {

    private AuditPlan.AuditStatus auditStatus;
    private AuditPlan.AuditPriority auditPriority;

    public AuditPlan.AuditStatus getAuditStatus() {
        return auditStatus;
    }

    public void setAuditStatus(AuditPlan.AuditStatus auditStatus) {
        this.auditStatus = auditStatus;
    }

    public AuditPlan.AuditPriority getAuditPriority() {
        return auditPriority;
    }

    public void setAuditPriority(AuditPlan.AuditPriority auditPriority) {
        this.auditPriority = auditPriority;
    }
}