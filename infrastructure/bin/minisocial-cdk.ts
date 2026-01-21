#!/usr/bin/env node
import * as cdk from "aws-cdk-lib";
import { MiniSocialStack } from "../lib/minisocial-stack";
import * as dotenv from "dotenv";
import * as path from "path";

// Load environment variables from .env file
dotenv.config({ path: path.join(__dirname, "..", ".env") });

const app = new cdk.App();

const backend = app.node.tryGetContext("backend");
if (!backend) throw new Error("Missing context.backend in cdk.json");

// Get region from context or fall back to default
const region =
  app.node.tryGetContext("region") || process.env.CDK_DEFAULT_REGION;

// Override env variables from .env file if they exist
if (backend.env) {
  backend.env = {
    SPRING_DATASOURCE_URL:
      process.env.SPRING_DATASOURCE_URL || backend.env.SPRING_DATASOURCE_URL,
    DB_USERNAME: process.env.DB_USERNAME || backend.env.DB_USERNAME,
    DB_PASSWORD: process.env.DB_PASSWORD || backend.env.DB_PASSWORD,
    JWT_SECRET: process.env.JWT_SECRET || backend.env.JWT_SECRET,
    B2_ENDPOINT: process.env.B2_ENDPOINT || backend.env.B2_ENDPOINT,
    B2_ACCESS_KEY_ID:
      process.env.B2_ACCESS_KEY_ID || backend.env.B2_ACCESS_KEY_ID,
    B2_SECRET_ACCESS_KEY:
      process.env.B2_SECRET_ACCESS_KEY || backend.env.B2_SECRET_ACCESS_KEY,
    B2_BUCKET_NAME: process.env.B2_BUCKET_NAME || backend.env.B2_BUCKET_NAME,
    SERVER_PORT: process.env.SERVER_PORT || backend.env.SERVER_PORT || "5000",
  };
}

new MiniSocialStack(app, "MiniSocialBackendEb", {
  env: {
    account: process.env.CDK_DEFAULT_ACCOUNT,
    region: region,
  },
  config: backend,
});
