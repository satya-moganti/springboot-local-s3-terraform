# Troubleshooting Guide: Spring Boot + Terraform + LocalStack

**Project:** Local S3 File Upload Practice  
**Environment:** Windows 11, PowerShell, Docker Desktop, Java 17, Spring Boot, Terraform  
**Objective:** Run an S3-compatible storage service locally without provisioning real AWS resources.

---

## 1. Issue Summary

### Error 1: LocalStack authentication failure

When running:

```powershell
docker compose logs -f localstack
```

The container reports an error similar to:

```text
Reason: No credentials were found in the environment.
Please set the LOCALSTACK_AUTH_TOKEN variable to a valid auth token.

LocalStack has quit.
LocalStack Pro features can only be used with a valid license.
```

### Error 2: Missing environment variable

Docker Compose may also report:

```text
error while interpolating services.localstack.environment.LOCALSTACK_AUTH_TOKEN:
required variable LOCALSTACK_AUTH_TOKEN is missing a value:
Set LOCALSTACK_AUTH_TOKEN first
```

### Impact

LocalStack exits before providing the local S3 endpoint. Consequently, Terraform cannot create the bucket and the Spring Boot application cannot access the local storage service.

**Important:** This is primarily a container startup or authentication issue. It does not, by itself, indicate a problem with the Java application or Terraform resource definitions.

---

## 2. Root Cause

The configured LocalStack image requires an authentication token, but one of the following conditions applies:

- `LOCALSTACK_AUTH_TOKEN` is not configured.
- The token is invalid or expired.
- The selected image or distribution has authentication or licensing requirements that are not satisfied.
- Docker Compose cannot resolve the required environment variable.

The `latest` image tag can change over time. Verify the requirements for your selected image rather than assuming that changing the tag alone will fix the issue.

### Official references

