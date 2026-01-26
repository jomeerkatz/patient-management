package jomeerkatz.pm.stack;

import software.amazon.awscdk.*;
import software.amazon.awscdk.services.ec2.Vpc;

// cdk: cloud development kid -> for creating cloud infra but with java code and NOT
// with CloudFormation code/syntax
// so what we can do is describing how the aws infra should look like
public class LocalStack extends Stack {
    private final Vpc vpc;

    // constructor for creating a stack
    public LocalStack(final App scope, final String id, final StackProps props) {
        super(scope, id, props);
        this.vpc = createVpc();
    }

    private Vpc createVpc() {
        return Vpc.Builder.create(this, "PatientManagementVPC").vpcName("PatientManagementVPC")
                .maxAzs(2).build(); // max available zones
    }

    public static void main(final String[] args) {
        App app = new App(AppProps.builder() // create new CDK project
                .outdir(". /cdk.out") // define, where we want to store the CloudFormation file
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
