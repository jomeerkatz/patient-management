package jomeerkatz.pm.patient_service.grpc;

import billing.BillingRequest;
import billing.BillingResponse;
import billing.BillingServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class BillingServiceGrpcClient {
    private static final Logger log = LoggerFactory.getLogger(BillingServiceGrpcClient.class);
    private final BillingServiceGrpc.BillingServiceBlockingStub blockingStub;

    public BillingServiceGrpcClient(
            @Value("${billing.service.address:localhost}") String serverAddress,
            @Value("${billing.service.grpc.port:9001}") int serverPort
    ) {
        log.info("🤝🏽patient-service (0/2): connecting to billing service grpc service at {}:{}", serverAddress, serverPort);
        ManagedChannel channel = ManagedChannelBuilder // create the "way" to the service
                .forAddress(serverAddress, serverPort)
                .usePlaintext()
                .build();

        blockingStub = BillingServiceGrpc.newBlockingStub(channel); // the service "structure" which we use for calling
        // the methods on service site
    }

    public BillingResponse createBillingAccount(String patientId, String name,
                                                String email) {
        BillingRequest billingRequest = BillingRequest.newBuilder()
                .setPatientId(patientId)
                .setName(name)
                .setEmail(email)
                .build();

        log.info("📧📥 patient-service (1/2): sending request!...");


        BillingResponse response = blockingStub.createBillingAccount(billingRequest);

        log.info("✅patient-service (2/2): received response from billing service via grpc: {}", response);

        return response;
    }

}
