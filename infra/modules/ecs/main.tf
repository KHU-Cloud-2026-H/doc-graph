data "aws_caller_identity" "current" {}
data "aws_region" "current" {}

resource "aws_ecs_cluster" "main" {
  name = var.cluster_name
  tags = { Name = var.cluster_name }
}

resource "aws_cloudwatch_log_group" "app" {
  name              = "/ecs/app"
  retention_in_days = 7
}

resource "aws_ecs_task_definition" "app" {
  family                   = "app-task"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = 1024  # 1 vCPU — Spring Boot 4 JVM 기동에 최소 필요
  memory                   = 2048  # 2 GB — 512MB에서 JVM OOM으로 시작 실패

  execution_role_arn = "arn:aws:iam::${data.aws_caller_identity.current.account_id}:role/LabRole"
  task_role_arn      = "arn:aws:iam::${data.aws_caller_identity.current.account_id}:role/LabRole"

  container_definitions = jsonencode([{
    name      = "app"
    image     = "${var.ecr_repository_url}:${var.image_tag}"
    essential = true

    portMappings = [{
      containerPort = 8080
      protocol      = "tcp"
    }]

    environment = [
      { name = "SPRING_DATASOURCE_URL",         value = "jdbc:postgresql://${var.rds_endpoint}/docgraph" },
      { name = "SPRING_DATASOURCE_USERNAME",     value = "docgraph" },
      { name = "SPRING_DOCKER_COMPOSE_ENABLED",  value = "false" },
      { name = "MANAGEMENT_SERVER_PORT",         value = "8080" },
      { name = "JAVA_TOOL_OPTIONS",              value = "-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0" },
      { name = "AI_OPENAI_BASE_URL",             value = var.ai_openai_base_url },
      { name = "AI_OPENAI_MODEL",                value = var.ai_openai_model },
    ]

    # 민감 값은 Secrets Manager ARN 참조 — 태스크 실행 시 ECS가 직접 주입
    secrets = [
      { name = "SPRING_DATASOURCE_PASSWORD",                                          valueFrom = var.rds_password_secret_arn },
      { name = "SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_NOTION_CLIENT_ID",         valueFrom = var.notion_client_id_secret_arn },
      { name = "SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_NOTION_CLIENT_SECRET",     valueFrom = var.notion_client_secret_arn },
      { name = "AI_OPENAI_API_KEY",                                                   valueFrom = var.openai_api_key_secret_arn },
    ]

    healthCheck = {
      command     = ["CMD-SHELL", "wget -qO- http://localhost:8080/actuator/health || exit 1"]
      interval    = 30
      timeout     = 10
      retries     = 3
      startPeriod = 120  # Spring Boot + Flyway 기동 시간 확보
    }

    logConfiguration = {
      logDriver = "awslogs"
      options = {
        "awslogs-group"         = "/ecs/app"
        "awslogs-region"        = data.aws_region.current.name
        "awslogs-stream-prefix" = "ecs"
      }
    }
  }])

  depends_on = [aws_cloudwatch_log_group.app]
}

resource "aws_ecs_service" "app" {
  name            = "app-service"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.app.arn
  desired_count   = 1
  launch_type     = "FARGATE"

  # Spring Boot 기동 완료 전에 ALB health check 실패로 태스크가 교체되는 문제 방지
  health_check_grace_period_seconds = 180

  network_configuration {
    subnets          = var.public_subnet_ids
    security_groups  = [var.sg_id]
    assign_public_ip = true  # NAT GW 없이 ECR pull 가능
  }

  load_balancer {
    target_group_arn = var.target_group_arn
    container_name   = "app"
    container_port   = 8080
  }

  # 배포 실패 시 무한 재시작 루프 방지
  # rollback = false: 최초 배포 시 롤백 대상이 없어 오류 발생하는 Learner Lab 문제 회피
  deployment_circuit_breaker {
    enable   = true
    rollback = false
  }

  deployment_controller {
    type = "ECS"
  }
}
