package jomeerkatz.pm.stack;

import org.jetbrains.annotations.NotNull;
import software.amazon.awscdk.*;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.ec2.InstanceType;
import software.amazon.awscdk.services.ecs.*;
import software.amazon.awscdk.services.ecs.Protocol;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedFargateService;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.amazon.awscdk.services.msk.CfnCluster;
import software.amazon.awscdk.services.rds.*;
import software.amazon.awscdk.services.route53.CfnHealthCheck;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// cdk: cloud development kid -> for creating cloud infra but with java code and NOT
// with CloudFormation code/syntax
// so what we can do is describing how the aws infra should look like
// IaC, but higher abstraction lvl
// we just define, how the infrastructure should look like
// will get converted to CloudFormation format, which is IaC in aws "language"
public class LocalStack extends Stack {
    private final Vpc vpc;
    private final Cluster ecsCluster;

    // constructor for creating a stack
    public LocalStack(final App scope, final String id, final StackProps props) {
        super(scope, id, props);
        this.vpc = createVpc();
        DatabaseInstance authServiceDB = createDatabase("AuthServiceDB", "auth-service-db");
        DatabaseInstance patientServiceDB = createDatabase("PatientServiceDB",
                "patient-service" + "-db");
        CfnHealthCheck authDbHealthCheck = createDbHealthCheck(authServiceDB,
                "AuthServiceDBHealthCheck");
        CfnHealthCheck patientDbHealthCheck = createDbHealthCheck(authServiceDB,
                "PatientServiceDBHealthCheck");
        CfnCluster mskCluster = createMskCluster();
        this.ecsCluster = createEcsCluster();
        FargateService authService = createFargateService("AuthService", "auth-service",
                List.of(4005), authServiceDB, Map.of("JWT_SECRET",
                        "ySAjh6mh2M6dRnEUXeZKMiCqHrJn65tAhKdj16pOXjtBJb1vNQ2nPSj8qRZdNmv31hHldnuVlzsHjdL/ODhunw=="));

        authService.getNode().addDependency(authDbHealthCheck);
        authService.getNode().addDependency(authServiceDB);

        FargateService billingService = createFargateService("BillingService", "billing-service",
                List.of(4001, 9001), null, null);

        FargateService analyticsService =
                createFargateService("AnalyticsService", "analytics-service",
                        List.of(4002),
                        null,
                        null);

        analyticsService.getNode().addDependency(mskCluster);

        FargateService patientService =
                createFargateService("PatientService",
                        "patient-service",
                        List.of(4000),
                        patientServiceDB,
                        Map.of("BILLING_SERVICE_ADDRESS", "host.docker.internal",
                                "BILLING_SERVICE_GRPC_PORT", "9001")
                        );

        patientService.getNode().addDependency(patientServiceDB);
        patientService.getNode().addDependency(patientDbHealthCheck);
        patientService.getNode().addDependency(billingService);
        patientService.getNode().addDependency(mskCluster);

        createApiGatewayService();
    }

    private Vpc createVpc() {
        return Vpc.Builder.create(this, "PatientManagementVPC").vpcName("PatientManagementVPC").maxAzs(2).build(); // max available zones
    }

    private DatabaseInstance createDatabase(String id, String dbName) {
        // construct, which will create a database
        return DatabaseInstance.Builder.create(this, id).engine(DatabaseInstanceEngine.postgres(PostgresInstanceEngineProps.builder().version(PostgresEngineVersion.VER_17_2).build())).vpc(this.vpc).instanceType(InstanceType.of(InstanceClass.BURSTABLE2, InstanceSize.MICRO)) // doesn't matter that much bec of local dev
                .allocatedStorage(20).credentials(Credentials.fromGeneratedSecret("admin_user")).databaseName(dbName).removalPolicy(RemovalPolicy.DESTROY).build();
    }

    private CfnHealthCheck createDbHealthCheck(DatabaseInstance db, String id) {
        return CfnHealthCheck.Builder.create(this, id).healthCheckConfig(CfnHealthCheck.HealthCheckConfigProperty.builder().type("TCP").port(Token.asNumber(db.getDbInstanceEndpointPort())).ipAddress(db.getDbInstanceEndpointAddress()).requestInterval(30) // 30 seconds
                .failureThreshold(3) // try it 3 times
                .build()).build();
    }

    // Creates an Amazon MSK (Managed Streaming for Apache Kafka) cluster
    // Kafka but from AWS, which will handle scaling etc.
    private CfnCluster createMskCluster() {
        // Collect all private subnet IDs from the VPC
        // MSK brokers must run inside private subnets
        List<@NotNull String> collectedSubnets = vpc.getPrivateSubnets()              // get all
                // private subnets of the VPC
                .stream()                         // convert list to stream
                .map(ISubnet::getSubnetId)        // extract subnet ID from each subnet
                .toList();                        // collect results into a list

        // Define a low-level CloudFormation MSK Cluster resource
        return CfnCluster.Builder.create(this, "MskCluster") // bind MSK cluster to this stack
                .clusterName("kafka-cluster")               // logical name of the Kafka cluster
                .kafkaVersion("2.8.0")                      // Kafka version to use
                .numberOfBrokerNodes(1)                     // number of Kafka broker nodes
                .brokerNodeGroupInfo(
                        // Configuration for Kafka broker nodes
                        CfnCluster.BrokerNodeGroupInfoProperty.builder().instanceType("kafka.m5" + ".xlarge") // EC2 instance type for each broker
                                .clientSubnets(collectedSubnets) // subnets where brokers will be
                                // placed
                                .brokerAzDistribution("DEFAULT") // distribute brokers across AZs
                                .build())                        // build broker node group config
                .build();                                       // build the MSK cluster resource
    }

