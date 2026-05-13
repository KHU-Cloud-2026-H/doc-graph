resource "aws_secretsmanager_secret" "rds_password" {
  name                    = "docgraph/rds-password"
  recovery_window_in_days = 0  # terraform destroy 시 즉시 삭제 — Learner Lab 재배포 대응
  tags                    = { Name = "docgraph/rds-password" }
}

resource "aws_secretsmanager_secret_version" "rds_password" {
  secret_id     = aws_secretsmanager_secret.rds_password.id
  secret_string = var.rds_password
}

resource "aws_secretsmanager_secret" "notion_client_id" {
  name                    = "docgraph/notion-client-id"
  recovery_window_in_days = 0
  tags                    = { Name = "docgraph/notion-client-id" }
}

resource "aws_secretsmanager_secret_version" "notion_client_id" {
  secret_id     = aws_secretsmanager_secret.notion_client_id.id
  secret_string = var.notion_client_id
}

resource "aws_secretsmanager_secret" "notion_client_secret" {
  name                    = "docgraph/notion-client-secret"
  recovery_window_in_days = 0
  tags                    = { Name = "docgraph/notion-client-secret" }
}

resource "aws_secretsmanager_secret_version" "notion_client_secret" {
  secret_id     = aws_secretsmanager_secret.notion_client_secret.id
  secret_string = var.notion_client_secret
}

resource "aws_secretsmanager_secret" "openai_api_key" {
  name                    = "docgraph/openai-api-key"
  recovery_window_in_days = 0
  tags                    = { Name = "docgraph/openai-api-key" }
}

resource "aws_secretsmanager_secret_version" "openai_api_key" {
  secret_id     = aws_secretsmanager_secret.openai_api_key.id
  secret_string = var.openai_api_key
}
