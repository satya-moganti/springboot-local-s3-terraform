resource "aws_s3_bucket" "uploads" {
  bucket = var.bucket_name

  tags = {
    Project     = "springboot-local-s3-terraform"
    Environment = "local"
    ManagedBy   = "terraform"
  }
}
