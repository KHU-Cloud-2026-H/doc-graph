variable "aws_region" {
  description = "배포 리전. Learner Lab은 us-east-1 또는 us-west-2만 허용."
  type        = string
  default     = "us-east-1"
}

variable "aws_profile" {
  description = "~/.aws/credentials에 등록된 named profile 이름"
  type        = string
  default     = "my-lab"
}

variable "vpc_cidr" {
  type    = string
  default = "10.0.0.0/16"
}

variable "azs" {
  type    = list(string)
  default = ["us-east-1a", "us-east-1b"]
}

variable "public_subnets" {
  type    = list(string)
  default = ["10.0.1.0/24", "10.0.2.0/24"]
}

variable "private_subnets" {
  type    = list(string)
  default = ["10.0.11.0/24", "10.0.12.0/24"]
}

variable "rds_password" {
  description = "RDS 마스터 비밀번호. tfvars로 주입, 코드에 하드코딩 금지."
  type        = string
  sensitive   = true
}

variable "app_image_tag" {
  description = "ECR에 push된 Spring Boot 이미지 태그"
  type        = string
  default     = "latest"
}

variable "notion_client_id" {
  description = "Notion OAuth 앱 Client ID"
  type        = string
  sensitive   = true
}

variable "notion_client_secret" {
  description = "Notion OAuth 앱 Client Secret"
  type        = string
  sensitive   = true
}

variable "ai_openai_api_key" {
  description = "OpenAI API Key"
  type        = string
  sensitive   = true
}

variable "ai_openai_base_url" {
  description = "OpenAI API Base URL"
  type        = string
  default     = "https://api.openai.com"
}

variable "ai_openai_model" {
  description = "사용할 OpenAI 모델명 (예: gpt-4o)"
  type        = string
}
