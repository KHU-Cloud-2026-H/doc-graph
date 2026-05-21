import { X, AlertTriangle, FileText, ExternalLink, RefreshCw, ChevronUp, Lightbulb } from "lucide-react";
import { Link } from "react-router-dom";
import { useState } from "react";
import type { DocumentNode } from "../store";

export const RightSidebar = ({ document: doc, onClose }: { document: DocumentNode, onClose: () => void }) => {
  // 각 충돌 카드의 접힘 상태. key는 issue.id.
  const [collapsed, setCollapsed] = useState<Record<string, boolean>>({});

  const toggleCollapsed = (id: string) => {
    setCollapsed(prev => ({ ...prev, [id]: !prev[id] }));
  };

  return (
    <aside className="w-[320px] bg-white border-l border-slate-200 h-screen hidden lg:flex flex-col shrink-0 shadow-[-4px_0_15px_-5px_rgba(0,0,0,0.05)] z-30 font-sans">
      {/* 헤더: INTEGRITY ISSUES 타이틀 + 이슈 수 배지 + 닫기 */}
      <div className="border-b border-slate-200 flex items-center gap-2 h-14 px-4 shrink-0">
        <AlertTriangle className="w-4 h-4 text-red-600" />
        <h3 className="font-bold text-sm text-slate-900 tracking-tight">INTEGRITY ISSUES</h3>
        {doc.issues && doc.issues.length > 0 && (
          <span className="bg-red-600 text-white rounded-full w-5 h-5 flex items-center justify-center text-[11px] font-bold">
            {doc.issues.length}
          </span>
        )}
        <button
          onClick={onClose}
          className="ml-auto p-1 rounded-full hover:bg-slate-100 text-slate-400 hover:text-slate-900 transition-colors flex items-center justify-center"
          aria-label="닫기"
        >
          <X className="w-5 h-5" />
        </button>
      </div>

      {/* 검증 상태 영역: "N분 전 마지막으로 검증됨" + 새로고침 트리거 */}
      {/* TODO(API): 실제 검증 시각은 ValidationTaskResponse.createdAt 또는 ConflictFindingResponse.detectedAt 기반으로 도출.
                    재검증 트리거 API는 현재 백엔드에 문서 단위로는 없음 (project 단위 POST /projects/{id}/sync만 존재).
                    백엔드에 문서 단위 재검증 API 추가 요청 후보. */}
      <div className="px-4 py-2.5 border-b border-slate-200 flex items-center gap-2 text-xs text-slate-500">
        <button className="flex items-center gap-2 hover:text-slate-900 transition-colors">
          <RefreshCw className="w-3.5 h-3.5" />
          <span>2분 전 마지막으로 검증됨</span>
        </button>
      </div>

      {/* 충돌 카드 리스트 */}
      <div className="flex-1 overflow-y-auto p-4 space-y-4">
        {doc.issues?.map((issue, index) => {
          const isCollapsed = !!collapsed[issue.id];
          return (
            <div key={issue.id} className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
              {/* 카드 헤더: "정합성 충돌 의심 #N" + 접기/펼치기 토글 */}
              <button
                onClick={() => toggleCollapsed(issue.id)}
                className="w-full px-4 py-3 flex items-center justify-between hover:bg-slate-50 transition-colors"
              >
                <div className="flex items-center gap-2">
                  <AlertTriangle className="w-4 h-4 text-amber-600" />
                  <span className="text-sm font-semibold text-slate-900">정합성 충돌 의심 #{index + 1}</span>
                </div>
                <ChevronUp className={`w-5 h-5 text-slate-400 transition-transform ${isCollapsed ? 'rotate-180' : ''}`} />
              </button>

              {!isCollapsed && (
                <div className="px-4 pb-4 space-y-4">
                  {/* 충돌 원인 파악 [AI] */}
                  <div className="text-xs leading-relaxed">
                    <span className="text-slate-900 font-semibold inline-block">충돌 원인 파악</span>
                    <span className="inline-block ml-2 px-1.5 py-0.5 text-[9px] font-bold text-white bg-gradient-to-r from-blue-600 to-purple-600 leading-none align-middle rounded-full relative -top-[1px]">AI</span>
                    <p className="text-slate-600 mt-1">{issue.rationale}</p>
                  </div>

                  {/* 상충 문서 카드 (GraphRightSidebar 스타일 차용, 좁은 폭에 맞게 조정) */}
                  <div className="border border-red-200 rounded-lg overflow-hidden shadow-sm">
                    <div className="bg-red-50 px-3 py-2 flex items-center justify-between gap-2 border-b border-red-100">
                      <div className="flex items-center gap-1.5 min-w-0">
                        <FileText className="w-3.5 h-3.5 text-red-600 shrink-0" />
                        <span className="text-xs font-bold text-red-900 truncate">{issue.sourceDocumentTitle}</span>
                      </div>
                      <Link
                        to={`/w/sample-workspace/docs/${issue.sourceDocumentId}`}
                        className="text-red-600 hover:text-red-800 shrink-0"
                        title="원본 문서로 이동"
                      >
                        <ExternalLink className="w-3.5 h-3.5" />
                      </Link>
                    </div>
                    <div className="bg-white p-3">
                      <p className="text-xs text-slate-700 leading-relaxed font-medium">
                        {issue.sourceBlockText}
                      </p>
                    </div>
                  </div>

                  {/* validationCriterion (자연어 검증 기준) + 작은 액션 버튼들 */}
                  <div className="text-xs text-slate-500 space-y-2">
                    <p className="leading-relaxed">💡 {issue.validationCriterion}</p>
                    <div className="flex items-center gap-1.5 flex-wrap">
                      {/* TODO(API): "연결 무시" = edge 자체를 ignore 처리. 현재 API에는 edge ignore endpoint 없음 (conflict ignore만 있음: POST /conflicts/{id}/ignore). 백엔드 협의 필요. */}
                      <button className="px-2 py-1 text-[11px] font-medium text-slate-600 bg-slate-100 hover:bg-slate-200 rounded transition-colors">
                        연결 무시
                      </button>
                      <Link
                        to={`/w/sample-workspace/graph`}
                        className="px-2 py-1 text-[11px] font-medium text-slate-600 bg-slate-100 hover:bg-slate-200 rounded transition-colors flex items-center gap-1"
                      >
                        Dependency Graph
                        <ExternalLink className="w-3 h-3" />
                      </Link>
                    </div>
                  </div>

                  {/* DocGraph AI 제안 - before/after diff */}
                  <div className="space-y-2">
                    <div className="flex items-center gap-1.5">
                      <Lightbulb className="w-4 h-4 text-blue-600" />
                      <span className="text-xs font-semibold text-slate-900">DocGraph AI 제안</span>
                    </div>
                    <div className="rounded-lg border border-slate-200 overflow-hidden text-xs">
                      <div className="flex items-start gap-2 bg-red-50 text-slate-700 p-2.5 border-b border-slate-200">
                        <span className="bg-red-600 text-white rounded-full flex items-center justify-center w-4 h-4 text-[11px] font-bold shrink-0 mt-0.5">-</span>
                        <span className="whitespace-pre-wrap leading-relaxed line-through decoration-red-400">{issue.currentText}</span>
                      </div>
                      <div className="flex items-start gap-2 bg-green-50 text-slate-700 p-2.5">
                        <span className="bg-green-600 text-white rounded-full flex items-center justify-center w-4 h-4 text-[11px] font-bold shrink-0 mt-0.5">+</span>
                        <span className="whitespace-pre-wrap leading-relaxed">{issue.newText}</span>
                      </div>
                    </div>
                  </div>

                  {/* 하단 액션: 무시하기 / 제안 적용하기 */}
                  {/* TODO(API): "제안 적용하기" = POST /conflicts/{cid}/findings/{fid}/approve.
                                "무시하기" = POST /conflicts/{id}/ignore. */}
                  <div className="flex items-center justify-end gap-2 pt-1">
                    <button className="px-3 py-1.5 text-xs font-semibold text-slate-600 hover:bg-slate-100 rounded transition-colors">
                      무시하기
                    </button>
                    <button className="px-3 py-1.5 text-xs font-bold text-white bg-blue-600 hover:bg-blue-700 rounded shadow-sm transition-colors">
                      제안 적용하기
                    </button>
                  </div>
                </div>
              )}
            </div>
          );
        })}
      </div>
    </aside>
  );
};
