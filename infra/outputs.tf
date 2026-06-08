output "alb_dns_name" {
  description = "ALB DNS 이름. Notion Webhook URL 및 OAuth Redirect URI 등록에 사용."
  value       = module.alb.dns_name
}

output "swagger_ui_url" {
  description = "Swagger UI 접속 주소. context-path(/api) 포함."
  value       = "${module.apigateway.api_endpoint}/api/swagger-ui/index.html"
}

output "api_gateway_endpoint" {
  description = "API Gateway default HTTPS endpoint. Use this for frontend, Notion OAuth redirect, webhook, and API access."
  value       = module.apigateway.api_endpoint
}

output "api_gateway_id" {
  description = "HTTP API ID."
  value       = module.apigateway.api_id
}

output "frontend_bucket_name" {
  description = "Private S3 bucket for React build artifacts."
  value       = module.s3_frontend.bucket_name
}

output "ecr_repository_url" {
  description = "Docker 이미지 push 대상 URL"
  value       = module.ecr.repository_url
}

output "rds_endpoint" {
  description = "Spring Boot DB 연결 주소"
  value       = module.rds.endpoint
  sensitive   = true
}
