# Integration test ideas

This starter keeps integration tests separate from the application module so you can add them incrementally.

Suggested tests:
1. Start LocalStack.
2. Apply Terraform to create `practice-file-uploads`.
3. Start the Spring Boot app with `S3_ENDPOINT=http://localhost:4566`.
4. Upload a small file and assert that the returned key matches the original filename.
5. List objects and assert the uploaded key is present.
6. Download the object and compare the bytes.
7. Delete it and assert it no longer appears in the list.

For an automated CI workflow, consider Testcontainers and an isolated LocalStack container. Verify current LocalStack image/licensing requirements before implementing CI.
