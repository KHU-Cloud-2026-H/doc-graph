variable "cluster_name" {
  type = string
}

variable "ecr_repository_url" {
  type = string
}

variable "image_tag" {
  type = string
}

variable "public_subnet_ids" {
  type = list(string)
}

variable "sg_id" {
  type = string
}

variable "target_group_arn" {
  type = string
}

variable "rds_endpoint" {
  type      = string
  sensitive = true
  default   = ""
}

variable "rds_password_secret_arn" {
  type      = string
  sensitive = true
}

variable "notion_client_id_secret_arn" {
  type      = string
  sensitive = true
}

variable "notion_client_secret_arn" {
  type      = string
  sensitive = true
}

variable "openai_api_key_secret_arn" {
  type      = string
  sensitive = true
}

variable "ai_openai_base_url" {
  type    = string
  default = "https://api.openai.com"
}

variable "ai_openai_model" {
  type    = string
  default = ""
}
