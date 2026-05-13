# ── 1단계: 네트워크 ──────────────────────────────────────────
module "vpc" {
  source          = "./modules/vpc"
  vpc_cidr        = var.vpc_cidr
  azs             = var.azs
  public_subnets  = var.public_subnets
  private_subnets = var.private_subnets
}

module "sg" {
  source = "./modules/sg"
  vpc_id = module.vpc.vpc_id
}

# ── 2단계: 컨테이너 레지스트리 ───────────────────────────────
module "ecr" {
  source = "./modules/ecr"
}

# ── 3단계: 로드밸런서 + 컨테이너 실행 ───────────────────────
module "alb" {
  source            = "./modules/alb"
  vpc_id            = module.vpc.vpc_id
  public_subnet_ids = module.vpc.public_subnet_ids
  sg_id             = module.sg.alb_sg_id
}

module "ecs" {
  source             = "./modules/ecs"
  cluster_name       = "app-cluster"
  ecr_repository_url = module.ecr.repository_url
  image_tag          = var.app_image_tag
  public_subnet_ids  = module.vpc.public_subnet_ids
  sg_id              = module.sg.fargate_sg_id
  target_group_arn   = module.alb.target_group_arn
  rds_endpoint       = module.rds.endpoint
  ai_openai_base_url = var.ai_openai_base_url
  ai_openai_model    = var.ai_openai_model

  rds_password_secret_arn     = module.secrets.rds_password_arn
  notion_client_id_secret_arn = module.secrets.notion_client_id_arn
  notion_client_secret_arn    = module.secrets.notion_client_secret_arn
  openai_api_key_secret_arn   = module.secrets.openai_api_key_arn
}

# ── 4단계: 데이터베이스 ──────────────────────────────────────
module "rds" {
  source             = "./modules/rds"
  private_subnet_ids = module.vpc.private_subnet_ids
  sg_id              = module.sg.rds_sg_id
  db_password        = var.rds_password
}

# ── 5단계: 시크릿 관리 ───────────────────────────────────────
module "secrets" {
  source               = "./modules/secrets"
  rds_password         = var.rds_password
  notion_client_id     = var.notion_client_id
  notion_client_secret = var.notion_client_secret
  openai_api_key       = var.ai_openai_api_key
}
