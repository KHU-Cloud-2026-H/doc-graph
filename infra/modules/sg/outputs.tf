output "alb_sg_id"     { value = aws_security_group.alb.id }
output "fargate_sg_id" { value = aws_security_group.fargate.id }
output "rds_sg_id"     { value = aws_security_group.rds.id }
