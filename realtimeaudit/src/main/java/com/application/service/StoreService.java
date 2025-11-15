package com.application.service;

import java.util.List;
import java.util.Optional;

import com.application.entities.Auditors;
import com.application.entities.Store;
import com.application.entities.Store.StoreStatus;

public interface StoreService {

	Store saveStore(Store store);	
	List<Store> findAllStores();
	List<Store> findByStoreStatus();
	Store updateStoreStatus(int id, StoreStatus newStatus);
	Optional<Store> findStoreById(int targetStoreId);
	
}
