package jomeerkatz.pm.patient_service.kafka;

import jomeerkatz.pm.patient_service.model.Patient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import patient.events.PatientEvent;

@Service
public class KafkaProducer {
    private static final Logger log = LoggerFactory.getLogger(KafkaProducer.class);
    private final KafkaTemplate<String, byte[]> kafkaTemplate;

    public KafkaProducer(KafkaTemplate<String, byte[]> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendEvent(Patient patient) {
        PatientEvent patientEvent = PatientEvent.newBuilder()
                .setPatientId(patient.getId().toString())
                .setName(patient.getName())
                .setEmail(patient.getEmail())
                .setEventType("PATIENT_CREATED")
                .build();

        try {
            // WICHTIG: Das Ergebnis abwarten oder Callback nutzen
            kafkaTemplate.send("patient", patientEvent.toByteArray())
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.info("✅ SUCCESS! Offset: {}", result.getRecordMetadata().offset());
                        } else {
                            log.error("❌ FAILED to send: {}", ex.getMessage(), ex);
                        }
                    });
        } catch (Exception e) {
            log.error("❌ error sending PatientCreated event: {}", patientEvent);
        }
    }

}
