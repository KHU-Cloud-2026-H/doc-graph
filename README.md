# DocGraph

Notion 문서 간 의존 관계를 그래프로 모델링하고, 변경 전파와 정합성 위반을 자동 감지하는 SaaS.

## 기술 스택

- **백엔드** — Kotlin, Spring Boot 4, Java 21
- **데이터베이스** — PostgreSQL 17
- **프론트엔드** — React (TypeScript)
- **인프라** — AWS (ECS Fargate + RDS), Terraform
- **문서 연동** — Notion API + Webhook

## 구조

```
├── apps/
│   ├── backend/      # Kotlin Spring Boot
│   └── frontend/     # React
├── packages/         # 공유 라이브러리 (OpenAPI 생성 클라이언트 등)
├── terraform/
├── tests/            # pytest 시스템 테스트
├── docs/
├── docker-compose.yml
├── docker-compose.test.yml  # 시스템 테스트용 overlay
├── .env.example
└── .env              # gitignore
```

## 문서

- [제품 기획](docs/product.md)
- [아키텍처](docs/architecture.md)
- [개발 환경 가이드](docs/development.md)
