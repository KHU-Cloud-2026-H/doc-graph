resource "aws_db_subnet_group" "main" {
  name       = "main-db-subnet-group"
  subnet_ids = var.private_subnet_ids  # RDS는 반드시 2개 AZ 이상의 서브넷 필요

  tags = { Name = "main-db-subnet-group" }
}

resource "aws_db_instance" "main" {
  identifier        = "main-postgres"
  engine            = "postgres"
  engine_version    = "17"
  instance_class    = "db.t3.micro"
  allocated_storage = 20

  db_name  = var.db_name
  username = var.db_username
  password = var.db_password

  db_subnet_group_name   = aws_db_subnet_group.main.name
  vpc_security_group_ids = [var.sg_id]

  multi_az            = false  # Learner Lab 크레딧 절약
  publicly_accessible = false  # Private Subnet 격리

  skip_final_snapshot = true

  tags = { Name = "main-postgres" }
}
