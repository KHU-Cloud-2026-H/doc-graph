# CONTRIBUTING

## 커밋 메시지 컨벤션

[Conventional Commits](https://www.conventionalcommits.org)를 따른다.

```
<type>(<scope>): <subject>
```

- **Type 필수**, 아래 목록 중 하나.
- **Scope 선택**, 영역이 명백히 한정될 때 아래 목록 중 하나. 두 영역 이상이거나 모노레포 전반이면 생략. type이 영역과 동의어인 경우(`docs`)도 생략.

### Type

| type | 용도 |
| --- | --- |
| `feat` | 새 기능 |
| `fix` | 버그 수정 |
| `docs` | 문서 변경 |
| `refactor` | 동작 변경 없는 구조 개선 |
| `test` | 테스트 추가·수정 |
| `build` | 빌드·의존성 설정 |
| `chore` | 그 외 (`.gitignore`, 에디터 설정 등) |

### Scope

| scope | 대상 |
| --- | --- |
| `frontend` | `apps/frontend/` |
| `backend` | `apps/backend/` |
| `api-types` | `packages/api-types/` |
| `tests` | `tests/` |
| `docs` | `docs/` |
| `infra` | `infra/` |

### 예시

```
feat(backend): webhook 수신 어댑터 추가
fix(api-types): generate 스크립트 종료 코드 처리
docs: 도메인 간 통신 컨벤션 정리
refactor(backend): validation worker를 단일 메서드로 통합
test(backend): ValidationTask 라이프사이클 시나리오 추가
chore: gitignore에 build 산출물 추가
```

## Issue

작업 단위 등록·진행 추적.

### Label

| 축 | label | 비고 |
| --- | --- | --- |
| priority | `priority:P0` / `priority:P1` / `priority:P2` | `docs/product.md` 우선순위 매핑 |
| type | `type:feature` / `type:bug` / `type:refactor` / `type:docs` / `type:chore` / `type:test` | commit type과 일관 |
| scope | `scope:frontend` / `scope:backend` / `scope:api-types` / `scope:tests` / `scope:docs` / `scope:infra` | commit scope 매핑. 두 영역 이상이면 생략 |
| domain | `domain:auth` / `domain:workspace` / `domain:project` / `domain:document` / `domain:graph` / `domain:validation` / `domain:notification` | 제품 모델. 현재는 `scope:backend` issue 위주. 단일 도메인일 때만 |

### 닫기

PR 본문에 `closes #N`으로 자동 닫는다. 수동 close는 *작업 폐기* 등 예외만.