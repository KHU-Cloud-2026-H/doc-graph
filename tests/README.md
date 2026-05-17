# tests

시스템 테스트 (`system/`) — backend 부팅·alive smoke. 인수 절차 선결.
인수 테스트 (`acceptance/`) — UC happy path 시나리오.

## 실행

```bash
just test-system                  # 시스템
just test-acceptance              # 인수 (default local-mock)
just test-acceptance local-live
just test-acceptance remote-live
```

Docker 데몬이 떠 있어야 한다. 첫 실행 시 backend image 빌드로 시간이 소요된다.