package com.application.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import com.application.entities.Store;
import com.application.entities.Store.StoreStatus;

@Repository
public interface StoreRepository extends JpaRepository<Store, Integer> {
	List<Store> findByStoreStatus(Store.StoreStatus storeStatus);
	Optional<Store> findByName(String storeName); 
	
	@Query("SELECT s FROM Store s WHERE s.storeStatus = 'OPEN' AND s.id NOT IN (SELECT ap.store.id FROM AuditPlan ap)")
    List<Store> findOpenAndUnassignedStores();
	
	//List<Store> findByStoreStatus1(Store.StoreStatus status);
}
