package jomeerkatz.pm.patient_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class PatientRequestDTO {
    @NotBlank
    @Size(max = 100, message = "name can't exceed 100 characters")
    private String name;
}
