import { useNavigate } from "react-router-dom";
import { useState } from "react";
import { Plus, Users, Layers, FileText, Clock, AlertTriangle } from "lucide-react";
import { TopAppBar } from '../components/TopAppBar';
import { InboxList, inboxActiveCount, type InboxFilter } from '../components/InboxList';
import { formatRelativeTime } from '../lib/timeAgo';

// ── WorkspaceSummaryMock ──────────────────────────────────────────────────────
// TODO(API): GET /workspaces → WorkspaceSummary[] 로 교체
//            (id, name, memberCount, projectCount, documentCount,
//             unresolvedConflictCount, lastNotionChangedAt 모두 API 제공)
interface WorkspaceSummaryMock {
  id: number;
  name: string;
  memberCount: number;
  projectCount: number;
  documentCount: number;
  unresolvedConflictCount: number;
  lastNotionChangedAt: string | null;
}

const MOCK_WORKSPACE_SUMMARIES: WorkspaceSummaryMock[] = [
  {
    id: 1,
    name: '엔터프라이즈 근태관리 B2B SaaS 프로젝트',
    memberCount: 7,
    projectCount: 1,
    documentCount: 25,
    unresolvedConflictCount: 2,
    lastNotionChangedAt: new Date(Date.now() - 5 * 60 * 60 * 1000).toISOString(),
  },
];

export const WorkspaceSelection = () => {
  const navigate = useNavigate();
  const [inboxFilter, setInboxFilter] = useState<InboxFilter>('ACTIVE');

  return (
    <div className="bg-slate-50 text-slate-900 min-h-screen flex flex-col font-sans">
      <TopAppBar centerLabel="artboi@khu.ac.kr" />

      <main className="flex-1 w-full max-w-[1200px] mx-auto px-6 py-10">
        <div className="flex gap-16">
          {/* Left: Workspaces */}
          <div className="flex-1 max-w-[calc(100%-430px)]">
            <div className="flex justify-between items-end mb-4">
              <div className="flex items-center">
                <h1 className="text-3xl font-bold text-slate-900">Workspaces</h1>
                <span className="bg-slate-200 text-slate-600 text-xs py-0.5 px-2 rounded-full font-medium ml-2">
                  {MOCK_WORKSPACE_SUMMARIES.length}
                </span>
              </div>
              <button className="flex items-center gap-2 bg-blue-600 text-white px-5 py-2.5 rounded hover:bg-blue-700 transition-colors font-medium text-sm shadow-sm">
                <Plus className="w-4 h-4" />
                새 워크스페이스 연동
              </button>
            </div>
            <p className="text-slate-500 text-sm mb-6">Notion에서 연동된 모든 워크스페이스 목록입니다.</p>
            <div className="space-y-3">
              {MOCK_WORKSPACE_SUMMARIES.map((ws) => (
                <div
                  key={ws.id}
                  onClick={() => navigate(`/w/${ws.id}`)}
                  className="bg-white border border-slate-200 rounded-xl p-5 hover:shadow-md hover:border-blue-300 transition-all cursor-pointer relative"
                >
                  {/* 충돌 알약 */}
                  {ws.unresolvedConflictCount > 0 && (
                    <span className="absolute top-4 right-4 inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full bg-red-100 text-red-600 text-xs font-semibold">
                      <AlertTriangle className="w-3 h-3" />
                      충돌 {ws.unresolvedConflictCount}건
                    </span>
                  )}

                  {/* 아이콘 + [이름 / 통계] */}
                  <div className="flex items-center gap-4">
                    <div className="bg-blue-100 text-blue-700 rounded-lg w-14 h-14 flex items-center justify-center text-2xl font-bold shrink-0">
                      {ws.name[0]}
                    </div>
                    <div className="flex flex-col gap-1.5 flex-1 min-w-0 pr-28">
                      <span className="text-2xl font-bold text-slate-900 truncate">{ws.name}</span>
                      <div className="flex items-center gap-2.5 flex-wrap text-xs text-slate-400 font-medium">
                        <span className="flex items-center gap-1">
                          <Users className="w-3.5 h-3.5" />
                          {ws.memberCount}명의 멤버
                        </span>
                        <span className="text-slate-200 select-none">·</span>
                        <span className="flex items-center gap-1">
                          <Layers className="w-3.5 h-3.5" />
                          {ws.projectCount}개의 프로젝트
                        </span>
                        <span className="text-slate-200 select-none">·</span>
                        <span className="flex items-center gap-1">
                          <FileText className="w-3.5 h-3.5" />
                          {ws.documentCount}개의 페이지
                        </span>
                        <span className="text-slate-200 select-none">·</span>
                        <span className="flex items-center gap-1">
                          <Clock className="w-3.5 h-3.5" />
                          {ws.lastNotionChangedAt ? formatRelativeTime(ws.lastNotionChangedAt) : '동기화 전'}
                        </span>
                      </div>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Right: Inbox */}
          <div className="w-[400px] shrink-0">
            <div className="flex items-center justify-between mb-2">
              <div className="flex items-center gap-2">
                <h1 className="text-3xl font-bold text-slate-900">Inbox</h1>
                <span className="bg-red-600 text-white text-xs font-bold px-1.5 py-0.5 rounded-full">
                  {inboxActiveCount}
                </span>
              </div>
              {/* 필터 pills */}
              <div className="flex items-center gap-1.5">
                {(
                  [
                    { value: 'ACTIVE', label: '미해소' },
                    { value: 'IGNORED', label: '무시됨' },
                    { value: 'ALL', label: '전체' },
                  ] as const
                ).map((f) => (
                  <button
                    key={f.value}
                    onClick={() => setInboxFilter(f.value)}
                    className={`px-2.5 py-0.5 rounded-full text-xs font-semibold transition-colors ${inboxFilter === f.value
                        ? f.value === 'ACTIVE'
                          ? 'bg-red-100 text-red-700'
                          : f.value === 'IGNORED'
                            ? 'bg-emerald-100 text-emerald-700'
                            : 'bg-slate-100 text-slate-700'
                        : 'text-slate-400 hover:text-slate-600 hover:bg-slate-50'
                      }`}
                  >
                    {f.label}
                  </button>
                ))}
              </div>
            </div>
            <p className="text-sm text-slate-500 mb-4">
              내가 담당하는 모든 프로젝트에서 발생한<br />
              정합성 충돌 알림입니다.
            </p>
            <div className="bg-white border border-slate-200 rounded-xl overflow-hidden">
              <InboxList filter={inboxFilter} />
            </div>
          </div>
        </div>
      </main>

      <footer className="w-full mt-auto bg-slate-100 border-t border-slate-200">
        <div className="max-w-[1200px] mx-auto py-8 px-6 flex flex-col md:flex-row justify-between items-center">
          <div className="font-bold text-slate-500 text-sm mb-4 md:mb-0">© 2026 DocGraph.</div>
          <div className="flex gap-6">
            <a className="text-slate-500 hover:text-blue-600 text-sm transition-colors duration-200" href="#">개인정보 처리방침</a>
            <a className="text-slate-500 hover:text-blue-600 text-sm transition-colors duration-200" href="#">이용약관</a>
            <a className="text-slate-500 hover:text-blue-600 text-sm transition-colors duration-200" href="#">고객센터</a>
          </div>
        </div>
      </footer>
    </div>
  );
};
