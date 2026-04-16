# DocGraph

Notion 문서 간 의존 관계를 그래프로 모델링하고, 변경 전파와 정합성 위반을 자동 감지하는 SaaS.

## 기술 스택

| | 기술 |
| --- | --- |
| 프론트엔드 | React |
| 백엔드 | Kotlin, Spring Boot |
| 데이터베이스 | PostgreSQL |
| 인프라 | AWS, Terraform |
| 문서 연동 | Notion API + Webhook |

## 구조

```
├── apps/
│   ├── backend/      # Kotlin Spring Boot
│   └── frontend/     # React
├── packages/         # 공유 라이브러리 (OpenAPI 생성 클라이언트 등, 필요 시)
├── terraform/
├── tests/            # pytest 시스템 테스트
├── docs/
├── docker-compose.yml  # pytest 실행용 테스트 환경 (tmpfs)
├── .env.example
└── .env              # gitignore
```

## 문서

[docs/](docs/README.md)
