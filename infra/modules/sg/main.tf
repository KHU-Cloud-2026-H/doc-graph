# ── sg-alb ────────────────────────────────────────────────────────────────────
# 인터넷 → ALB: 80, 443

resource "aws_security_group" "alb" {
  name        = "alb-sg"
  description = "ALB: allow HTTP/HTTPS from internet"
  vpc_id      = var.vpc_id

  ingress {
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = { Name = "sg-alb" }
}

# ── sg-fargate ────────────────────────────────────────────────────────────────
# sg-alb → Fargate: 8080

resource "aws_security_group" "fargate" {
  name        = "fargate-sg"
  description = "Fargate: allow 8080 from ALB only"
  vpc_id      = var.vpc_id

  ingress {
    from_port       = 8080
    to_port         = 8080
    protocol        = "tcp"
    security_groups = [aws_security_group.alb.id]  # ALB SG에서 오는 트래픽만 허용
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]  # ECR pull, 외부 API 호출에 필요
  }

  tags = { Name = "sg-fargate" }
}

# ── sg-rds ────────────────────────────────────────────────────────────────────
# sg-fargate → RDS: 5432

resource "aws_security_group" "rds" {
  name        = "rds-sg"
  description = "RDS: allow 5432 from Fargate only"
  vpc_id      = var.vpc_id

  ingress {
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.fargate.id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = { Name = "sg-rds" }
}
