package jomeerkatz.pm.stack;

import org.jetbrains.annotations.NotNull;
import software.amazon.awscdk.*;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.ec2.InstanceType;
import software.amazon.awscdk.services.msk.CfnCluster;
import software.amazon.awscdk.services.rds.*;
import software.amazon.awscdk.services.route53.CfnHealthCheck;

import java.util.List;
import java.util.stream.Collectors;

// cdk: cloud development kid -> for creating cloud infra but with java code and NOT
// with CloudFormation code/syntax
// so what we can do is describing how the aws infra should look like
// IaC, but higher abstraction lvl
// we just define, how the infrastructure should look like
// will get converted to CloudFormation format, which is IaC in aws "language"
public class LocalStack extends Stack {
    private final Vpc vpc;

    // constructor for creating a stack
    public LocalStack(final App scope, final String id, final StackProps props) {
        super(scope, id, props);
        this.vpc = createVpc();
        DatabaseInstance authServiceDB = createDatabase("AuthServiceDB", "auth-service-db");
        DatabaseInstance patientServiceDB = createDatabase("PatientServiceDB", "patient-service-db");
        CfnHealthCheck authDbHealthCheck = createDbHealthCheck(authServiceDB, "AuthServiceDBHealthCheck");
        CfnHealthCheck patientDbHealthCheck = createDbHealthCheck(authServiceDB, "PatientServiceDBHealthCheck");
    }

    private Vpc createVpc() {
        return Vpc.Builder.create(this, "PatientManagementVPC").vpcName("PatientManagementVPC")
                .maxAzs(2).build(); // max available zones
    }

    private DatabaseInstance createDatabase(String id, String dbName) {
        // construct, which will create a database
        return DatabaseInstance.Builder
                .create(this, id)
                .engine(DatabaseInstanceEngine.postgres(PostgresInstanceEngineProps.builder()
                        .version(PostgresEngineVersion.VER_17_2).build()))
                .vpc(this.vpc)
                .instanceType(InstanceType.of(InstanceClass.BURSTABLE2, InstanceSize.MICRO)) // doesn't matter that much bec of local dev
                .allocatedStorage(20)
                .credentials(Credentials.fromGeneratedSecret("admin_user"))
                .databaseName(dbName)
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();
    }

    private CfnHealthCheck createDbHealthCheck(DatabaseInstance db, String id) {
        return CfnHealthCheck.Builder.create(this, id)
                .healthCheckConfig(CfnHealthCheck.HealthCheckConfigProperty.builder()
                        .type("TCP")
                        .port(Token.asNumber(db.getDbInstanceEndpointPort()))
                        .ipAddress(db.getDbInstanceEndpointAddress())
                        .requestInterval(30) // 30 seconds
                        .failureThreshold(3) // try it 3 times
                        .build())
                .build();
    }

    // Creates an Amazon MSK (Managed Streaming for Apache Kafka) cluster
    // Kafka but from AWS, which will handle scaling etc.
    private CfnCluster createMskCluster() {
        // Collect all private subnet IDs from the VPC
        // MSK brokers must run inside private subnets
        List<@NotNull String> collectedSubnets =
                vpc.getPrivateSubnets()              // get all private subnets of the VPC
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
                        CfnCluster.BrokerNodeGroupInfoProperty.builder()
                                .instanceType("kafka.m5.xlarge") // EC2 instance type for each broker
                                .clientSubnets(collectedSubnets) // subnets where brokers will be placed
                                .brokerAzDistribution("DEFAULT") // distribute brokers across AZs
                                .build())                        // build broker node group config
                .build();                                       // build the MSK cluster resource
    }

    public static void main(final String[] args) {
        App app = new App(AppProps.builder() // create new CDK project
                .outdir("./cdk.out") // define, where we want to store the CloudFormation file
                .build()); // no resources created yet, just the "wrapper"

        StackProps props = StackProps.builder() // create config for stack
                .synthesizer(new BootstraplessSynthesizer()) // we define, project should not need bootstrap resources
                .build();

        new LocalStack(app, "localstack", props); // instantiate - just empty "wrapper"
        app.synth(); // convert stack into cloudformation style - it creates: /cdk.out/localstack.template.json
        // this describes all aws resources
        System.out.print("app synthesizing in progress...");
    }
}
