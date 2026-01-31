package jomeerkatz.pm.stack;

import software.amazon.awscdk.*;
import software.amazon.awscdk.services.ec2.InstanceClass;
import software.amazon.awscdk.services.ec2.InstanceSize;
import software.amazon.awscdk.services.ec2.InstanceType;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.rds.*;

// cdk: cloud development kid -> for creating cloud infra but with java code and NOT
// with CloudFormation code/syntax
// so what we can do is describing how the aws infra should look like
public class LocalStack extends Stack { // stack class, where we define which ressources are in the stack
    private final Vpc vpc;

    // constructor for creating a stack
    public LocalStack(final App scope, final String id, final StackProps props) {
        super(scope, id, props);
        this.vpc = createVpc();

        DatabaseInstance authServiceDB = createDatabase("AuthServiceDB", "auth-service-db");

        DatabaseInstance patientServiceDB = createDatabase("PatientServiceDB", "patient-service-db");
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
                .instanceType(InstanceType.of(InstanceClass.BURSTABLE2, InstanceSize.MICRO)) // doesnt matter that much bec of local dev
                .allocatedStorage(20)
                .credentials(Credentials.fromGeneratedSecret("admin_user"))
                .databaseName(dbName)
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();
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
