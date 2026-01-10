package jomeerkatz.pm.analytics_service.kafka;

import com.google.protobuf.InvalidProtocolBufferException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import patient.events.PatientEvent;

@Service
public class KafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(KafkaConsumer.class);

    // connect kafka consumer to the kafka topic via this annotation
    // topics: where we want to consume data from
    // groupId: tells the kafka broker, who the consumer is
    // kafka opens/calls this method when something new arrives in the topic (message)
    @KafkaListener(topics = "patient", groupId = "analytics-service")
    public void consumeEvent(byte[] event) {
        try {
            PatientEvent patientEvent = PatientEvent.parseFrom(event);
            // here can follow any business logic which can use this data for more action
            log.info("✅ received patient event: [patientId={}, patientName={}, " +
                    "patientEmail={}]", patientEvent.getPatientId(),
                    patientEvent.getName(), patientEvent.getEmail());

        } catch (InvalidProtocolBufferException ex) {
            log.error("❌ error deserializing event {}", ex.getMessage());
        }
    }
}
