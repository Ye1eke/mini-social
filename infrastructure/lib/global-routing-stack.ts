import * as cdk from "aws-cdk-lib";
import { Construct } from "constructs";
import * as route53 from "aws-cdk-lib/aws-route53";
import * as route53targets from "aws-cdk-lib/aws-route53-targets";

export interface GlobalRoutingStackProps extends cdk.StackProps {
  domainName?: string;
  hostedZoneId?: string;
  endpoints: {
    region: string;
    url: string;
  }[];
}

export class GlobalRoutingStack extends cdk.Stack {
  constructor(scope: Construct, id: string, props: GlobalRoutingStackProps) {
    super(scope, id, props);

    // Only create if domain is provided
    if (!props.domainName || !props.hostedZoneId) {
      new cdk.CfnOutput(this, "Note", {
        value:
          "No domain configured. Skipping Route 53 setup. Access each region directly via their URLs.",
      });
      return;
    }

    // Import existing hosted zone
    const hostedZone = route53.HostedZone.fromHostedZoneAttributes(
      this,
      "HostedZone",
      {
        hostedZoneId: props.hostedZoneId,
        zoneName: props.domainName,
      },
    );

    // Create health checks and records for each region
    props.endpoints.forEach((endpoint, index) => {
      // Create a CNAME record for each region
      new route53.CnameRecord(this, `RegionRecord-${endpoint.region}`, {
        zone: hostedZone,
        recordName: `api-${endpoint.region}`,
        domainName: endpoint.url,
        ttl: cdk.Duration.minutes(1),
        comment: `Direct access to ${endpoint.region}`,
      });
    });

    // Create geolocation routing for main API endpoint
    // This routes users to the nearest region
    const geoRecords = [
      {
        region: "eu-central-1",
        location: route53.GeoLocation.continent(route53.Continent.EUROPE),
        url: props.endpoints.find((e) => e.region === "eu-central-1")?.url,
      },
      {
        region: "us-east-1",
        location: route53.GeoLocation.continent(
          route53.Continent.NORTH_AMERICA,
        ),
        url: props.endpoints.find((e) => e.region === "us-east-1")?.url,
      },
      {
        region: "us-east-1", // Default for rest of world
        location: route53.GeoLocation.default(),
        url: props.endpoints.find((e) => e.region === "us-east-1")?.url,
      },
    ];

    geoRecords.forEach((record, index) => {
      if (record.url) {
        new route53.CnameRecord(this, `GeoRecord-${index}`, {
          zone: hostedZone,
          recordName: "api",
          domainName: record.url,
          ttl: cdk.Duration.minutes(1),
          geoLocation: record.location,
          comment: `Geo-routed to ${record.region}`,
        });
      }
    });

    // Outputs
    new cdk.CfnOutput(this, "GlobalEndpoint", {
      value: `api.${props.domainName}`,
      description: "Global API endpoint with geo-routing",
    });

    props.endpoints.forEach((endpoint) => {
      new cdk.CfnOutput(this, `${endpoint.region}-Endpoint`, {
        value: `api-${endpoint.region}.${props.domainName}`,
        description: `Direct endpoint for ${endpoint.region}`,
      });
    });
  }
}
