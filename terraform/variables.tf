variable "aws_region" {
  description = "Region used by the local S3-compatible service."
  type        = string
  default     = "eu-west-1"
}

variable "localstack_endpoint" {
  description = "LocalStack endpoint exposed on the host."
  type        = string
  default     = "http://localhost:4566"
}

variable "bucket_name" {
  description = "S3 bucket name to create in LocalStack."
  type        = string
  default     = "practice-file-uploads"
}
