output "bucket_name" {
  description = "Name of the provisioned local S3 bucket."
  value       = aws_s3_bucket.uploads.bucket
}

output "s3_endpoint" {
  description = "LocalStack S3 endpoint."
  value       = var.localstack_endpoint
}
