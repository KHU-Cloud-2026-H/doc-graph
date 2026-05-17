# Windows default shell을 PowerShell → sh로 강제 (Unix는 영향 없음 — 기본이 sh)
set windows-shell := ["sh", "-cu"]

# .env(평문 + dotenvx 암호화) + .env.local(개인 시크릿)을 child process에 주입.
# --overload: .env.local이 .env의 빈 placeholder를 override (last-wins).
# .env.keys로 암호화 항목 복호화.
# env 파일 경로는 justfile_directory() 절대경로 → recipe가 cwd를 자유롭게 옮겨도 안전.
dotenv-run := 'dotenvx run --strict --overload -f "' + (justfile_directory() / ".env") + '" -f "' + (justfile_directory() / ".env.local") + '" --'

# 인수·시스템 테스트 stack의 compose 파일 묶음 (cross-platform 위해 -f 명시).
test-compose-files := '-f "' + (justfile_directory() / "docker-compose.yml") + '" -f "' + (justfile_directory() / "docker-compose.test.yml") + '"'

# 기본 — 사용 가능한 recipe 목록
default:
    @just --list

# 백엔드 로컬 개발. extra_profile 인자로 추가 profile 활성 (예: just bootRun live → ngrok 같이).
bootRun extra_profile="":
    COMPOSE_PROFILES="{{extra_profile}}" {{dotenv-run}} docker compose up -d --wait
    cd apps/backend && {{dotenv-run}} sh ./gradlew bootRun

# 백엔드 테스트 — bootRun과 동일하게 dotenvx로 .env + .env.local 주입.
# fixture 결정성은 TestPropertySource가 env 위 precedence로 강제.
test-unit:
    cd apps/backend && {{dotenv-run}} sh ./gradlew unitTest

test-slice:
    cd apps/backend && {{dotenv-run}} sh ./gradlew sliceTest

test-component:
    cd apps/backend && {{dotenv-run}} sh ./gradlew componentTest

test-all:
    cd apps/backend && {{dotenv-run}} sh ./gradlew test

test-class class:
    cd apps/backend && {{dotenv-run}} sh ./gradlew test --tests {{class}}

# 인수 stack — postgres + backend(acceptance profile) + 외부 시스템 stub.
# mode: mock (default, wiremock) | live (실제 Notion·OpenAI + ngrok).
# 회원가입 UI 실제 흐름 시연은 live + OAuth2 backend 통합 완료가 전제.
compose-up mode="mock":
    COMPOSE_PROFILES=backend,{{mode}} {{dotenv-run}} docker compose up

compose-down:
    {{dotenv-run}} docker compose down

# 시스템 테스트 — application alive·부팅·env wiring. 로컬 compose 자동 띄움.
test-system:
    #!/usr/bin/env sh
    set -e
    export COMPOSE_PROJECT_NAME=doc-graph-test
    export COMPOSE_PROFILES=backend
    trap '{{dotenv-run}} docker compose {{test-compose-files}} down -v' EXIT
    {{dotenv-run}} docker compose {{test-compose-files}} up -d --build --wait
    cd "{{justfile_directory()}}/tests" && {{dotenv-run}} uv run pytest system

# 인수 테스트 — env=local-mock(default) / local-live / remote-live.
test-acceptance env="local-mock":
    #!/usr/bin/env sh
    set -e
    export COMPOSE_PROJECT_NAME=doc-graph-test
    case "{{env}}" in
      local-mock)
        export COMPOSE_PROFILES=backend,mock
        trap '{{dotenv-run}} docker compose {{test-compose-files}} down -v' EXIT
        {{dotenv-run}} sh -c 'AI_OPENAI_BASE_URL=$MOCK_AI_OPENAI_BASE_URL NOTION_AUTHORIZATION_URI=$MOCK_NOTION_AUTHORIZATION_URI NOTION_TOKEN_URI=$MOCK_NOTION_TOKEN_URI docker compose {{test-compose-files}} up -d --build --wait'
        ;;
      local-live)
        export COMPOSE_PROFILES=backend,live
        trap '{{dotenv-run}} docker compose {{test-compose-files}} down -v' EXIT
        {{dotenv-run}} docker compose {{test-compose-files}} up -d --build --wait
        ;;
      remote-live) ;;
      *) echo "unknown env: {{env}}" >&2; exit 1 ;;
    esac
    cd "{{justfile_directory()}}/tests" && {{dotenv-run}} uv run pytest acceptance --env={{env}}

# OpenAPI JSON dump — bootRun으로 backend 띄운 후 /v3/api-docs endpoint curl로 spec 캡쳐.
# springdoc-openapi-gradle-plugin 1.9.0이 spec fetch 시 4xx response 누락하는 bug 회피
# (starter는 정상 응답이라 runtime mode 직접 활용).
openapi-dump:
    #!/usr/bin/env sh
    set -e
    BOOT_PID=""
    cleanup() {
        set +e
        [ -n "$BOOT_PID" ] && kill $BOOT_PID 2>/dev/null
        wait $BOOT_PID 2>/dev/null
        {{dotenv-run}} docker compose down 2>/dev/null
        return 0
    }
    trap cleanup EXIT
    {{dotenv-run}} docker compose up -d --wait
    (cd apps/backend && {{dotenv-run}} sh ./gradlew bootRun --no-daemon > /tmp/openapi-boot.log 2>&1) &
    BOOT_PID=$!
    for i in $(seq 1 90); do
        if curl -s -f http://localhost:8080/api/v3/api-docs -o packages/api-types/openapi.json 2>/dev/null; then
            echo "spec captured ($(wc -c < packages/api-types/openapi.json) bytes)"
            exit 0
        fi
        sleep 1
    done
    echo "boot timeout — see /tmp/openapi-boot.log" >&2
    exit 1

# OpenAPI → TypeScript 타입 생성 (packages/api-types/openapi.json 입력)
gen-types: openapi-dump
    npm run generate:types

# Redoc HTML 미리보기 빌드 (apps/docs workspace, 산출물은 apps/docs/dist/index.html)
gen-redoc: openapi-dump
    npm --workspace apps/docs run build

# 팀 공유 시크릿 암호화 후 .env에 저장
encrypt-secret key value:
    dotenvx set {{key}} {{value}}