    // Creates an ECS Cluster
    // An ECS Cluster is the logical place where ECS Services and Tasks can run
    private Cluster createEcsCluster() {
        return Cluster.Builder.create(this, "PatientManagementCluster")
                // Attach the cluster to our VPC (private network)
                .vpc(this.vpc)

                // Enable Service Discovery for all ECS services in this cluster
                // Services can find each other by name instead of IP addresses
                // Example DNS names:
                // auth.patient-management.local
                // billing.patient-management.local
                .defaultCloudMapNamespace(CloudMapNamespaceOptions.builder()
                        // Internal DNS namespace (private, not public)
                        .name("patient-management.local").build())

                // Build and create the ECS Cluster resource
                .build();
    }

    private FargateService createFargateService(String id, String imageName, List<Integer> ports,
                                                DatabaseInstance db,
                                                Map<String, String> additionalEnvVars) {

        // define resources for each running container
        FargateTaskDefinition taskDefinition = FargateTaskDefinition.Builder.create(this, id +
                "Task").cpu(256).memoryLimitMiB(512).build();

        ContainerDefinitionOptions.Builder containerOptions = ContainerDefinitionOptions.builder()
                .image(ContainerImage.fromRegistry(imageName))
                .portMappings(ports.stream() // we have to define it, because it should get accessed from outside. here we say, where the app is listening
                .map(port -> PortMapping.builder()
                        .containerPort(port)
                        .hostPort(port)
                        .protocol(Protocol.TCP).build()).toList())
                .logging(LogDriver.awsLogs(AwsLogDriverProps.builder()
                        .logGroup(LogGroup.Builder.create(this, id + "LogGroup")
                                .logGroupName("/ecs/" + imageName)
                                .removalPolicy(RemovalPolicy.DESTROY)
                                .retention(RetentionDays.ONE_DAY).build())
                                .streamPrefix(imageName)
                        .build()));

        Map<String, String> envVars = new HashMap<>();
        envVars.put("SPRING_KAFKA_BOOTSTRAP_SERVERS", "localhost.localstack.cloud:4510, " +
                "localhost" + ".localstack.cloud:4511, localhost.localstack.cloud:4512");

        if (additionalEnvVars != null) {
            envVars.putAll(additionalEnvVars);
        }

        if (db != null) {
            envVars.put("SPRING_DATASOURCE_URL",
                    "jdbc:postgresql://%s:%s/%s-db".formatted(db.getDbInstanceEndpointAddress(),
                            db.getDbInstanceEndpointPort(), imageName));

            envVars.put("SPRING_DATASOURCE_USERNAME", "admin_user");
            envVars.put("SPRING_DATASOURCE_PASSWORD", db.getSecret().secretValueFromJson(
                    "password").toString());
            envVars.put("SPRING_JPA_HIBERNATE_DDL_AUTO", "update");
            envVars.put("SPRING_SQL_INIT_MODE", "always");
            envVars.put("SPRING_DATASOURCE_HIKARI_INITIALIZATION_FAIL_TIMEOUT", "600000");
        }

        containerOptions.environment(envVars);
        taskDefinition.addContainer(imageName + "Container", containerOptions.build());

        return FargateService.Builder.create(this, id).cluster(ecsCluster).taskDefinition(taskDefinition).assignPublicIp(false).serviceName(imageName).build();
    }

    private void createApiGatewayService() {
        // define resources for each running container
        FargateTaskDefinition taskDefinition = FargateTaskDefinition.Builder.create(this, "APIGatewayTaskDefinition").cpu(256).memoryLimitMiB(512).build();

        ContainerDefinitionOptions containerOptions = ContainerDefinitionOptions.builder()
                .image(ContainerImage.fromRegistry("api-gateway"))
                .environment(Map.of(
                        "SPRING_PROFILES_ACTIVE", "prod",
                        "AUTH_SERVICE_URL", "http://host.docker.internal:4005"
                ))
                .portMappings(List.of(4004).stream() // we have to define it, because it should get accessed from outside. here we say, where the app is listening
                        .map(port -> PortMapping.builder()
                                .containerPort(port)
                                .hostPort(port)
                                .protocol(Protocol.TCP).build()).toList())
                .logging(LogDriver.awsLogs(AwsLogDriverProps.builder()
                        .logGroup(LogGroup.Builder.create(this, "APIGatewayLogGroup")
                                .logGroupName("/ecs/api-gateway")
                                .removalPolicy(RemovalPolicy.DESTROY)
                                .retention(RetentionDays.ONE_DAY).build())
                        .streamPrefix("api-gateway")
                        .build()))
                .build();

        taskDefinition.addContainer("APIGatewayContainer", containerOptions);

        ApplicationLoadBalancedFargateService.Builder.create(this, "APIGatewayService")
                .cluster(ecsCluster)
                .serviceName("api-gateway")
                .taskDefinition(taskDefinition)
                .desiredCount(1)
                .healthCheckGracePeriod(Duration.seconds(60))
                .build();

    }

    public static void main(final String[] args) {
        App app = new App(AppProps.builder() // create new CDK project
                .outdir("./cdk.out") // define, where we want to store the CloudFormation file
                .build()); // no resources created yet, just the "wrapper"

        StackProps props = StackProps.builder() // create config for stack
                .synthesizer(new BootstraplessSynthesizer()) // we define, project should not
                // need bootstrap resources
                .build();

        new LocalStack(app, "localstack", props); // instantiate - just empty "wrapper"
        app.synth(); // convert stack into cloudformation style - it creates: /cdk.out/localstack
        // .template.json
        // this describes all aws resources
        System.out.print("app synthesizing in progress...");
    }
}
