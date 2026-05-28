import { useNavigate, useParams } from "react-router-dom";
import { useAppStore } from "../store";
import { AlertCircle, CheckCircle } from "lucide-react";

// TODO(API): GET /inbox?status=ACTIVE|IGNORED|ALL → InboxItemResponse[] 로 교체
// API 응답 필드: id, type("CONFLICT"), projectId, documentId, status, createdAt 등
export const inboxNotifications = [
  {
    id: 1,
    type: "issue",
    status: 'ACTIVE' as const,
    projectId: 1,
    title: '"WorkSync 도입 프로젝트"에 정합성 충돌이 의심되는 문서가 있습니다.',
    target: "📄 제품 요구사항 명세서 (PRD)",
    time: "2시간 전",
    docId: 2,
  },
  {
    id: 3,
    type: "issue",
    status: 'ACTIVE' as const,
    projectId: 1,
    title: '"WorkSync 도입 프로젝트"에 정합성 충돌이 의심되는 문서가 있습니다.',
    target: "📄 API 명세서 (User & Auth 도메인)",
    time: "5시간 전",
    docId: 13,
  },
  {
    id: 4,
    type: "issue",
    status: 'IGNORED' as const,
    projectId: 1,
    title: '"WorkSync 도입 프로젝트"에 정합성 충돌이 의심되는 문서가 있습니다.',
    target: "📄 데이터베이스 스키마 설계서 (ERD 물리 모델)",
    time: "12시간 전",
    docId: 15,
  },
];

type Notif = (typeof inboxNotifications)[number];

export type InboxFilter = 'ACTIVE' | 'IGNORED' | 'ALL';

// ACTIVE(미해소) 알림 건수 — Sidebar/TopAppBar/WorkspaceSelection 배지용
export const inboxActiveCount = inboxNotifications.filter((n) => n.status === 'ACTIVE').length;

interface InboxListProps {
  onClose?: () => void;
  filter?: InboxFilter;
}

export const InboxList = ({ onClose, filter }: InboxListProps) => {
  const navigate = useNavigate();
  const { workspaceId: urlWorkspaceId, projectId: urlProjectId } = useParams();
  const currentProjectId = useAppStore((s) => s.currentProjectId);
  const projects = useAppStore((s) => s.projects);

  const projectId = urlProjectId ?? (currentProjectId ? String(currentProjectId) : undefined);

  const activeFilter = filter ?? 'ACTIVE';

  const filteredNotifications = inboxNotifications.filter((n) => {
    if (activeFilter === 'ALL') return true;
    if (activeFilter === 'IGNORED') return n.status === 'IGNORED';
    return n.status === 'ACTIVE';
  });

  const handleNotificationClick = (notif: Notif) => {
    if ("docId" in notif && notif.docId) {
      // URL params 우선 사용 (프로젝트/문서 페이지 내 Inbox).
      // 없으면 알림의 projectId로 store에서 workspaceId 역조회 (WorkspaceSelection 등).
      // TODO(API): API 응답에 workspaceId가 포함되면 역조회 없이 직접 사용.
      const targetProjectId = projectId ?? String(notif.projectId);
      const targetWorkspaceId =
        urlWorkspaceId ??
        String(projects.find((p) => p.id === notif.projectId)?.workspaceId ?? '');

      if (targetWorkspaceId && targetProjectId) {
        navigate(`/w/${targetWorkspaceId}/p/${targetProjectId}/docs/${notif.docId}?openIssues=true`);
        onClose?.();
      }
    }
  };

  if (filteredNotifications.length === 0) {
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
      {filteredNotifications.map((notif) => (
        <div
          key={notif.id}
          onClick={() => handleNotificationClick(notif)}
          className="p-4 border-b border-slate-100 hover:bg-slate-50 transition-colors cursor-pointer"
        >
          <div className="flex items-start gap-3">
            <div className="mt-0.5 shrink-0 w-8 h-8 rounded-full flex items-center justify-center bg-red-100 text-red-600">
              <AlertCircle className="w-4 h-4" />
            </div>
            <div className="flex-1 min-w-0">
              <p className="text-[13px] font-medium leading-snug text-slate-900 mb-1">
                {notif.title}
              </p>
              <p className="text-[12px] text-slate-500 truncate mb-1">{notif.target}</p>
              <span className="text-[11px] text-slate-400 whitespace-nowrap flex items-center gap-1 mt-1">
                {notif.time}
                <span className="text-slate-300">·</span>
                <span className={`inline-flex items-center px-1.5 py-0 rounded-full text-[10px] font-semibold leading-5 ${
                  notif.status === 'ACTIVE' ? 'bg-red-100 text-red-700' : 'bg-emerald-100 text-emerald-700'
                }`}>
                  {notif.status === 'ACTIVE' ? '미해소' : '무시됨'}
                </span>
              </span>
            </div>
          </div>
        </div>
      ))}
    </div>
  );
};
