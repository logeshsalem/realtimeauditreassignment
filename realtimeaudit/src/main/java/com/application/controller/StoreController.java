package com.application.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.application.entities.Store;
import com.application.service.StoreService;

@RestController
@RequestMapping("/api")
public class StoreController {
	
	private final StoreService storeService;
	
	@Autowired
	public StoreController(StoreService storeService) {
		this.storeService = storeService;
	}
	
	@PostMapping("/store")
	public ResponseEntity<Store> saveStore(@RequestBody Store store) {
		try {			
			Store createStore = storeService.saveStore(store);
			return new ResponseEntity<>(createStore, HttpStatus.CREATED);
			
		}catch (Exception e) {
		    return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .header("Error-Message", e.getMessage()) 
            .build();
		}

	}
	
	@GetMapping("/store")
	public ResponseEntity<List<Store>> getAllStores(){
		try {
			List<Store> stores = storeService.findAllStores();
			return new ResponseEntity<>(stores, HttpStatus.OK);
			
		}catch (Exception e) {
			return ResponseEntity
					.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.header("error message", e.getMessage())
					.build();
		}
	}
	
	
	@GetMapping("/store/open")
	public ResponseEntity<List<Store>> findByAvailableStores(){
		try {
			List<Store> availableStores = storeService.findByStoreStatus();
			return new ResponseEntity<>(availableStores, HttpStatus.OK);
		}catch (Exception e) {
			return ResponseEntity
					.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.header("error message", e.getMessage())
					.build();
		}		
	}
	
	@PutMapping("/store/{id}")
	public ResponseEntity<Store> updateStoreStatus(@PathVariable int id, @RequestParam Store.StoreStatus status){
		try {
			
			Store updateStore = storeService.updateStoreStatus(id, status);
			return new ResponseEntity<>(updateStore, HttpStatus.OK);
		}catch (Exception e) {
			return ResponseEntity
					.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.header("error message", e.getMessage())
					.build();
		}
	}
	

}
