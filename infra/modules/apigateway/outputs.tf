output "api_id" {
  description = "HTTP API ID."
  value       = aws_apigatewayv2_api.main.id
}

output "api_endpoint" {
  description = "HTTP API default HTTPS endpoint."
  value       = aws_apigatewayv2_api.main.api_endpoint
}

output "frontend_proxy_rest_api_id" {
  description = "Internal REST API ID used as the S3 frontend proxy."
  value       = aws_api_gateway_rest_api.frontend.id
}
