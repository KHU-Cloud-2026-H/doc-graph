variable "bucket_name_prefix" {
  description = "Prefix for the generated frontend artifact bucket name."
  type        = string
  default     = "docgraph-frontend"
}

variable "tags" {
  description = "Tags applied to frontend S3 resources."
  type        = map(string)
  default     = {}
}
