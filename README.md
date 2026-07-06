# DocGraph

팀의 Notion 문서(기획·요구사항·설계 등)는 서로를 링크·멘션하며 의존하지만, 한 문서가 바뀌어도 딸린 문서는 방치돼 정합성이 조용히 깨진다. DocGraph는 문서 간 의존 관계를 그래프로 모델링하고, 변경을 Webhook으로 감지해 영향받는 문서와의 정합성 충돌을 AI로 자동 검증하는 SaaS다.

```
Notion 변경 → 영향 엣지 식별 → AI 정합성 검증 → 수정 제안 → Notion 반영
```

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
│   ├── docs/         # Redoc API 문서
│   └── frontend/     # React
├── packages/
│   └── api-types/    # TypeScript API 타입
├── infra/
├── tests/            # pytest 시스템 테스트
├── docs/
├── docker-compose.yml
├── docker-compose.test.yml   # 시스템 테스트 override
├── Justfile          # dev 명령 진입점
├── .env              # 환경변수 (commit)
├── .env.local        # 개인 시크릿 (gitignored)
└── .env.keys         # 복호화 키 (gitignored)
```

## 실행

사전 준비물(Java 21·Node.js 24+·uv 등)과 환경변수 설정은 [개발 환경 가이드](docs/development.md) 참고.

```bash
just bootRun        # 인프라 기동 + 백엔드 실행
just dev-frontend   # 프론트엔드(Vite) 실행
```

## 문서

- [제품 기획](docs/product.md)
- [아키텍처](docs/architecture.md)
- [개발 환경 가이드](docs/development.md)
