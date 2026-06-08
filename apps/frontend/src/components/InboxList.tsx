import { useNavigate, useParams } from "react-router-dom";
import { useAppStore } from "../store";
import { AlertCircle, CheckCircle } from "lucide-react";
import { useInbox, type MyConflictRow } from "../hooks/useInbox";

export type InboxFilter = 'ACTIVE' | 'IGNORED' | 'ALL';

// ACTIVE 건수 훅 — Sidebar/TopAppBar 배지용
export function useInboxActiveCount() {
  const { conflicts } = useInbox();
  return conflicts.filter((c) => c.status === 'ACTIVE').length;
}

function toRelativeTime(iso: string): string {
  const diff = Date.now() - new Date(iso).getTime();
  const mins = Math.floor(diff / 60000);
  if (mins < 60) return `${mins}분 전`;
  const hours = Math.floor(mins / 60);
  if (hours < 24) return `${hours}시간 전`;
  return `${Math.floor(hours / 24)}일 전`;
}

interface InboxListProps {
  onClose?: () => void;
  filter?: InboxFilter;
}

export const InboxList = ({ onClose, filter }: InboxListProps) => {
  const navigate = useNavigate();
  const { workspaceId: urlWorkspaceId, projectId: urlProjectId } = useParams();
  const currentProjectId = useAppStore((s) => s.currentProjectId);
  const { conflicts, isLoading } = useInbox();

  const projectId = urlProjectId ?? (currentProjectId ? String(currentProjectId) : undefined);
  const activeFilter = filter ?? 'ACTIVE';

  const filtered = conflicts.filter((c) => {
    if (activeFilter === 'ALL') return true;
    if (activeFilter === 'IGNORED') return c.status === 'IGNORED';
    return c.status === 'ACTIVE';
  });

  const handleClick = (conflict: MyConflictRow) => {
    const targetProjectId = projectId ?? String(conflict.projectId);
    const targetWorkspaceId = urlWorkspaceId ?? String(conflict.workspaceId);

    if (targetWorkspaceId && targetProjectId) {
      navigate(`/w/${targetWorkspaceId}/p/${targetProjectId}/docs/${conflict.targetDocument.id}?openIssues=true`);
      onClose?.();
    }
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-12">
        <p className="text-slate-400 text-sm">불러오는 중...</p>
      </div>
    );
  }

  if (filtered.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center py-12">
        <CheckCircle className="w-16 h-16 text-slate-300" />
        <p className="text-slate-400 text-center text-sm mt-3 whitespace-pre-line">
          {"정합성 충돌이\n발견되지 않았습니다!"}
        </p>
      </div>
    );
  }

  return (
    <div>
      {filtered.map((conflict) => (
        <div
          key={conflict.id}
          onClick={() => handleClick(conflict)}
          className="p-4 border-b border-slate-100 hover:bg-slate-50 transition-colors cursor-pointer"
        >
          <div className="flex items-start gap-3">
            <div className="mt-0.5 shrink-0 w-8 h-8 rounded-full flex items-center justify-center bg-red-100 text-red-600">
              <AlertCircle className="w-4 h-4" />
            </div>
            <div className="flex-1 min-w-0">
              <p className="text-[13px] font-medium leading-snug text-slate-900 mb-1">
                {conflict.title}
              </p>
              <p className="text-[12px] text-slate-500 truncate mb-1">
                📄 {conflict.targetDocument.title}
              </p>
              <span className="text-[11px] text-slate-400 whitespace-nowrap flex items-center gap-1 mt-1">
                {toRelativeTime(conflict.firstDetectedAt)}
                <span className="text-slate-300">·</span>
                <span className={`inline-flex items-center px-1.5 py-0 rounded-full text-[10px] font-semibold leading-5 ${
                  conflict.status === 'ACTIVE' ? 'bg-red-100 text-red-700' : 'bg-emerald-100 text-emerald-700'
                }`}>
                  {conflict.status === 'ACTIVE' ? '미해소' : '무시됨'}
                </span>
              </span>
            </div>
          </div>
        </div>
      ))}
    </div>
  );
};
