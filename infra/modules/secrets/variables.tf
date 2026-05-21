variable "rds_password" {
  type      = string
  sensitive = true
}

variable "notion_client_id" {
  type      = string
  sensitive = true
}

variable "notion_client_secret" {
  type      = string
  sensitive = true
}

variable "openai_api_key" {
  type      = string
  sensitive = true
}
