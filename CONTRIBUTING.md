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