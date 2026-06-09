import { X, AlertTriangle, FileText, ShieldCheck, GitBranch, CheckCircle, XCircle, Loader2 } from "lucide-react";
import { Link, useParams } from "react-router-dom";
import type { AppEdge } from "../features/graph/mockData";
import type { ConflictResponse } from "../hooks/useProjectConflicts";

const SOURCE_LABEL: Record<string, string> = {
  NOTION_REFERENCE: 'Notion 링크/멘션',
  PROPOSAL_ACCEPTED: '제안 수락',
  CUSTOM: '수동 추가',
};

interface Props {
  edge: AppEdge;
  sourceLabel: string;
  targetLabel: string;
  conflict?: ConflictResponse;
  isAccepting: boolean;
  isRejecting: boolean;
  onAccept: () => void;
  onReject: () => void;
  onClose: () => void;
}

export const GraphRightSidebar = ({
  edge,
  sourceLabel,
  targetLabel,
  conflict,
  isAccepting,
  isRejecting,
  onAccept,
  onReject,
  onClose,
}: Props) => {
  const { workspaceId, projectId } = useParams();
  const isProposal = edge.id.startsWith('p-');
  const isConflict = edge.data?.conflictStatus === 'CONFLICT';
  const findings = conflict?.findings ?? [];

  return (
    <aside className="w-[420px] h-full bg-white border-l border-slate-200 shadow-2xl flex flex-col shrink-0 z-50 font-sans absolute right-0 top-0">
      {/* Header */}
      <div className="border-b border-slate-200 flex items-center justify-between h-14 px-4 shrink-0 bg-white">
        <div className="flex items-center gap-2">
          {isProposal ? (
            <GitBranch className="w-4 h-4 text-slate-500" />
          ) : (
            <AlertTriangle className="w-4 h-4 text-red-600" />
          )}
          <h3 className="font-bold text-sm text-slate-900 tracking-tight">
            {isProposal ? 'EDGE PROPOSAL' : 'INTEGRITY ISSUE'}
          </h3>
        </div>
        <button
          onClick={onClose}
          className="p-1.5 rounded-md hover:bg-slate-100 text-slate-400 hover:text-slate-900 transition-colors flex items-center justify-center"
        >
          <X className="w-4 h-4" />
        </button>
      </div>

      <div className="flex-1 overflow-y-auto p-5 space-y-6">
        {/* 충돌 findings — INTEGRITY ISSUE일 때만 */}
        {isConflict && (
          <div>
            <div className="flex items-center gap-2 mb-3">
              <AlertTriangle className="w-4 h-4 text-red-600" />
              <h4 className="text-sm font-semibold text-slate-900">충돌 내용</h4>
            </div>
            {findings.length === 0 ? (
              <div className="bg-red-50 border border-red-100 rounded-lg px-4 py-3">
                <p className="text-xs text-red-500">충돌 상세 정보를 불러올 수 없습니다.</p>
              </div>
            ) : (
              <div className="space-y-3">
                {findings.map((f, idx) => (
                  <div key={f.id ?? idx} className="border border-red-100 rounded-lg overflow-hidden">
                    {f.title && (
                      <div className="bg-red-50 px-3 py-2 border-b border-red-100">
                        <p className="text-xs font-semibold text-red-700">{f.title}</p>
                      </div>
                    )}
                    {f.rationale && (
                      <div className="px-3 py-2.5">
                        <p className="text-xs text-slate-700 leading-relaxed">{f.rationale}</p>
                      </div>
                    )}
                    {f.newText && (
                      <div className="border-t border-red-100 px-3 py-2 bg-green-50">
                        <p className="text-[10px] font-semibold text-green-700 mb-1">AI 제안 수정</p>
                        <p className="text-xs text-green-800 leading-relaxed whitespace-pre-wrap">{f.newText}</p>
                      </div>
                    )}
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

        {/* 관련 문서 */}
        <div>
          <div className="flex items-center gap-2 mb-3">
            <h4 className="text-sm font-semibold text-slate-900">관련 문서</h4>
          </div>
          <div className="space-y-2">
            <div className="border border-slate-200 rounded-lg overflow-hidden">
              <div className="bg-slate-50 px-3 py-2 flex items-center justify-between border-b border-slate-100">
                <div className="flex items-center gap-1.5">
                  <FileText className="w-3.5 h-3.5 text-slate-500" />
                  <span className="text-xs font-semibold text-slate-700">출발 문서</span>
                </div>
                <Link
                  to={`/w/${workspaceId}/p/${projectId}/docs/${edge.source}`}
                  className="text-[10px] font-bold text-blue-600 hover:underline"
                >
                  바로가기 →
                </Link>
              </div>
              <div className="bg-white px-3 py-2.5">
                <p className="text-xs text-slate-800 font-medium">{sourceLabel || edge.source}</p>
              </div>
            </div>

            <div className="border border-slate-200 rounded-lg overflow-hidden">
              <div className="bg-slate-50 px-3 py-2 flex items-center justify-between border-b border-slate-100">
                <div className="flex items-center gap-1.5">
                  <FileText className="w-3.5 h-3.5 text-slate-500" />
                  <span className="text-xs font-semibold text-slate-700">도착 문서</span>
                </div>
                <Link
                  to={`/w/${workspaceId}/p/${projectId}/docs/${edge.target}`}
                  className="text-[10px] font-bold text-blue-600 hover:underline"
                >
                  바로가기 →
                </Link>
              </div>
              <div className="bg-white px-3 py-2.5">
                <p className="text-xs text-slate-800 font-medium">{targetLabel || edge.target}</p>
              </div>
            </div>
          </div>
        </div>

        {/* 검증 기준 */}
        {edge.data?.validationCriterion && (
          <div>
            <div className="flex items-center gap-2 mb-3">
              <h4 className="text-sm font-semibold text-slate-900">검증 기준</h4>
              <ShieldCheck className="w-4 h-4 text-slate-600" />
            </div>
            <div className="bg-slate-50 p-4 rounded-lg border border-slate-200">
              <p className="text-sm text-slate-700 leading-relaxed">{edge.data.validationCriterion}</p>
            </div>
          </div>
        )}

        {/* 엣지 정보 */}
        <div>
          <h4 className="text-sm font-semibold text-slate-900 mb-3">엣지 정보</h4>
          <div className="space-y-2">
            <div className="flex items-center justify-between py-1.5 border-b border-slate-100">
              <span className="text-xs text-slate-500">생성 출처</span>
              <span className="text-xs font-medium text-slate-700">
                {SOURCE_LABEL[edge.data?.source ?? ''] ?? edge.data?.source ?? '-'}
              </span>
            </div>
            <div className="flex items-center justify-between py-1.5 border-b border-slate-100">
              <span className="text-xs text-slate-500">충돌 상태</span>
              <span className={`text-xs font-bold ${isConflict ? 'text-red-600' : 'text-emerald-600'}`}>
                {isConflict ? '충돌 감지됨' : '정상'}
              </span>
            </div>
            {isProposal && (
              <div className="flex items-center justify-between py-1.5">
                <span className="text-xs text-slate-500">유형</span>
                <span className="text-xs font-medium text-blue-600">AI 추론 제안</span>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Footer Actions */}
      <div className="border-t border-slate-200 p-4 bg-white mt-auto shrink-0 flex flex-col gap-2.5">
        {isProposal ? (
          <>
            <button
              onClick={onAccept}
              disabled={isAccepting || isRejecting}
              className="w-full py-2.5 bg-blue-600 hover:bg-blue-700 disabled:opacity-50 text-white text-xs font-bold rounded-md shadow-sm transition-colors flex items-center justify-center gap-2"
            >
              {isAccepting ? (
                <Loader2 className="w-4 h-4 animate-spin" />
              ) : (
                <CheckCircle className="w-4 h-4" />
              )}
              {isAccepting ? '수락 중...' : '제안 수락'}
            </button>
            <button
              onClick={onReject}
              disabled={isAccepting || isRejecting}
              className="w-full py-2.5 bg-white border border-slate-200 hover:bg-red-50 hover:border-red-200 hover:text-red-600 disabled:opacity-50 text-slate-700 text-xs font-bold rounded-md transition-colors flex items-center justify-center gap-2"
            >
              {isRejecting ? (
                <Loader2 className="w-4 h-4 animate-spin" />
              ) : (
                <XCircle className="w-4 h-4" />
              )}
              {isRejecting ? '거절 중...' : '제안 거절'}
            </button>
          </>
        ) : (
          <button
            disabled
            title="백엔드 구현 예정"
            className="w-full py-2.5 bg-slate-100 text-slate-400 text-xs font-bold rounded-md cursor-not-allowed flex items-center justify-center gap-2"
          >
            <ShieldCheck className="w-4 h-4" />
            제안 적용하기 (준비 중)
          </button>
        )}
        <Link
          to={`/w/${workspaceId}/p/${projectId}/docs/${edge.source}`}
          className="w-full py-2.5 bg-white border border-slate-200 hover:bg-slate-50 text-slate-700 text-xs font-bold rounded-md transition-colors flex items-center justify-center"
        >
          출발 문서 열기
        </Link>
      </div>
    </aside>
  );
};
