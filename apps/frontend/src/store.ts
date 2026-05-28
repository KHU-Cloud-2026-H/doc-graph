import { create } from 'zustand';

// TODO: 이 파일의 mock 데이터는 API 연동 시 전부 교체됩니다.
// workspace, documents, notifications 모두 실제 API 응답으로 대체 예정

// ID 매핑 테이블 (슬러그 → number, 디버깅용)
// 'planning' → 1, 'prd' → 2, 'srs' → 3, 'policy' → 4, 'rbac' → 5,
// 'meetings' → 6, 'meet-sprint1' → 7, 'meet-urgent' → 8, 'meet-daily' → 9, 'meet-retro' → 10,
// 'design' → 11, 'design-arch' → 12, 'design-api-auth' → 13, 'design-api-attendance' → 14, 'design-erd' → 15,
// 'qa' → 16, 'qa-strategy' → 17, 'sit' → 18, 'unit-test' → 19, 'uat' → 20,
// 'frontend' → 21, 'fe-ui' → 22, 'fe-state' → 23, 'fe-routing' → 24, 'error-mapping' → 25,
// 'api-attendance' → 26 (sourceDocumentId only; no matching DocumentNode id)

// 정합성 충돌 finding 1건.
// 실제 백엔드 API의 ConflictFindingResponse + EdgeResponse 일부를 합친 mock 표현.
// 주의: 본 mock에서 issue가 달린 문서가 "보고 있는 문서(=target)"이고,
//      sourceDocumentId가 가리키는 문서가 "충돌 상대(=source)"이다.
//      이전 mock은 필드명이 정반대였으나 API 매핑 혼동 방지를 위해 정정됨.
export interface IntegrityIssue {
  id: string;
  title: string;                 // API: ConflictResponse.title (충돌 사안 한 줄 요약 제목)
  rationale: string;             // API: ConflictFindingResponse.rationale (충돌 원인 AI 판정)
  sourceDocumentId: number;      // API: ConflictResponse.sourceDocumentId (충돌의 근거가 된 다른 문서)
  sourceDocumentTitle: string;   // API 미제공, mock 임시 (실연동 시 별도 조회 필요)
  sourceBlockText: string;       // API: sourceBlockIds[]로 조회한 source 블록 본문 (mock 임시)
  currentText: string;           // mock 한정: 보고 있는 문서(=target)의 충돌 블록 현재 본문
  newText: string;               // API: ConflictFindingResponse.newText (해당 블록 수정 후 본문)
  validationCriterion: string;   // API: EdgeResponse.validationCriterion (자연어 검증 기준)
}

export interface DocumentNode {
  id: number;
  title: string;
  emoji?: string;
  children?: DocumentNode[];
  hasIssue?: boolean;
  issues?: IntegrityIssue[];
  contentHtml?: string;
}

interface Workspace {
  id: number;
  name: string;
}

interface Project {
  id: number;
  name: string;
  workspaceId: number;
  notionRootPageId?: string;
  // TODO(API): ProjectSummary·ProjectDetail에 notionRootPageIcon(IconResponse) 포함 시 교체
  notionRootPageEmoji?: string | null;
}

interface AppState {
  workspace: string;
  isSidebarOpen: boolean;
  toggleSidebar: () => void;
  documents: DocumentNode[];
  workspaces: Workspace[];
  projects: Project[];
  currentWorkspaceId: number | null;
  currentProjectId: number | null;
  setCurrentProject: (projectId: number) => void;
}

