output "rds_password_arn" {
  value     = aws_secretsmanager_secret.rds_password.arn
  sensitive = true
}

output "notion_client_id_arn" {
  value     = aws_secretsmanager_secret.notion_client_id.arn
  sensitive = true
}

output "notion_client_secret_arn" {
  value     = aws_secretsmanager_secret.notion_client_secret.arn
  sensitive = true
}

output "openai_api_key_arn" {
  value     = aws_secretsmanager_secret.openai_api_key.arn
  sensitive = true
}
