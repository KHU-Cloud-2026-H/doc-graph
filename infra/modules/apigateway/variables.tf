variable "name" {
  description = "Base name for API Gateway resources."
  type        = string
  default     = "docgraph"
}

variable "region" {
  description = "AWS region."
  type        = string
}

variable "vpc_link_subnet_ids" {
  description = "Subnet IDs used by API Gateway VPC Link."
  type        = list(string)
}

variable "vpc_link_security_group_ids" {
  description = "Security group IDs attached to API Gateway VPC Link ENIs."
  type        = list(string)
}

variable "alb_listener_arn" {
  description = "ALB HTTP listener ARN used as the HTTP API private integration target."
  type        = string
}

variable "frontend_bucket_name" {
  description = "Private S3 bucket that stores the React build output."
  type        = string
}

variable "frontend_bucket_arn" {
  description = "Private S3 bucket ARN."
  type        = string
}

variable "integration_role_arn" {
  description = "Existing LabRole ARN used by API Gateway REST API to read private S3 objects."
  type        = string
}

variable "tags" {
  description = "Tags applied to API Gateway resources."
  type        = map(string)
  default     = {}
}
