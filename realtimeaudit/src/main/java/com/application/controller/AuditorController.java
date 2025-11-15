package com.application.controller;

import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.application.entities.Auditors;
import com.application.service.AuditorService;

@RestController
@RequestMapping("/api")
public class AuditorController {
	
	private final AuditorService auditorService;
	
	@Autowired
	public AuditorController(AuditorService auditorService) {
		this.auditorService = auditorService;
	}
	
	@PostMapping("/auditor")
	public ResponseEntity<Auditors> saveAuditors(@RequestBody Auditors auditors){
		try {
			Auditors createdAuditors = auditorService.saveAuditors(auditors);
			return new ResponseEntity<>(createdAuditors, HttpStatus.CREATED);
		}catch (Exception e) {
			return  ResponseEntity
					.status(HttpStatus.INTERNAL_SERVER_ERROR)
		            .header("Error-Message", e.getMessage()) 
		            .build();
		}
	}
	
	@GetMapping("/auditors")
	public ResponseEntity<List<Auditors>> getAllAuditors(){
		try {
			List<Auditors> auditors = auditorService.findAllAuditors();
			return new ResponseEntity<>(auditors, HttpStatus.OK);
		}catch (Exception e) {
			return  ResponseEntity
					.status(HttpStatus.INTERNAL_SERVER_ERROR)
		            .header("Error-Message", e.getMessage()) 
		            .build();
		}
	}
	
	@GetMapping("/auditors/available")
	public ResponseEntity<List<Auditors>> getAvailableAuditors(){
		try {
			List<Auditors> availableAuditors = auditorService.findAvailableAuditors();
			return new ResponseEntity<>(availableAuditors, HttpStatus.OK);
		}catch (Exception e) {
			return  ResponseEntity
					.status(HttpStatus.INTERNAL_SERVER_ERROR)
		            .header("Error-Message", e.getMessage()) 
		            .build();		}
	}
	
	@GetMapping("/auditor/{id}")
	public ResponseEntity<Auditors> getAuditorsById(@PathVariable int id){
		try {
			Optional<Auditors> auditor = auditorService.findByAuditorsId(id);
			if(auditor.isPresent()) {
				return new ResponseEntity<>(auditor.get(), HttpStatus.OK);
			}else {
				return new ResponseEntity<>(HttpStatus.NOT_FOUND);
			}
		}catch (Exception e) {
			return  ResponseEntity
					.status(HttpStatus.INTERNAL_SERVER_ERROR)
		            .header("Error-Message", e.getMessage()) 
		            .build();
		}
	}
	
	

	@PutMapping("/auditor/{id}")
	public ResponseEntity<Auditors> updateAvailabilityStatus(
	        @PathVariable int id,
	        @RequestParam Auditors.AvailabilityStatus status) {
	
	    try {
	    		Auditors updated = auditorService.updateAvailabilityStatus(id, status);
		    return new ResponseEntity<>(updated, HttpStatus.OK);
	    	
	    }catch (Exception e) {
	    	return  ResponseEntity
					.status(HttpStatus.INTERNAL_SERVER_ERROR)
		            .header("Error-Message", e.getMessage()) 
		            .build();
		}
	}
	
	


	
	
	
	

}
