package jomeerkatz.pm.patient_service.controller;

import jakarta.validation.Valid;
import jakarta.validation.groups.Default;
import jomeerkatz.pm.patient_service.dto.PatientRequestDTO;
import jomeerkatz.pm.patient_service.dto.PatientResponseDTO;
import jomeerkatz.pm.patient_service.mapper.PatientMapper;
import jomeerkatz.pm.patient_service.service.PatientService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(value = "/patients")
public class PatientController {
    private final PatientService patientService;

    public PatientController(final PatientService patientService){
        this.patientService = patientService;
    }

    @GetMapping
    public ResponseEntity<List<PatientResponseDTO>> getPatients() {
        List<PatientResponseDTO> patients = patientService.getPatients();
        return ResponseEntity.ok(patients);
    }

    @PostMapping
    public ResponseEntity<PatientResponseDTO> createPatient(@Valid @RequestBody PatientRequestDTO patientRequestDTO){
        PatientResponseDTO patient = patientService.createPatient(patientRequestDTO);
        return ResponseEntity.ok().body(patient);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PatientResponseDTO> updatePatient(@PathVariable UUID id,
                                                            @Valid @RequestBody PatientRequestDTO patientRequestDTO) {
        PatientResponseDTO patientResponseDTO = patientService.updatePatient(id, patientRequestDTO);
        return ResponseEntity.ok().body(patientResponseDTO);
    }

}
