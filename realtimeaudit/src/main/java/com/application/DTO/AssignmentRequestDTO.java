package com.application.DTO;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssignmentRequestDTO {
	
	 // Initialize the lists to prevent them from ever being null
    private List<AuditorDTO> auditors = new ArrayList<>();
    private List<StoreDTO> stores = new ArrayList<>();

    // Getters and Setters remain the same
    public List<AuditorDTO> getAuditors() {
        return auditors;
    }

    public void setAuditors(List<AuditorDTO> auditors) {
        this.auditors = auditors;
    }

    public List<StoreDTO> getStores() {
        return stores;
    }

    public void setStores(List<StoreDTO> stores) {
        this.stores = stores;
    }

}