export const useAppStore = create<AppState>((set) => ({
  workspace: '엔터프라이즈 근태관리 B2B SaaS 프로젝트',
  isSidebarOpen: true,
  toggleSidebar: () => set((state) => ({ isSidebarOpen: !state.isSidebarOpen })),
  documents: [
    {
      id: 0,
      title: '프로젝트',
      emoji: '🏠',
      hasIssue: false,
      issues: [],
      contentHtml: `
        <div class="space-y-2">
          <a href="/w/sample-workspace/docs/1" class="flex items-center gap-2 p-2 hover:bg-slate-50 rounded border border-slate-200 text-blue-600 transition-colors">💡 기획 (Planning)</a>
          <a href="/w/sample-workspace/docs/6" class="flex items-center gap-2 p-2 hover:bg-slate-50 rounded border border-slate-200 text-blue-600 transition-colors">🗓️ 회의록 (Meeting Notes)</a>
          <a href="/w/sample-workspace/docs/11" class="flex items-center gap-2 p-2 hover:bg-slate-50 rounded border border-slate-200 text-blue-600 transition-colors">💻 설계 (Design)</a>
          <a href="/w/sample-workspace/docs/16" class="flex items-center gap-2 p-2 hover:bg-slate-50 rounded border border-slate-200 text-blue-600 transition-colors">🔍 QA 및 테스트 (QA/Testing)</a>
          <a href="/w/sample-workspace/docs/21" class="flex items-center gap-2 p-2 hover:bg-slate-50 rounded border border-slate-200 text-blue-600 transition-colors">🎨 프론트엔드/UI 명세 (Frontend/UI Specification)</a>
        </div>
      `,
      children: [
        {
          id: 1,
          title: '기획 (Planning)',
          emoji: '💡',
          contentHtml: `
        <h1 class="text-3xl font-bold text-slate-900 mb-8 outline-none tracking-tight">기획 (Planning)</h1>
        <p class="mb-6">기획 단계의 문서들은 시스템이 해결하고자 하는 비즈니스 문제와 사용자 가치를 정의한다. 이는 이해관계자 간의 합의를 도출하고 개발의 방향성을 설정하는 기준 문서이다.</p>
        <div class="space-y-2">
          <a href="/w/sample-workspace/docs/prd" class="flex items-center gap-2 p-2 hover:bg-slate-50 rounded border border-slate-200 text-blue-600 transition-colors">📄 제품 요구사항 명세서 (PRD)</a>
          <a href="/w/sample-workspace/docs/srs" class="flex items-center gap-2 p-2 hover:bg-slate-50 rounded border border-slate-200 text-blue-600 transition-colors">📄 시스템 기능 및 비기능 요구사항 명세서 (SRS)</a>
          <a href="/w/sample-workspace/docs/policy" class="flex items-center gap-2 p-2 hover:bg-slate-50 rounded border border-slate-200 text-blue-600 transition-colors">📄 B2B 기업 근태 및 휴가 정책 정의서</a>
          <a href="/w/sample-workspace/docs/rbac" class="flex items-center gap-2 p-2 hover:bg-slate-50 rounded border border-slate-200 text-blue-600 transition-colors">📄 권한(Role) 및 접근 제어 정의서</a>
        </div>
      `,
          children: [
            {
              id: 2,
              title: '제품 요구사항 명세서 (PRD)',
              emoji: '📄',
              hasIssue: true,
              issues: [
                {
                  id: 'err-4',
                  title: '모바일 플랫폼 테스트 범위 불일치',
                  rationale: '문서 1의 핵심 요구사항에서는 모바일 환경에서 iOS 및 Android 양쪽 모두를 지원해야 한다고 기재되어 있으나, QA 테스트 전략에서는 오직 Android 환경에서만 테스트를 수행하도록 제한하여 충돌이 발생함.',
                  sourceDocumentId: 17,
                  sourceDocumentTitle: 'QA 테스트 전략 및 품질 보증 계획',
                  sourceBlockText: '크로스 플랫폼 호환성을 유지하기 위해 QA 팀은 프론트엔드 렌더링 및 모바일 앱 동작 검증을 오직 Android OS 12 이상 환경에서만 수행하도록 정책을 제한하여 테스트 리소스를 최적화한다.',
                  currentText: '사용자는 모바일 기기(iOS 및 Android) 및 웹 브라우저를 통해 출근 및 퇴근 상태를 기록할 수 있어야 한다.',
                  newText: '사용자는 모바일 기기(Android OS 우선 지원) 및 웹 브라우저를 통해 출근 및 퇴근 상태를 기록할 수 있어야 한다.',
                  validationCriterion: '요구사항 명세서와 QA 문서 관계이므로 자동 연결되었습니다.'
                },
                {
                  id: 'err-9',
                  title: '비밀번호 최소 길이 기준 불일치',
                  rationale: '보안 요구사항에서는 비밀번호 강도를 10자리 이상으로 엄격히 통제하나, 클라이언트 에러 명세의 8자리 제한 안내 메시지와 모순됨.',
                  sourceDocumentId: 25,
                  sourceDocumentTitle: '클라이언트 에러 코드 매핑',
                  sourceBlockText: '422 PASSWORD_TOO_SHORT 에러 발생 시 사용자에게 노출되는 안내 메시지: \'보안 기준 미달: 비밀번호는 최소 8자리 이상이어야 합니다.\'',
                  currentText: '영문 대소문자, 숫자, 특수문자를 모두 포함한 10자리 이상',
                  newText: '영문 대소문자, 숫자, 특수문자를 모두 포함한 최소 8자리 이상',
                  validationCriterion: '보안 요구사항과 클라이언트 에러 메시지 관계이므로 자동 연결되었습니다.'
                }
              ],
              contentHtml: `
            <h2 class="text-xl font-semibold text-slate-900 mt-10 mb-4">1. 개요 및 비즈니스 목적</h2>
            <p>본 제품 요구사항 명세서는 B2B 엔터프라이즈 고객을 위한 클라우드 기반 모바일/웹 근태관리 SaaS 'WorkSync'의 핵심 기능 및 가치를 정의한다. 현대의 기업 환경은 재택근무와 거점 오피스 활용 등 유연 근무제가 확산됨에 따라 기존의 온프레미스 기반 수기 출퇴근 기록 방식의 한계를 직면하고 있다. WorkSync 시스템은 조직 내 관리자가 직원의 활동을 실시간으로 모니터링하고, 위치 기반 출퇴근 기록을 검증하며, 급여 시스템과 연동될 수 있는 신뢰성 높은 리포트를 제공하는 것을 목적으로 한다.</p>

            <h2 class="text-xl font-semibold text-slate-900 mt-10 mb-4">2. 목표 사용자(Target Audience)</h2>
            <p>시스템은 세 가지 주요 사용자 페르소나를 대상으로 설계된다. 첫째, 자신의 근태와 휴가를 관리하는 일반 사원(Employee)이다. 둘째, 소속 팀원의 근태 현황을 파악하고 휴가를 승인하는 부서장(Department Head)이다. 셋째, 전사적 근태 규정을 설정하고 통계를 추출하는 인사 담당자(HR Manager)이다. 각 페르소나는 시스템에 로그인하여 자신의 역할에 맞는 대시보드와 기능을 제공받는다.</p>

            <h2 class="text-xl font-semibold text-slate-900 mt-10 mb-4">3. 핵심 기능 요구사항 (Core Workflows)</h2>
            <ul class="list-disc pl-6 space-y-3">
              <li><strong>다중 매체 출퇴근 기록:</strong> <span class="bg-red-50 text-red-700 px-1 rounded">사용자는 모바일 기기(iOS 및 Android) 및 웹 브라우저를 통해 출근 및 퇴근 상태를 기록할 수 있어야 한다.</span> 이 기록은 조작을 방지하기 위해 서버 측 시간을 기준으로 저장된다.</li>
              <li><strong>위치 기반 검증 시스템:</strong> 부정 출퇴근 기록을 원천적으로 차단하기 위해, 모바일 앱을 통한 출근 기록 시 반드시 디바이스의 GPS 좌표를 수집해야 한다. 수집된 좌표는 사전 등록된 회사 근무지 반경 100m 이내인지 검증되어야 한다.</li>
              <li><strong>휴가 관리 워크플로우:</strong> 사용자는 잔여 연차를 확인하고 휴가를 신청할 수 있으며, 전자 결재 시스템을 통해 승인 과정을 거친다.</li>
              <li><strong>자동화 리포팅:</strong> 관리자를 위한 일간, 주간, 월간 근태 통계 리포트가 시스템에 의해 자동 생성되며, 엑셀 및 CSV 형태로 추출 가능해야 한다.</li>
            </ul>

            <h2 class="text-xl font-semibold text-slate-900 mt-10 mb-4">4. 보안 및 비기능 요구사항</h2>
            <ul class="list-disc pl-6 space-y-3">
              <li><strong>엔터프라이즈 비밀번호 보안 정책:</strong> B2B 고객의 엄격한 정보보안 감사 기준을 충족하기 위해, 모든 사용자의 비밀번호는 '<span class="bg-red-50 text-red-700 px-1 rounded">영문 대소문자, 숫자, 특수문자를 모두 포함한 10자리 이상</span>'의 조합으로만 설정할 수 있도록 시스템에서 강제한다.</li>
              <li><strong>가용성 및 호환성:</strong> 시스템은 99.9%의 월간 가동 시간을 보장해야 하며, 최소 500MB 이상의 RAM을 보유한 디바이스에서 원활하게 동작해야 한다.</li>
            </ul>
          `
            },
            {
              id: 3,
              title: '시스템 기능 및 비기능 요구사항 명세서 (SRS)',
              emoji: '📄',
              hasIssue: true,
              issues: [
                {
                  id: 'err-7',
                  title: '시간 데이터 포맷 불일치',
                  rationale: '비기능 요구사항은 시간 표기를 ISO 8601 문자열 포맷으로 강제하나, SIT 시나리오에서는 숫자형 Unix Timestamp로 전송하고 있어 포맷 충돌이 일어남.',
                  sourceDocumentId: 18,
                  sourceDocumentTitle: '시스템 통합 테스트 (SIT) 시나리오',
                  sourceBlockText: 'TC-INT-01 시나리오에서는 출근 기록 전송 시 checkInTime 필드를 Unix Timestamp(예: 1682812800 같은 숫자형 Long Type)로 페이로드에 삽입하여 전송한다.',
                  currentText: 'ISO 8601 표준 포맷(예: YYYY-MM-DDTHH:mm:ss+09:00)을 엄격히 따른다.',
                  newText: 'Unix Timestamp(Long Type) 포맷을 엄격히 따른다.',
                  validationCriterion: '시스템 요구사항과 통합 테스트 시나리오 관계이므로 자동 연결되었습니다.'
                }
              ],
              contentHtml: `
            <p><strong>문서 정보:</strong> WorkSync v1.0 SRS (작성자: 기획팀)</p>
            <p class="mb-4">이 문서는 WorkSync 애플리케이션 개발에 필요한 구체적인 기능적, 비기능적 요구사항을 나열한다. 명확하게 정의된 요구사항은 모호성을 제거하고 개발 주기의 지연을 방지하는 핵심 수단이다.</p>
            <div class="overflow-x-auto">
              <table class="min-w-full border-collapse border border-slate-200">
                <thead>
                  <tr class="bg-slate-50">
                    <th class="border border-slate-200 px-4 py-2 text-left">요구사항 ID</th>
                    <th class="border border-slate-200 px-4 py-2 text-left">카테고리</th>
                    <th class="border border-slate-200 px-4 py-2 text-left">상세 내용</th>
                    <th class="border border-slate-200 px-4 py-2 text-left">우선순위</th>
                  </tr>
                </thead>
                <tbody>
                  <tr>
                    <td class="border border-slate-200 px-4 py-2">REQ-F-01</td>
                    <td class="border border-slate-200 px-4 py-2">인증/인가</td>
                    <td class="border border-slate-200 px-4 py-2">모든 사용자는 부여된 사내 이메일 계정 및 초기 비밀번호를 사용하여 시스템에 로그인해야 한다.</td>
                    <td class="border border-slate-200 px-4 py-2">High</td>
                  </tr>
                  <tr>
                    <td class="border border-slate-200 px-4 py-2">REQ-N-01</td>
                    <td class="border border-slate-200 px-4 py-2">데이터 표준</td>
                    <td class="border border-slate-200 px-4 py-2"><span class="bg-red-50 text-red-700 px-1 rounded">ISO 8601 표준 포맷(예: YYYY-MM-DDTHH:mm:ss+09:00)을 엄격히 따른다.</span></td>
                    <td class="border border-slate-200 px-4 py-2">High</td>
                  </tr>
                </tbody>
              </table>
            </div>
          `
            },
            {
              id: 4,
              title: 'B2B 기업 근태 및 휴가 정책 정의서',
              emoji: '📄',
              hasIssue: true,
              issues: [
                {
                  id: 'err-8',
                  title: '출근 시간 기준 불일치',
                  rationale: '09:10:59까지의 기록을 정상 출근으로 유예 인정한다고 정책을 명시하였으나 단위 테스트에서는 09:00:01 기록부터 즉각 지각(LATE) 처리되도록 하드코딩되어 모순됨.',
                  sourceDocumentId: 19,
                  sourceDocumentTitle: '백엔드 비즈니스 로직 단위 테스트',
                  sourceBlockText: '단위 테스트 케이스에서 09:00:01 입력 시 즉각 LATE 상태로 전이되는지를 경계값으로 검증하도록 하드코딩되어 있다.',
                  currentText: "'오전 9시 10분 59초'까지 시스템에 기록된 출근 데이터는 정상 출근(PRESENT)으로 인정한다. 정확히 09:11:00의 기록부터 지각(LATE) 상태로 산정되어 리포트에 기록된다.",
                  newText: "'오전 9시 00분 00초'까지 시스템에 기록된 출근 데이터는 정상 출근(PRESENT)으로 인정한다. 09시 정각 이후의 기록부터 즉시 지각(LATE) 상태로 산정되어 리포트에 기록된다.",
                  validationCriterion: '비즈니스 정책과 단위 테스트 검증 규칙 관계이므로 자동 연결되었습니다.'
                }
              ],
              contentHtml: `
            <p>본 문서는 고객사 시스템에 공통으로 적용될 표준 비즈니스 룰을 정의한다.</p>

            <h2 class="text-xl font-semibold text-slate-900 mt-10 mb-4">1. 근무 시간 및 지각/조퇴 기준</h2>
            <p>기업의 정규 근무 시간은 09:00부터 18:00까지이며, 중식 시간을 포함한 1시간의 휴게 시간이 기본으로 산정된다. 특히 지각 기준의 경우, 현실적인 출근 혼잡도와 엘리베이터 대기 시간 등을 고려하여 유예 시간을 둔다. 따라서 <span class="bg-red-50 text-red-700 px-1 rounded">'오전 9시 10분 59초'까지 시스템에 기록된 출근 데이터는 정상 출근(PRESENT)으로 인정한다. 정확히 09:11:00의 기록부터 지각(LATE) 상태로 산정되어 리포트에 기록된다.</span> 퇴근의 경우 18:00 이전의 모든 퇴근 기록은 조퇴로 간주된다.</p>

            <h2 class="text-xl font-semibold text-slate-900 mt-10 mb-4">2. 휴가 발생 및 차감 정책</h2>
            <ul class="list-disc pl-6 space-y-2">
              <li>휴가의 발생 및 소멸은 근로기준법에 따라 입사일 또는 기업별 회계연도(1월 1일)를 기준으로 자동 계산되어 부여된다.</li>
              <li>휴가 사용의 유연성을 극대화하기 위해 최소 사용 단위는 <strong>'반차(0.5일)'</strong> 단위로 지원한다.</li>
              <li>이에 따라 사원의 잔여 연차 및 사용 연차 일수는 정수가 아닌 소수점 첫째 자리까지 데이터베이스에 저장 및 관리되어야 한다.</li>
            </ul>
          `
            },
            {
              id: 5,
              title: '권한(Role) 및 접근 제어 정의서',
              emoji: '📄',
              hasIssue: true,
              issues: [
                {
                  id: 'err-10',
                  title: '근태 기록 접근 제어 미구현',
                  rationale: 'RBAC 매트릭스에서는 일반 사원이 타인의 근태 기록을 열람하는 것을 완벽히 차단해야 한다고 명확히 요구함. 그러나 Attendance 다건 조회 API는 토큰의 유효성만 확인할 뿐 필터링 로직이 누락됨.',
                  sourceDocumentId: 14,
                  sourceDocumentTitle: 'API 명세서 (Attendance 도메인)',
                  sourceBlockText: 'Attendance 다건 조회 API(GET /api/v1/attendance)는 JWT 토큰의 유효성만 확인할 뿐, Role 기반 필터링 로직이 명세에 포함되어 있지 않다.',
                  currentText: '일반 사원. 타인의 근태 기록 열람 및 조회는 시스템적으로 완벽히 차단(Deny)되어야 함.',
                  newText: '일반 사원. (현재 v1.0 기준 동일 부서/팀원 간의 열람은 허용됨)',
                  validationCriterion: '권한 정책과 API 인가 로직 관계이므로 자동 연결되었습니다.'
                }
              ],
              contentHtml: `
            <p><strong>Role-Based Access Control (RBAC) 아키텍처</strong></p>
            <p class="mb-4">시스템 내 모든 데이터 열람 및 수정 행위는 철저한 권한 기반 인가 시스템을 거친다. 외부 B2B 고객을 대상으로 하므로 JWT 토큰 내 클레임을 기반으로 수직적, 수평적 권한 분리를 수행한다.</p>

            <div class="overflow-x-auto mt-6">
              <table class="min-w-full border-collapse border border-slate-200 text-sm">
                <thead>
                  <tr class="bg-slate-50">
                    <th class="border border-slate-200 px-4 py-2 text-left">역할 (Role)</th>
                    <th class="border border-slate-200 px-4 py-2 text-left">비고 (Scope)</th>
                  </tr>
                </thead>
                <tbody>
                  <tr>
                    <td class="border border-slate-200 px-4 py-2 font-bold">SYSTEM_ADMIN</td>
                    <td class="border border-slate-200 px-4 py-2">기업의 최상위 시스템 관리자</td>
                  </tr>
                  <tr>
                    <td class="border border-slate-200 px-4 py-2 font-bold">EMPLOYEE</td>
                    <td class="border border-slate-200 px-4 py-2"><span class="bg-red-50 text-red-700 px-1 rounded">일반 사원. 타인의 근태 기록 열람 및 조회는 시스템적으로 완벽히 차단(Deny)되어야 함.</span></td>
                  </tr>
                </tbody>
              </table>
            </div>
          `
            }
          ]
        },
        {
          id: 6,
          title: '회의록 (Meeting Notes)',
          emoji: '🗓️',
          contentHtml: `
        <h1 class="text-3xl font-bold text-slate-900 mb-8 outline-none tracking-tight">회의록 (Meeting Notes)</h1>
        <p class="mb-6">회의록은 애자일 팀의 커뮤니케이션 맥락과 의사결정의 이력을 추적하는 데 핵심적인 역할을 한다. 개발 과정에서 발생하는 정책의 변경과 타협점은 이 문서에 고스란히 담긴다.</p>
        <div class="space-y-2">
          <a href="/w/sample-workspace/docs/meet-sprint1" class="flex items-center gap-2 p-2 hover:bg-slate-50 rounded border border-slate-200 text-blue-600 transition-colors">📄 주간 스프린트 1 플래닝 회의록</a>
          <a href="/w/sample-workspace/docs/meet-urgent" class="flex items-center gap-2 p-2 hover:bg-slate-50 rounded border border-slate-200 text-blue-600 transition-colors">📄 스프린트 1 진행 중 긴급 정책 변경 회의</a>
          <a href="/w/sample-workspace/docs/meet-daily" class="flex items-center gap-2 p-2 hover:bg-slate-50 rounded border border-slate-200 text-blue-600 transition-colors">📄 데일리 스크럼 (Sprint 1 후반부)</a>
          <a href="/w/sample-workspace/docs/meet-retro" class="flex items-center gap-2 p-2 hover:bg-slate-50 rounded border border-slate-200 text-blue-600 transition-colors">📄 Sprint 1 회고 및 Sprint 2 플래닝</a>
        </div>
      `,
          children: [
            {
              id: 7,
              title: '주간 스프린트 1 플래닝 회의록',
              emoji: '📄',
              contentHtml: `
            <ul class="space-y-2">
              <li><strong>일시:</strong> 2026-04-02 10:00 KST</li>
              <li><strong>회의 안건:</strong> WorkSync v1.0 첫 번째 스프린트(Sprint 1)의 백로그 정리 및 스토리 포인트 할당</li>
            </ul>
            <h2 class="text-xl font-semibold text-slate-900 mt-10 mb-4">논의 및 결정 사항</h2>
            <ul class="list-disc pl-6 space-y-2">
              <li><strong>아키텍처 및 코어 개발:</strong> 이번 스프린트의 최우선 목표는 JWT 기반의 인증/인가 구조와 출퇴근을 기록하는 코어 API의 완성이다.</li>
              <li><strong>데이터베이스 설계 규약:</strong> 객체 식별자는 대소문자 혼용에 따른 오류를 방지하기 위해 엄격한 스네이크 케이스(snake_case) 네이밍 컨벤션을 따르기로 합의한다.</li>
            </ul>
          `
            },
            {
              id: 8,
              title: '스프린트 1 진행 중 긴급 정책 변경 회의',
              emoji: '📄',
              hasIssue: false, // Target document (issue belongs to API Spec)
              contentHtml: `
            <ul class="space-y-2">
              <li><strong>일시:</strong> 2026-04-09 14:00 KST</li>
              <li><strong>주요 안건:</strong> 모바일 앱 GPS 기반 출퇴근 위치 수집 기능 유지 여부 논의</li>
            </ul>
            <h2 class="text-xl font-semibold text-slate-900 mt-10 mb-4">이슈 배경 및 결정 사항</h2>
            <p>최근 B2B 고객사 사전 인터뷰 및 당사 법무팀의 강도 높은 검토 결과, 사원의 개인 스마트폰에서 GPS 위도 및 경도 데이터를 수집하여 서버에 저장하는 행위가 개인정보보호법에 강하게 위배될 소지가 있음이 확인되었다.</p>
            <p class="mt-4 font-bold text-red-600">시스템 런칭 시 발생할 수 있는 리스크를 차단하기 위해 'GPS 기반 위치 수집 및 근무지 반경 검증 기능'을 이번 릴리즈에서 완전히 삭제(Drop)하기로 합의하였다.</p>
          `
            },
            {
              id: 9,
              title: '데일리 스크럼 (Sprint 1 후반부)',
              emoji: '📄',
              contentHtml: `
            <ul class="space-y-2">
              <li><strong>일시:</strong> 2026-04-12 09:30 KST</li>
            </ul>
            <h2 class="text-xl font-semibold text-slate-900 mt-10 mb-4">블로커 (Blocker) 공유 및 정책 합의</h2>
            <p><strong>PM 결정 사항:</strong> 일정 지연을 막기 위해 '오프라인 출퇴근 기록 보정(Sync) 기능'은 Phase 2(다음 분기) 마일스톤으로 연기한다. 개발팀 혼선을 막기 위해 프론트엔드 화면 마크업 및 UI 컴포넌트 목록에서 오프라인 보정 버튼과 관련 사유 입력 폼을 완전히 제외하고 기능 없이 깔끔하게 릴리즈하기로 결정한다.</p>
          `
            },
            {
              id: 10,
              title: 'Sprint 1 회고 및 Sprint 2 플래닝',
              emoji: '📄',
              contentHtml: `
            <ul class="space-y-2">
              <li><strong>일시:</strong> 2026-04-15 13:00 KST</li>
            </ul>
            <h2 class="text-xl font-semibold text-slate-900 mt-10 mb-4">스프린트 2 안건: 휴가 결재 워크플로우 축소 개편</h2>
            <p>파일럿 고객사 피드백 결과, 2단계 결재 프로세스는 실무에서 너무 무겁고 HR 부서의 업무 과중을 유발한다는 지적이 팽배했다.</p>
            <p class="mt-2"><strong>최종 결정:</strong> 휴가 승인 프로세스를 대폭 단축한다. 사원의 휴가 신청은 '부서장의 1차 승인'만 이루어지면 즉각적으로 최종 처리(APPROVED) 상태로 전이되도록 시스템을 개편한다. 따라서 기존 아키텍처에 존재했던 HR 승인 대기 단계는 시스템에서 전면 폐지된다.</p>
          `
            }
          ]
        },
        {
          id: 11,
          title: '설계 (Design)',
          emoji: '💻',
          contentHtml: `
        <h1 class="text-3xl font-bold text-slate-900 mb-8 outline-none tracking-tight">설계 (Design)</h1>
        <div class="space-y-2">
          <a href="/w/sample-workspace/docs/design-arch" class="flex items-center gap-2 p-2 hover:bg-slate-50 rounded border border-slate-200 text-blue-600 transition-colors">📄 시스템 아키텍처 및 상태 머신 설계서</a>
          <a href="/w/sample-workspace/docs/design-api-auth" class="flex items-center gap-2 p-2 hover:bg-slate-50 rounded border border-slate-200 text-blue-600 transition-colors">📄 API 명세서 (User & Auth 도메인)</a>
          <a href="/w/sample-workspace/docs/design-api-attendance" class="flex items-center gap-2 p-2 hover:bg-slate-50 rounded border border-slate-200 text-blue-600 transition-colors">📄 API 명세서 (Attendance 도메인)</a>
          <a href="/w/sample-workspace/docs/design-erd" class="flex items-center gap-2 p-2 hover:bg-slate-50 rounded border border-slate-200 text-blue-600 transition-colors">📄 데이터베이스 스키마 설계서 (ERD 물리 모델)</a>
        </div>
      `,
          children: [
            {
              id: 12,
              title: '시스템 아키텍처 및 상태 머신 설계서',
              emoji: '📄',
              hasIssue: true,
              issues: [
                {
                  id: 'err-2',
                  title: '휴가 결재 프로세스 단축 누락',
                  rationale: '회고 및 플래닝 과정에서 휴가 결재 프로세스를 부서장 1차 승인만으로 단축하고 HR 2차 승인 단계를 폐지하기로 결정했으나 비즈니스 상태 전이도에 여전히 PENDING_HR 단계가 존재함.',
                  sourceDocumentId: 10,
                  sourceDocumentTitle: 'Sprint 1 회고 및 Sprint 2 플래닝',
                  sourceBlockText: '고객 피드백을 반영하여 휴가 결재 프로세스에서 인사부서(HR) 2차 최종 승인 단계를 전면 폐지하고, 부서장 1차 승인만으로 휴가 결재가 완료되도록 단축하기로 결정함.',
                  currentText: '- PENDING_HR: 부서장 승인 후 인사부서(HR)의 최종 결재를 대기 중인 상태.',
                  newText: '(해당 줄 삭제됨)',
                  validationCriterion: '회의록 결정사항이 시스템 아키텍처에 반영되어야 하는 관계입니다.'
                }
              ],
              contentHtml: `
            <h2 class="text-xl font-semibold text-slate-900 mt-10 mb-4">1. MSA 기반 클라우드 아키텍처 개요</h2>
            <p>WorkSync는 유연성과 확장성을 보장하기 위해 AWS 클라우드 환경에서 마이크로서비스 아키텍처(MSA)를 지향하여 설계된다.</p>

            <h2 class="text-xl font-semibold text-slate-900 mt-10 mb-4">3. 비즈니스 상태 전이도 (State Transition): 휴가 승인 프로세스</h2>
            <ul class="list-disc pl-6 space-y-2">
              <li>DRAFT: 사용자가 휴가 일자를 선택하고 작성 중인 임시 상태.</li>
              <li>PENDING_MANAGER: 작성이 완료되어 부서장(팀장)의 결재를 대기 중인 상태.</li>
              <li><span class="bg-red-50 text-red-700 px-1 rounded">- PENDING_HR: 부서장 승인 후 인사부서(HR)의 최종 결재를 대기 중인 상태.</span></li>
              <li>APPROVED: 모든 결재가 완료되어 시스템에 휴가가 확정된 상태.</li>
            </ul>
          `
            },
            {
              id: 13,
              title: 'API 명세서 (User & Auth 도메인)',
              emoji: '📄',
              hasIssue: true,
              issues: [
                {
                  id: 'err-5',
                  title: '사원 식별자 형식 불일치',
                  rationale: 'DB 스키마는 정수형 타입에 스네이크 케이스를 적용한 employee_no (INTEGER)로 설계되었으나, Auth API 명세서에서는 카멜케이스 기반의 문자열 타입인 employeeId 로 응답을 정의함.',
                  sourceDocumentId: 15,
                  sourceDocumentTitle: '데이터베이스 스키마 설계서',
                  sourceBlockText: '사원 고유 식별자는 employee_no 컬럼으로 정의하며, 데이터 타입은 INTEGER, 스네이크 케이스 네이밍 컨벤션을 적용한다.',
                  currentText: '"employeeId": "EMP-928374",  // 문자열 형태로 추상화된 사원 식별자',
                  newText: '"employee_no": 928374,  // 정수형 사원 고유 번호',
                  validationCriterion: 'DB 스키마와 API 명세 관계이므로 자동 연결되었습니다.'
                }
              ],
              contentHtml: `
            <p><strong>기본 규약</strong><br/>Base URL: https://api.worksync.com/v1/auth</p>
            <h2 class="text-xl font-semibold text-slate-900 mt-10 mb-4">1. 시스템 로그인 및 Access Token 발급</h2>
            <pre class="bg-slate-100 p-4 rounded text-sm mt-4">
{
  "accessToken": "eyJhbGciOiJIUzI1...",
  <span class="bg-red-200 text-red-900 px-1">"employeeId": "EMP-928374",  // 문자열 형태로 추상화된 사원 식별자</span>
  "role": "EMPLOYEE",
  "expiresIn": 3600
}</pre>
          `
            },
            {
              id: 14,
              title: 'API 명세서 (Attendance 도메인)',
              emoji: '📄',
              hasIssue: true,
              issues: [
                {
                  id: 'err-1',
                  title: 'GPS 위치 수집 기능 불일치',
                  rationale: '회의록에서는 위치 수집 기능을 완전히 삭제하기로 합의하였으나 API 명세서의 Request Body에는 여전히 latitude와 longitude가 필수 파라미터로 남아있음.',
                  sourceDocumentId: 8,
                  sourceDocumentTitle: '긴급 정책 변경 회의',
                  sourceBlockText: '개인정보보호법 위반 우려와 노조 반발 리스크를 고려하여, 모바일 앱의 GPS 기반 위치 수집 기능을 이번 릴리즈에서 완전히 폐지하기로 개발팀 전원이 합의함.',
                  currentText: '"latitude": 37.5665,\n"longitude": 126.9780,',
                  newText: '(삭제)',
                  validationCriterion: '회의록 결정사항이 API 명세에 반영되어야 하는 관계입니다.'
                },
                {
                  id: 'err-6',
                  title: '출근 상태 표현 형식 불일치',
                  rationale: '백엔드는 attendance_status라는 명칭의 열거형 문자열을 반환하도록 규정하나 프론트엔드 상태 관리는 단순 boolean인 isCheckedIn 속성으로 정의하여 충돌함.',
                  sourceDocumentId: 23,
                  sourceDocumentTitle: '프론트엔드 상태 관리 명세',
                  sourceBlockText: '출근 여부 상태는 isCheckedIn이라는 boolean 타입 속성으로 정의하며, true는 출근, false는 미출근을 의미한다.',
                  currentText: '"attendance_status": "PRESENT",',
                  newText: '"isCheckedIn": true,',
                  validationCriterion: 'API 명세와 프론트엔드 상태 정의 관계이므로 자동 연결되었습니다.'
                }
              ],
              contentHtml: `
            <h2 class="text-xl font-semibold text-slate-900 mt-6 mb-4">1. 일일 출근 기록 등록 (Check-in)</h2>
            <p>엔드포인트: POST /api/v1/attendance/check-in</p>
            <pre class="bg-slate-100 p-4 rounded text-sm mt-4">
{
  "checkInTime": "2026-04-30T08:55:00+09:00",
  <span class="bg-red-200 text-red-900 px-1">"latitude": 37.5665,
  "longitude": 126.9780,</span>
  "device_id": "DEV_883A"
}</pre>
            <h3 class="text-lg font-semibold mt-6 mb-2">Response (201 Created)</h3>
            <pre class="bg-slate-100 p-4 rounded text-sm mt-4">
{
  "record_id": 1029,
  <span class="bg-red-200 text-red-900 px-1">"attendance_status": "PRESENT",</span>
  "message": "성공적으로 출근 처리되었습니다."
}</pre>
          `
            },
            {
              id: 15,
              title: '데이터베이스 스키마 설계서 (ERD 물리 모델)',
              emoji: '📄',
              contentHtml: `
            <p>본 문서는 근태 관리 시스템의 ERD를 바탕으로 도출된 데이터베이스 테이블 정의서이다. 모든 식별자는 스네이크 케이스(snake_case)를 적용한다.</p>
            <h2 class="text-xl font-semibold text-slate-900 mt-6 mb-4">Table 1: employees (사원 마스터 테이블)</h2>
            <p>employee_no (INTEGER) | fname | lname | email | department_id</p>
          `
            }
          ]
        },
        {
          id: 16,
          title: 'QA 및 테스트 (QA/Testing)',
          emoji: '🔍',
          contentHtml: `
        <h1 class="text-3xl font-bold text-slate-900 mb-8 outline-none tracking-tight">QA 및 테스트 (QA/Testing)</h1>
        <div class="space-y-2">
          <a href="/w/sample-workspace/docs/qa-strategy" class="flex items-center gap-2 p-2 hover:bg-slate-50 rounded border border-slate-200 text-blue-600 transition-colors">📄 QA 테스트 전략 및 품질 보증 계획</a>
          <a href="/w/sample-workspace/docs/sit" class="flex items-center gap-2 p-2 hover:bg-slate-50 rounded border border-slate-200 text-blue-600 transition-colors">📄 시스템 통합 테스트 (SIT) 시나리오</a>
          <a href="/w/sample-workspace/docs/unit-test" class="flex items-center gap-2 p-2 hover:bg-slate-50 rounded border border-slate-200 text-blue-600 transition-colors">📄 백엔드 비즈니스 로직 단위 테스트(Unit Test) 체크리스트</a>
          <a href="/w/sample-workspace/docs/uat" class="flex items-center gap-2 p-2 hover:bg-slate-50 rounded border border-slate-200 text-blue-600 transition-colors">📄 사용자 인수 테스트 (UAT) 시나리오</a>
        </div>
      `,
          children: [
            {
              id: 17,
              title: 'QA 테스트 전략 및 품질 보증 계획',
              emoji: '📄',
              contentHtml: `
            <h2 class="text-xl font-semibold text-slate-900 mt-10 mb-4">3. 디바이스 테스트 정책</h2>
            <p>크로스 플랫폼 호환성을 유지하기 위해 QA 팀은 프론트엔드 렌더링 및 모바일 앱 동작 검증을 오직 Android OS 12 이상 환경에서만 수행하도록 정책을 제한하여 테스트 리소스를 최적화한다.</p>
          `
            },
            {
              id: 18,
              title: '시스템 통합 테스트 (SIT) 시나리오',
              emoji: '📄',
              contentHtml: `
            <p>백엔드와 프론트엔드가 결합된 환경에서 API 요청과 데이터베이스 적재가 올바르게 수행되는지 검증한다.</p>
            <p>TC-INT-01: 출근 기록 전송. POST 본문에 {"checkInTime": 1682812800} 형식의 Unix Timestamp 숫자 데이터를 삽입하여 전송한다.</p>
          `
            },
            {
              id: 19,
              title: '백엔드 비즈니스 로직 단위 테스트(Unit Test) 체크리스트',
              emoji: '📄',
              contentHtml: `
            <p>Spring Boot 환경에서 서비스 계층 검증을 위한 JUnit 기반 체크리스트이다.</p>
            <ul class="list-disc pl-6 space-y-2 mt-4">
              <li>테스트 입력 파라미터: 09:00:01 -> 예상 결과: 09시 00분 00초를 단 1초라도 초과할 경우 즉각적으로 지각(LATE) 상태로 전이되는지 검증.</li>
            </ul>
          `
            },
            {
              id: 20,
              title: '사용자 인수 테스트 (UAT) 시나리오',
              emoji: '📄',
              contentHtml: `
            <p>UAT는 시스템이 최종 사용자의 실무 요구를 충족하는지 확인하기 위해 고객사 대표자가 직접 수행하는 테스트이다.</p>
          `
            }
          ]
        },
        {
          id: 21,
          title: '프론트엔드/UI 명세 (Frontend/UI Specification)',
          emoji: '🎨',
          contentHtml: `
        <h1 class="text-3xl font-bold text-slate-900 mb-8 outline-none tracking-tight">프론트엔드/UI 명세 (Frontend/UI Specification)</h1>
        <div class="space-y-2">
          <a href="/w/sample-workspace/docs/fe-ui" class="flex items-center gap-2 p-2 hover:bg-slate-50 rounded border border-slate-200 text-blue-600 transition-colors">📄 UI/UX 컴포넌트 아키텍처 및 마크업 명세서</a>
          <a href="/w/sample-workspace/docs/fe-state" class="flex items-center gap-2 p-2 hover:bg-slate-50 rounded border border-slate-200 text-blue-600 transition-colors">📄 프론트엔드 상태 관리 (State Management) 명세</a>
          <a href="/w/sample-workspace/docs/fe-routing" class="flex items-center gap-2 p-2 hover:bg-slate-50 rounded border border-slate-200 text-blue-600 transition-colors">📄 화면 흐름도 (Screen Flow) 및 네비게이션 라우팅 정책</a>
          <a href="/w/sample-workspace/docs/error-mapping" class="flex items-center gap-2 p-2 hover:bg-slate-50 rounded border border-slate-200 text-blue-600 transition-colors">📄 클라이언트 에러 코드 매핑 및 화면 노출 명세서</a>
        </div>
      `,
          children: [
            {
              id: 22,
              title: 'UI/UX 컴포넌트 아키텍처 및 마크업 명세서',
              emoji: '📄',
              hasIssue: true,
              issues: [
                {
                  id: 'err-3',
                  title: '오프라인 동기화 기능 제외 미반영',
                  rationale: '데일리 스크럼에서 오프라인 출퇴근 기록 보정(Sync) 기능을 제외하기로 결정하였으나 컴포넌트 설계서에는 액션 버튼과 모달이 남아있음.',
                  sourceDocumentId: 9,
                  sourceDocumentTitle: '데일리 스크럼 (Sprint 1 후반부)',
                  sourceBlockText: '오프라인 출퇴근 기록 보정(Sync) 기능은 로직 복잡도가 높아 이번 릴리즈에서 제외하고 다음 마일스톤으로 이관하기로 프론트엔드 리드와 PM이 합의함.',
                  currentText: '버튼 3: 네트워크가 단절되었던 상황에서 로컬 스토리지에 적재된 출퇴근 데이터를 서버로 일괄 전송하기 위한 액션 버튼.',
                  newText: '(삭제됨)',
                  validationCriterion: '데일리 스크럼 결정사항이 UI 컴포넌트 설계에 반영되어야 하는 관계입니다.'
                }
              ],
              contentHtml: `
            <p>React 기반 클라이언트 애플리케이션의 재사용 가능한 UI 컴포넌트 목록과 렌더링 정책을 정의한다.</p>
            <ul class="list-disc pl-6 space-y-2 mt-4">
              <li>AttendanceWidget: 메인 대시보드 상단 중앙에 고정 배치되는 핵심 위젯.</li>
              <li>버튼 1: [출근하기] - 클릭 시 POST /check-in API 트리거.</li>
              <li><span class="bg-red-50 text-red-700 px-1 rounded">버튼 3: 네트워크가 단절되었던 상황에서 로컬 스토리지에 적재된 출퇴근 데이터를 서버로 일괄 전송하기 위한 액션 버튼.</span></li>
            </ul>
          `
            },
            {
              id: 23,
              title: '프론트엔드 상태 관리 (State Management) 명세',
              emoji: '📄',
              contentHtml: `
            <pre class="bg-slate-100 p-4 rounded text-sm mt-4">
interface AttendanceState {
  isCheckedIn: boolean;
  lastCheckInTime: string | null;
}
interface LeaveState {
  status: 'DRAFT' | 'PENDING_MANAGER' | 'PENDING_HR' | 'APPROVED' | 'REJECTED';
}</pre>
          `
            },
            {
              id: 24,
              title: '화면 흐름도 (Screen Flow) 및 네비게이션 라우팅 정책',
              emoji: '📄',
              contentHtml: `<p>사용자의 권한과 인증 상태에 따른 페이지 접근 제어 및 라우팅 흐름도이다.</p>`
            },
            {
              id: 25,
              title: '클라이언트 에러 코드 매핑 및 화면 노출 명세서',
              emoji: '📄',
              contentHtml: `
            <p>클라이언트의 에러 메시지 매핑 테이블입니다.</p>
            <p class="mt-4"><span class="bg-red-50 text-red-700 px-1 rounded">422 PASSWORD_TOO_SHORT: "보안 기준 미달: 비밀번호는 최소 8자리 이상이어야 합니다."</span></p>
          `
            }
          ]
        }
      ]
    },
  ],
  workspaces: [{ id: 1, name: '엔터프라이즈 근태관리 B2B SaaS 프로젝트' }],
  projects: [{ id: 1, name: 'WorkSync 도입 프로젝트', workspaceId: 1, notionRootPageEmoji: '🏠' }],
  currentWorkspaceId: 1,
  currentProjectId: 1,
  setCurrentProject: (projectId) => set(() => ({ currentProjectId: projectId })),
}));
