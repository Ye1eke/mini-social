import * as cdk from "aws-cdk-lib";
import { Construct } from "constructs";
import * as ec2 from "aws-cdk-lib/aws-ec2";
import * as iam from "aws-cdk-lib/aws-iam";
import * as eb from "aws-cdk-lib/aws-elasticbeanstalk";

type BackendConfig = {
  applicationName: string;
  environmentName: string;
  solutionStackName: string;
  certificateArn?: string;
  appPort?: string;
  env?: Record<string, string>;
};

export class MiniSocialStack extends cdk.Stack {
  constructor(
    scope: Construct,
    id: string,
    props: cdk.StackProps & { config: BackendConfig },
  ) {
    super(scope, id, props);

    const cfg = props.config;
    const appPort = cfg.appPort ?? "8080";

    // IAM role for EC2 instances
    const ebEc2Role = new iam.Role(this, "EbEc2Role", {
      assumedBy: new iam.ServicePrincipal("ec2.amazonaws.com"),
      managedPolicies: [
        iam.ManagedPolicy.fromAwsManagedPolicyName(
          "AWSElasticBeanstalkWebTier",
        ),
        iam.ManagedPolicy.fromAwsManagedPolicyName(
          "AmazonSSMManagedInstanceCore",
        ),
      ],
    });

    const instanceProfile = new iam.CfnInstanceProfile(
      this,
      "EbInstanceProfile",
      {
        roles: [ebEc2Role.roleName],
      },
    );

    // EB Application
    const app = new eb.CfnApplication(this, "EbApp", {
      applicationName: cfg.applicationName,
    });

    // EB Option Settings - Let EB manage security groups
    const optionSettings: eb.CfnEnvironment.OptionSettingProperty[] = [
      {
        namespace: "aws:elasticbeanstalk:environment",
        optionName: "EnvironmentType",
        value: "LoadBalanced",
      },
      {
        namespace: "aws:elasticbeanstalk:environment",
        optionName: "LoadBalancerType",
        value: "application",
      },
      {
        namespace: "aws:autoscaling:launchconfiguration",
        optionName: "IamInstanceProfile",
        value: instanceProfile.ref,
      },
      {
        namespace: "aws:elasticbeanstalk:environment:process:default",
        optionName: "Port",
        value: appPort,
      },
      {
        namespace: "aws:elasticbeanstalk:environment:process:default",
        optionName: "Protocol",
        value: "HTTP",
      },
    ];

    // HTTPS listener if cert provided
    if (cfg.certificateArn) {
      optionSettings.push(
        {
          namespace: "aws:elbv2:listener:443",
          optionName: "ListenerEnabled",
          value: "true",
        },
        {
          namespace: "aws:elbv2:listener:443",
          optionName: "Protocol",
          value: "HTTPS",
        },
        {
          namespace: "aws:elbv2:listener:443",
          optionName: "SSLCertificateArns",
          value: cfg.certificateArn,
        },
      );
    }

    // Environment variables
    for (const [k, v] of Object.entries(cfg.env ?? {})) {
      optionSettings.push({
        namespace: "aws:elasticbeanstalk:application:environment",
        optionName: k,
        value: v,
      });
    }

    // EB Environment
    const env = new eb.CfnEnvironment(this, "EbEnv", {
      environmentName: cfg.environmentName,
      applicationName: app.applicationName!,
      solutionStackName: cfg.solutionStackName,
      optionSettings,
    });

    env.addDependency(app);

    new cdk.CfnOutput(this, "EnvironmentName", { value: cfg.environmentName });
    new cdk.CfnOutput(this, "EnvironmentUrl", {
      value: `http://${cfg.environmentName}.eba-XXXXXXXX.eu-central-1.elasticbeanstalk.com`,
      description: "Application URL (will be available after deployment)",
    });
  }
}
