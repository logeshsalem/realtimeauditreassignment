package com.application.DTO;

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
public class AssignmentResponseDTO {

	 private String status;
	    private String code;
	    private ResponseData data; // The 'data' object from the Python response

	    // Getters and Setters for status, code, and data
}

