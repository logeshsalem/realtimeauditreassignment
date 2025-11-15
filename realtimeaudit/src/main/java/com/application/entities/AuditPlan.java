package com.application.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name="audit_plan")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuditPlan {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "audit_id")
	private int id;

	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name = "auditor_id", nullable = false)
	private Auditors auditors;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "store_id", nullable = false)
	private Store store;
	
	@Enumerated(EnumType.STRING)
	@Column(name="audit_priority", nullable = false)
	private AuditPriority auditPriority;
	
	@Enumerated(EnumType.STRING)
	@Column(name="audit_status", nullable = false)
	private AuditStatus auditStatus = AuditStatus.PLANNED;
	
	
	public enum AuditPriority{
		HIGH,
		MEDIUM,
		LOW
	}
	
	public enum AuditStatus{
		PLANNED,
		IN_PROGRESS,
		DISRUPTED,
		REASSIGNED,
		COMPLETED
	}
	
}
