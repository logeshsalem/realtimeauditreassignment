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
@Table(name="store")
@Getter 
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Store {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name="store_id")
	private int id;
	
	@Column(name="name")
	private String name;
	
	@Column(name="address")
	private String address;
	
	@Column(name="location_lat", nullable = false)
	private double locationLat;
	
	@Column(name="location_lon", nullable = false)
	private double locationLon;
	
	@Enumerated(EnumType.STRING)
	@Column(name="store_status", nullable = false)
	private StoreStatus storeStatus = StoreStatus.OPEN;
	
	
	public enum StoreStatus{
		OPEN,
		CLOSED
	}

}
