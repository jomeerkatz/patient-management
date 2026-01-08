package jomeerkatz.pm.patient_service.service;

import jomeerkatz.pm.patient_service.dto.PatientRequestDTO;
import jomeerkatz.pm.patient_service.dto.PatientResponseDTO;
import jomeerkatz.pm.patient_service.exception.EmailAlreadyExistsException;
import jomeerkatz.pm.patient_service.exception.PatientNotFoundException;
import jomeerkatz.pm.patient_service.grpc.BillingServiceGrpcClient;
import jomeerkatz.pm.patient_service.mapper.PatientMapper;
import jomeerkatz.pm.patient_service.model.Patient;
import jomeerkatz.pm.patient_service.repository.PatientRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class PatientService {
    private final PatientRepository patientRepository;
    private final BillingServiceGrpcClient billingServiceGrpcClient;
    public PatientService (final PatientRepository patientRepository,
            BillingServiceGrpcClient billingServiceGrpcClient
                           ) {
        this.patientRepository = patientRepository;
        this.billingServiceGrpcClient = billingServiceGrpcClient;
    }

    public List<PatientResponseDTO> getPatients() {
        List<Patient> listResultPatients = patientRepository.findAll();
        List<PatientResponseDTO> patientResponseDTOs = listResultPatients.stream().map(PatientMapper::toDTO).toList();
        return patientResponseDTOs;
    }

    public PatientResponseDTO createPatient(PatientRequestDTO patientRequestDTO) {
        if(patientRepository.existsByEmail(patientRequestDTO.getEmail())) {
            throw new EmailAlreadyExistsException("Patient with this email already exists: " + patientRequestDTO.getEmail());
        }
        Patient model = PatientMapper.toModel(patientRequestDTO);
        Patient save = patientRepository.save(model);

        billingServiceGrpcClient.createBillingAccount(save.getId().toString(), save.getName(), save.getEmail());

        return PatientMapper.toDTO(save);
    }

    public PatientResponseDTO updatePatient(UUID id, PatientRequestDTO patientRequestDTO) {
        Patient patient = patientRepository.findById(id).orElseThrow(() -> new PatientNotFoundException("patient not found with id: "+ id));
        // check if there is another patient but with the same email
        if(patientRepository.existsByEmailAndIdNot(patientRequestDTO.getEmail(), id)) {
            throw new EmailAlreadyExistsException("Patient with this email already exists: " + patientRequestDTO.getEmail());
        }

        patient.setName(patientRequestDTO.getName());
        patient.setEmail(patientRequestDTO.getEmail());
        patient.setAddress(patientRequestDTO.getAddress());
        patient.setDateOfBirth(LocalDate.parse(patientRequestDTO.getDateOfBirth()));

        Patient save = patientRepository.save(patient);
        return PatientMapper.toDTO(save);
    }

    public void deletePatient(UUID id) {
        patientRepository.deleteById(id);
    }
}
