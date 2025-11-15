package com.application.entities;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name="auditors")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
//@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class Auditors {
	
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name="auditor_id")
	private int id;
	
	@Column(name="name")
	private String name;
	
	@Column(name="home_lat", nullable = false)
	private double homeLat;
	
	@Column(name="home_lon", nullable = false)
	private double homeLon;
	
	@Column(name="workload_capacity_hours", nullable = false)
	private double workLoadCapacityHours;
	
	@Column(name="current_assigned_hours", nullable = false)
	private double currentAssignedHours;
		
	
	@Enumerated(EnumType.STRING)
	@Column(name="availability_status")
	private AvailabilityStatus availabilityStatus;
	
	public enum AvailabilityStatus{
		AVAILABLE, 
		UNAVAILABLE,
		ON_LEAVE
	}

}
