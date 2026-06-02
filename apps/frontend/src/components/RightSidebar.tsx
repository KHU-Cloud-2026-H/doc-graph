import { X, AlertTriangle, CheckCircle, FileText, ExternalLink, RefreshCw, ChevronUp, ChevronDown, Lightbulb, EyeOff } from "lucide-react";
import { Link, useParams } from "react-router-dom";
import { useState } from "react";
import type { DocumentNode } from "../store";
import { formatRelativeTime } from "../lib/timeAgo";
import { useIgnoreConflict, useUnignoreConflict, useApproveConflict } from "../hooks/useConflictActions";

interface RightSidebarProps {
  document: DocumentNode;
  isOpen: boolean;
  onClose: () => void;
}

export const RightSidebar = ({ document: doc, isOpen, onClose }: RightSidebarProps) => {
  const { workspaceId, projectId } = useParams();
  const [collapsed, setCollapsed] = useState<Record<string, boolean>>({});
  // 무시됨 섹션 전체 토글
  const [showIgnoredSection, setShowIgnoredSection] = useState(true);
  // 무시됨 카드 개별 펼치기 (기본: 접힘 → Set에 있으면 펼쳐진 상태)
  const [openedIgnored, setOpenedIgnored] = useState<Set<string>>(new Set());

  const ignore = useIgnoreConflict();
  const unignore = useUnignoreConflict();
  const approve = useApproveConflict();

  const toggleCollapsed = (id: string) => {
    setCollapsed(prev => ({ ...prev, [id]: !prev[id] }));
  };

  // mock: 마지막 검증 시각. 실제 연동 시 ValidationTaskResponse.createdAt 또는
  // ConflictFindingResponse.detectedAt 기반으로 계산.
  // TODO(API): 백엔드에 문서 단위 재검증 트리거 API 추가 필요 (현재는 /projects/{id}/sync 프로젝트 전체만).
  const lastValidatedAt = new Date(Date.now() - 2 * 60 * 1000);
  const validatedAgo = formatRelativeTime(lastValidatedAt);

  const activeIssues = (doc.issues ?? []).filter(i => i.status !== 'IGNORED');
  const ignoredIssues = (doc.issues ?? []).filter(i => i.status === 'IGNORED');
  const issueCount = activeIssues.length;
  const hasIssues = issueCount > 0;

  return (
    <aside
      className={`w-[400px] bg-white border-l border-slate-200 h-screen hidden lg:flex flex-col shrink-0 shadow-[-4px_0_15px_-5px_rgba(0,0,0,0.05)] z-30 font-sans
        absolute right-0 top-0
        transform transition-transform duration-[225ms] ease-out
        ${isOpen ? 'translate-x-0' : 'translate-x-full'}`}
    >
      {/* 헤더: 아이콘 + 타이틀 + 이슈 배지 + 검증 시각 버튼 + 닫기 */}
      <div className="border-b border-slate-200 flex items-center gap-2 h-14 px-4 shrink-0">
        {hasIssues ? (
          <AlertTriangle className="w-4 h-4 text-red-600" />
        ) : (
          <CheckCircle className="w-4 h-4 text-green-600" />
        )}
        <h3 className="font-bold text-sm text-slate-900 tracking-tight">INTEGRITY ISSUES</h3>
        {hasIssues && (
          <span className="bg-red-600 text-white rounded-full w-5 h-5 flex items-center justify-center text-[11px] font-bold leading-none">
            {issueCount}
          </span>
        )}
        {/* 검증 시각 버튼: X 버튼과 비슷한 사이즈, 호버 효과
            TODO(API): 클릭 시 실제 재검증 트리거. 현재 백엔드에 문서 단위 재검증 API 없음. */}
        <button
          className="ml-auto flex items-center gap-1 px-2 py-1 rounded text-xs text-slate-500 hover:bg-slate-100 hover:text-slate-900 transition-colors"
          title="다시 검증하기"
        >
          <RefreshCw className="w-3 h-3" />
          <span>{validatedAgo} 갱신</span>
        </button>
        <button
          onClick={onClose}
          className="p-1 rounded-full hover:bg-slate-100 text-slate-400 hover:text-slate-900 transition-colors flex items-center justify-center"
          aria-label="닫기"
        >
          <X className="w-5 h-5" />
        </button>
      </div>

      {/* 본문: 이슈 있으면 카드 리스트, 없으면 빈 상태 */}
      {hasIssues || ignoredIssues.length > 0 ? (
        <div className="flex-1 overflow-y-auto p-4 space-y-4">
          {/* ── ACTIVE 이슈 카드들 ── */}
          {activeIssues.map((issue) => {
            const isCollapsed = !!collapsed[issue.id];
            return (
              <div key={issue.id} className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
                {/* 카드 헤더 */}
                <button
                  onClick={() => toggleCollapsed(issue.id)}
                  className="w-full px-4 py-3 flex items-center justify-between hover:bg-slate-50 transition-colors"
                >
                  <div className="flex items-center gap-2">
                    <AlertTriangle className="w-[18px] h-[18px] text-amber-600" />
                    <span className="text-[15px] font-semibold text-slate-900">{issue.title}</span>
                  </div>
                  <ChevronUp className={`w-5 h-5 text-slate-400 transition-transform ${isCollapsed ? 'rotate-180' : ''}`} />
                </button>

                {!isCollapsed && (
                  <div className="px-4 pb-4 space-y-4">
                    {/* 충돌 원인 파악 */}
                    <div className="text-sm leading-relaxed">
                      <span className="text-slate-900 font-semibold inline-block">충돌 원인 파악</span>
                      <span className="inline-block ml-2 px-1.5 py-0.5 text-[11px] font-bold text-white bg-gradient-to-r from-blue-600 to-purple-600 leading-none align-middle rounded-full relative -top-[1px]">AI</span>
                      <p className="text-slate-600 mt-1">{issue.rationale}</p>
                    </div>

                    {/* 소스 문서 + 검증 기준 + 액션 버튼 */}
                    <div className="space-y-2.5">
                      <div className="border border-yellow-200 rounded-lg overflow-hidden">
                        <Link
                          to={`/w/${workspaceId}/p/${projectId}/docs/${issue.sourceDocumentId}`}
                          className="bg-yellow-50 px-3 py-2 flex items-center justify-between gap-2 border-b border-yellow-100 hover:bg-yellow-100 transition-colors"
                          title="원본 문서로 이동"
                        >
                          <div className="flex items-center gap-1.5 min-w-0">
                            <FileText className="w-4 h-4 text-yellow-700 shrink-0" />
                            <span className="text-[13px] font-bold text-yellow-900 truncate">{issue.sourceDocumentTitle}</span>
                          </div>
                          <ExternalLink className="w-3.5 h-3.5 text-yellow-700 shrink-0" />
                        </Link>
                        <div className="bg-white p-3">
                          <p className="text-xs text-slate-700 leading-relaxed font-medium">{issue.sourceBlockText}</p>
                        </div>
                      </div>
                      <p className="text-xs text-slate-500 leading-relaxed px-0.5">💡 {issue.validationCriterion}</p>
                      <div className="flex items-center gap-1.5">
                        <button className="px-2.5 py-1 text-[11px] font-medium text-slate-500 hover:text-slate-700 hover:bg-slate-100 border border-slate-200 rounded-md transition-colors">
                          이 문서와의 연결 무시
                        </button>
                        <Link
                          to={`/w/${workspaceId}/p/${projectId}/graph`}
                          className="px-2.5 py-1 text-[11px] font-medium text-slate-500 hover:text-slate-700 hover:bg-slate-100 border border-slate-200 rounded-md transition-colors flex items-center gap-1"
                        >
                          Dependency Graph
                          <ExternalLink className="w-2.5 h-2.5" />
                        </Link>
                      </div>
                    </div>

                    {/* DocGraph AI 제안 + diff */}
                    <div className="space-y-2">
                      <div className="flex items-center gap-1.5">
                        <Lightbulb className="w-[18px] h-[18px] text-blue-600" />
                        <span className="text-[13px] font-semibold text-slate-900">DocGraph AI 제안</span>
                      </div>
                      <div className="rounded-lg border border-slate-200 overflow-hidden text-[13px]">
                        <div className="flex items-start gap-2 bg-red-50 text-slate-700 p-2.5 border-b border-slate-200">
                          <span className="bg-red-600 text-white rounded-full flex items-center justify-center w-[18px] h-[18px] text-[12px] font-bold shrink-0 mt-0.5">-</span>
                          <span className="whitespace-pre-wrap leading-relaxed">{issue.currentText}</span>
                        </div>
                        <div className="flex items-start gap-2 bg-green-50 text-slate-700 p-2.5">
                          <span className="bg-green-600 text-white rounded-full flex items-center justify-center w-[18px] h-[18px] text-[12px] font-bold shrink-0 mt-0.5">+</span>
                          <span className="whitespace-pre-wrap leading-relaxed">{issue.newText}</span>
                        </div>
                      </div>
                    </div>

                    {/* 하단 액션 버튼
                        TODO(API): "제안 적용하기" = POST /conflicts/{cid}/findings/{fid}/approve
                                   "무시하기" = POST /conflicts/{id}/ignore */}
                    <div className="flex items-center justify-end gap-2 pt-1">
                      <button
                        onClick={() => issue.conflictId !== undefined && ignore.mutate(issue.conflictId)}
                        disabled={issue.conflictId === undefined || ignore.isPending}
                        className="px-3 py-1.5 text-xs font-semibold text-slate-600 hover:bg-slate-100 rounded transition-colors disabled:opacity-40"
                      >
                        무시하기
                      </button>
                      <button
                        onClick={() => issue.conflictId !== undefined && issue.findingId !== undefined && approve.mutate({ conflictId: issue.conflictId, findingId: issue.findingId })}
                        disabled={issue.conflictId === undefined || issue.findingId === undefined || approve.isPending}
                        className="px-3 py-1.5 text-xs font-bold text-white bg-blue-600 hover:bg-blue-700 rounded shadow-sm transition-colors disabled:opacity-40"
                      >
                        제안 적용하기
                      </button>
                    </div>
                  </div>
                )}
              </div>
            );
          })}

          {/* ── 빈 상태 (ACTIVE 없고 IGNORED만 있을 때) ── */}
          {activeIssues.length === 0 && ignoredIssues.length > 0 && (
            <div className="flex flex-col items-center justify-center py-8 text-center">
              <CheckCircle className="w-12 h-12 text-slate-300 mb-3" strokeWidth={1.5} />
              <p className="text-[15px] text-slate-500 leading-relaxed">
                미해소 충돌이 없습니다.
              </p>
            </div>
          )}

          {/* ── 무시됨 충돌 섹션 ── */}
          {ignoredIssues.length > 0 && (
            <div className="border-t border-slate-100 pt-2 -mx-4 px-4">
              <button
                onClick={() => setShowIgnoredSection(!showIgnoredSection)}
                className="w-full flex items-center justify-between px-1 py-2 text-xs text-slate-400 hover:text-slate-600 hover:bg-slate-50 rounded-md transition-colors"
              >
                <span>무시됨 충돌 {ignoredIssues.length}건</span>
                <ChevronDown className={`w-3.5 h-3.5 transition-transform ${showIgnoredSection ? 'rotate-180' : ''}`} />
              </button>

              {showIgnoredSection && (
                <div className="space-y-3 mt-2">
                  {ignoredIssues.map((issue) => {
                    const isExpanded = openedIgnored.has(issue.id);
                    return (
                      <div key={issue.id} className="rounded-xl border border-slate-200 overflow-hidden opacity-70">
                        {/* 무시됨 카드 헤더 */}
                        <button
                          onClick={() => setOpenedIgnored(prev => {
                            const next = new Set(prev);
                            next.has(issue.id) ? next.delete(issue.id) : next.add(issue.id);
                            return next;
                          })}
                          className="w-full px-4 py-3 flex items-center justify-between hover:bg-slate-50 transition-colors bg-slate-50/50"
                        >
                          <div className="flex items-center gap-2">
                            <EyeOff className="w-[18px] h-[18px] text-slate-400" />
                            <span className="text-[14px] font-medium text-slate-500">{issue.title}</span>
                          </div>
                          <div className="flex items-center gap-2">
                            <span className="text-[10px] font-semibold text-slate-400 bg-slate-100 px-1.5 py-0.5 rounded-full">무시됨</span>
                            <ChevronUp className={`w-4 h-4 text-slate-300 transition-transform ${!isExpanded ? 'rotate-180' : ''}`} />
                          </div>
                        </button>

                        {isExpanded && (
                          <div className="px-4 pb-4 space-y-3 bg-white">
                            <p className="text-xs text-slate-500 leading-relaxed pt-3">{issue.rationale}</p>
                            <p className="text-xs text-slate-400 px-0.5">💡 {issue.validationCriterion}</p>
                            <div className="flex items-center justify-end pt-1">
                              <button
                                onClick={() => issue.conflictId !== undefined && unignore.mutate(issue.conflictId)}
                                disabled={issue.conflictId === undefined || unignore.isPending}
                                className="px-3 py-1.5 text-xs font-semibold text-slate-500 hover:text-slate-700 hover:bg-slate-100 border border-slate-200 rounded transition-colors disabled:opacity-40"
                              >
                                무시 취소
                              </button>
                            </div>
                          </div>
                        )}
                      </div>
                    );
                  })}
                </div>
              )}
            </div>
          )}
        </div>
      ) : (
        /* 빈 상태: 정합성 이슈 없음 */
        <div className="flex-1 flex flex-col items-center justify-center px-8 text-center">
          <CheckCircle className="w-16 h-16 text-slate-300 mb-4" strokeWidth={1.5} />
          <p className="text-[18px] text-slate-500 leading-relaxed whitespace-pre-line">
            {'이 문서에서는 정합성 충돌이\n발견되지 않았습니다!'}
          </p>
        </div>
      )}
    </aside>
  );
};
