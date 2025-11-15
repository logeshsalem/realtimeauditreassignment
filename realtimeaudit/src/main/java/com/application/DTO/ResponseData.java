package com.application.DTO;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ResponseData {
	    // This maps to the inner JSON object with auditors, stores, disruptions
	    private List<Object> auditors; // You can create more specific DTOs if needed
	    private List<Object> stores;
	    private List<Object> disruptions;

}
