resource "aws_lb" "main" {
  name               = "main-alb"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [var.sg_id]
  subnets            = var.public_subnet_ids

  tags = { Name = "main-alb" }
}

resource "aws_lb_target_group" "app" {
  name                 = "app-tg"
  port                 = 8080
  protocol             = "HTTP"
  vpc_id               = var.vpc_id
  target_type          = "ip"  # Fargate는 반드시 "ip" 타입
  deregistration_delay = 30    # 배포 시 드레이닝 대기 단축

  health_check {
    # MANAGEMENT_SERVER_PORT=8080 설정으로 actuator가 메인 포트에서 서빙됨
    # context-path(/api)는 management server에 적용되지 않으므로 /actuator/health 사용
    path                = "/actuator/health"
    matcher             = "200"
    healthy_threshold   = 2
    unhealthy_threshold = 5
    interval            = 30   # 60s → 30s: 기동 완료 감지를 빠르게
    timeout             = 10   # 30s → 10s: Actuator health는 응답이 빠름
  }
}

resource "aws_lb_listener" "http" {
  load_balancer_arn = aws_lb.main.arn
  port              = 80
  protocol          = "HTTP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.app.arn
  }
}
