package jomeerkatz.pm.patient_service.service;

import jomeerkatz.pm.patient_service.dto.PatientResponseDTO;
import jomeerkatz.pm.patient_service.mapper.PatientMapper;
import jomeerkatz.pm.patient_service.model.Patient;
import jomeerkatz.pm.patient_service.repository.PatientRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PatientService {
    private final PatientRepository patientRepository;

    public PatientService (final PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    public List<PatientResponseDTO> getPatients() {
        List<Patient> listResultPatients = patientRepository.findAll();

        List<PatientResponseDTO> patientResponseDTOs = listResultPatients.stream().map(PatientMapper::toDTO).toList();

        return patientResponseDTOs;
    }
}