- [LocalStack authentication documentation](https://docs.localstack.cloud/aws/getting-started/auth-token/)
- [LocalStack documentation](https://docs.localstack.cloud/)
- [LocalStack web application](https://app.localstack.cloud/)

---

## 3. Step 1 — Stop the Failed Container

Open PowerShell and navigate to the project root:

```powershell
cd springboot-local-s3-terraform

```

Stop the existing Compose services:

```powershell
docker compose down
```

This removes the Compose containers and network. It does not delete your project source files.

Check Docker Desktop is running before proceeding.

---

## 4. Step 2 — Review the Docker Compose Configuration

Open `docker-compose.yml` and locate the LocalStack service.

The image may currently be configured as:

```yaml
image: localstack/localstack:latest
```

Confirm the selected image's current authentication and licensing requirements using the official documentation.

If the selected image requires a token, the service's environment configuration should include:

```yaml
environment:
  SERVICES: s3
  AWS_DEFAULT_REGION: us-east-1
  PERSISTENCE: "0"
  LOCALSTACK_AUTH_TOKEN: ${LOCALSTACK_AUTH_TOKEN:?Set LOCALSTACK_AUTH_TOKEN first}
```

**Configuration note:** Merge these entries into the existing LocalStack service. Preserve its existing ports, volumes, health check and other required settings.

The expression `${LOCALSTACK_AUTH_TOKEN:?...}` instructs Docker Compose to stop with an explanatory error if the variable is missing.

---

## 5. Step 3 — Configure the LocalStack Token

Complete this step only if your selected LocalStack image requires a token and your account provides a suitable one.

1. Sign in to the LocalStack web application.
2. Review the available authentication and licensing options.
3. Obtain a valid token according to the official instructions.

In PowerShell, set the token for the current terminal session:

```powershell
$env:LOCALSTACK_AUTH_TOKEN = "YOUR_VALID_TOKEN"
```

Replace `YOUR_VALID_TOKEN` with the actual token. Do not include the placeholder literally.

Verify that the variable is set without displaying the secret:

```powershell
if ($env:LOCALSTACK_AUTH_TOKEN) {
    Write-Host "LocalStack token is set."
} else {
    Write-Host "LocalStack token is missing."
}
```

Start the service:

```powershell
docker compose up -d localstack
```

Review the logs:

```powershell
docker compose logs -f localstack
```

The environment variable applies to the current PowerShell session. Set it again in a new session unless you have configured another secure method.

**Security:** Never commit the token to GitHub or place it directly in a tracked Compose file.

---

## 6. Step 4 — Verify LocalStack Health

Once the container starts, open a separate PowerShell terminal and run:

```powershell
Invoke-RestMethod http://localhost:4566/_localstack/health
```

Check the response for service health information and confirm that S3 is available.

If the service fails to start, inspect the most recent logs:

```powershell
docker compose logs --tail 100 localstack
```

You can also check container status:

```powershell
docker compose ps
```

**Do not continue to Terraform provisioning until the local service is running and accessible.**

---

## 7. Step 5 — Initialize and Validate Terraform

Navigate to the Terraform directory:

```powershell
cd terraform
```

Initialize the provider:

```powershell
terraform init
```

Format and validate the configuration:

```powershell
terraform fmt -recursive
terraform validate
```

Preview the proposed changes:

```powershell
terraform plan
```

Review `providers.tf` and confirm that the AWS provider is configured to use the LocalStack endpoint:

```text
http://localhost:4566
```

Also verify that the provider uses test credentials and local endpoint settings rather than real AWS infrastructure.

If validation or planning fails, inspect the error message before applying changes.

Provision the bucket:

```powershell
terraform apply
```

Review the proposed changes and enter `yes` when prompted.

---

## 8. Step 6 — Verify the S3 Bucket

Return to the project root: springboot-local-s3-terraform

```powershell
cd ../..
cd springboot-local-s3-terraform

```

List the buckets created in LocalStack:

```powershell
docker compose exec localstack awslocal s3 ls
```

The expected bucket name is:

```text
practice-file-uploads
```

If the bucket appears, Terraform has successfully created the resource in the local S3 environment.

If it does not appear, check:

- Whether `terraform apply` completed successfully.
- Whether Terraform and Spring Boot use the same endpoint and region.
- Whether the expected bucket name is configured in `terraform/variables.tf`.
- Whether the LocalStack container has restarted or the local environment was reset.

---

## 9. Step 7 — Start the Spring Boot Application

Once the S3 service and bucket are ready, open PowerShell:

```powershell
cd app
```

Start the application:

```powershell
mvn spring-boot:run
```

Verify that the application starts successfully and listens on its configured port.

Check `app/src/main/resources/application.yml` for these settings:

```yaml
S3_ENDPOINT: http://localhost:4566
AWS_REGION: us-east-1
S3_BUCKET: practice-file-uploads
```

These are the intended values for the current LocalStack setup. Ensure the application's actual configuration uses the same endpoint and bucket created by Terraform.

---

## 10. If You Do Not Have a LocalStack Token

If your account does not provide a suitable token for the selected image, do not repeatedly retry the same configuration.

For a local-only Spring Boot and Terraform learning project, consider switching to **MinIO** or another suitable S3-compatible service.

A migration requires reviewing and updating:

- `docker-compose.yml` to start the replacement service.
- `terraform/providers.tf` to use the replacement S3 endpoint and compatible provider settings.
- `terraform/variables.tf` if the endpoint is configurable there.
- `app/src/main/resources/application.yml` to point Spring Boot to the replacement endpoint.
- Local access and secret keys, bucket setup and service health checks.

The AWS SDK for Java can work with compatible S3 endpoints, but the endpoint, credentials and path-style settings must match the selected service.

Before switching, check the replacement service's current licensing, image and configuration requirements. The existing LocalStack-specific commands, such as `awslocal`, may not be available in another service.

---

## 11. Troubleshooting Checklist

Use this checklist to track the recovery process.

- [ ] Docker Desktop is running.
- [ ] The LocalStack image and its authentication requirements have been verified.
- [ ] A valid token is configured if required.
- [ ] `docker compose up -d localstack` completes successfully.
- [ ] The container remains running.
- [ ] The local health endpoint responds.
- [ ] Terraform is configured to use the intended local S3 endpoint.
- [ ] `terraform init` completes successfully.
- [ ] `terraform fmt -recursive` and `terraform validate` complete successfully.
- [ ] `terraform plan` shows the expected bucket creation.
- [ ] `terraform apply` completes successfully.
- [ ] `practice-file-uploads` appears in the bucket listing.
- [ ] Spring Boot starts and uses the same endpoint and bucket.

---

## 12. Security and Cost Controls

For this local-only learning environment:

- Do not use real AWS credentials for LocalStack testing.
- Never commit authentication tokens, passwords or secret keys to GitHub.
- Keep Terraform state files and sensitive local configuration out of version control.
- Review Terraform plans before applying changes.
- Keep Docker Desktop and the local services stopped when they are not needed.
- Do not configure a real AWS endpoint accidentally when intending to run local tests.
- Remember that local emulation does not replace all testing against actual AWS services.

### Remove Terraform-managed resources

If you want to remove the bucket managed by Terraform, first ensure the local S3 service is running and navigate to the Terraform directory:

```powershell
cd springboot-local-s3-terraform\terraform
terraform destroy
```

Review the proposed changes and confirm the operation.

Then stop the Compose services from the project root:

```powershell
 cd springboot-local-s3-terraform
docker compose down
```

Terraform must be able to reach the configured endpoint to destroy the resources it manages. Do not remove the service before running `terraform destroy` if Terraform still needs to contact it.

---

## 13. Project Structure Reference

```text
springboot-local-s3-terraform/
├── docker-compose.yml
├── app/
│   ├── pom.xml
│   └── src/
│       └── main/
│           └── resources/
│               └── application.yml
├── terraform/
│   ├── versions.tf
│   ├── providers.tf
│   ├── main.tf
│   ├── variables.tf
│   └── outputs.tf
├── .gitignore
└── troubleshoot.md
```

---

## 14. Resolution Summary

The immediate issue is that LocalStack is not starting because authentication or licensing requirements are not being met, or because Docker Compose cannot resolve the required token variable.

Resolve the startup issue first. Then verify the local S3 endpoint, initialize and validate Terraform, create the bucket, and start the Spring Boot application.

If an appropriate LocalStack setup is not available, evaluate a different local S3-compatible service and update the Compose, Terraform and application endpoint configurations consistently.

**Expected outcome:** A working local development environment in which Terraform manages an S3-compatible bucket and the Spring Boot application can upload, list, download and delete files without provisioning real AWS resources.
