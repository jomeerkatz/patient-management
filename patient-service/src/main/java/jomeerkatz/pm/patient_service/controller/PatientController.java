package jomeerkatz.pm.patient_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.groups.Default;
import jomeerkatz.pm.patient_service.dto.PatientRequestDTO;
import jomeerkatz.pm.patient_service.dto.PatientResponseDTO;
import jomeerkatz.pm.patient_service.dto.validators.CreatePatientValidationGroup;
import jomeerkatz.pm.patient_service.service.PatientService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(value = "/patients")
@Tag(name = "patient", description = "API for managing Patients")
public class PatientController {
    private final PatientService patientService;

    public PatientController(final PatientService patientService){
        this.patientService = patientService;
    }

    @GetMapping
    @Operation(summary = "get patients")
    public ResponseEntity<List<PatientResponseDTO>> getPatients() {
        List<PatientResponseDTO> patients = patientService.getPatients();
        return ResponseEntity.ok(patients);
    }

    @PostMapping
    @Operation(summary = "create patient")
    public ResponseEntity<PatientResponseDTO> createPatient(@Validated({Default.class, CreatePatientValidationGroup.class})
                                                                @RequestBody PatientRequestDTO patientRequestDTO){
        PatientResponseDTO patient = patientService.createPatient(patientRequestDTO);
        return ResponseEntity.ok().body(patient);
    }

    @PutMapping("/{id}")
    @Operation(summary = "update patient")
    public ResponseEntity<PatientResponseDTO> updatePatient(@PathVariable UUID id,
                                                            @Validated({Default.class}) @RequestBody PatientRequestDTO patientRequestDTO) {
        PatientResponseDTO patientResponseDTO = patientService.updatePatient(id, patientRequestDTO);
        return ResponseEntity.ok().body(patientResponseDTO);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "delete patient")
    public ResponseEntity<Void> deletePatient(@PathVariable UUID id) {
        patientService.deletePatient(id);
        return ResponseEntity.noContent().build();
    }

}
