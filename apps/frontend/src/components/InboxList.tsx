import { useNavigate, useParams } from "react-router-dom";
import { useAppStore } from "../store";
import { AlertCircle, Bell, CheckCircle } from "lucide-react";

export const inboxNotifications = [
  {
    id: 1,
    type: "issue",
    title: '"WorkSync 도입 프로젝트"에 정합성 충돌이 의심되는 문서가 있습니다.',
    target: "📄 제품 요구사항 명세서 (PRD)",
    time: "2시간 전",
    docId: 2,
  },
  {
    id: 3,
    type: "issue",
    title: '"WorkSync 도입 프로젝트"에 정합성 충돌이 의심되는 문서가 있습니다.',
    target: "📄 API 명세서 (User & Auth 도메인)",
    time: "5시간 전",
    docId: 13,
  },
  {
    id: 4,
    type: "issue",
    title: '"WorkSync 도입 프로젝트"에 정합성 충돌이 의심되는 문서가 있습니다.',
    target: "📄 데이터베이스 스키마 설계서 (ERD 물리 모델)",
    time: "12시간 전",
    docId: 15,
  },
  {
    id: 2,
    type: "invite",
    title: '"WorkSync 도입 프로젝트" 프로젝트에 초대되었습니다.',
    target: "엔터프라이즈 근태관리 B2B SaaS 프로젝트",
    time: "1일 전",
  },
];

type Notif = (typeof inboxNotifications)[number];

interface InboxListProps {
  onClose?: () => void;
}

export const InboxList = ({ onClose }: InboxListProps) => {
  const navigate = useNavigate();
  const { workspaceId, projectId: urlProjectId } = useParams();
  const currentProjectId = useAppStore((s) => s.currentProjectId);
  const projectId = urlProjectId ?? (currentProjectId ? String(currentProjectId) : undefined);

  const handleNotificationClick = (notif: Notif) => {
    if (notif.type === "issue" && "docId" in notif && notif.docId && workspaceId && projectId) {
      navigate(`/w/${workspaceId}/p/${projectId}/docs/${notif.docId}?openIssues=true`);
      onClose?.();
    }
  };

  if (inboxNotifications.length === 0) {
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
      {inboxNotifications.map((notif) => (
        <div
          key={notif.id}
          onClick={() => handleNotificationClick(notif)}
          className={`p-4 border-b border-slate-100 hover:bg-slate-50 transition-colors ${
            notif.type === "issue" ? "cursor-pointer" : "cursor-default"
          }`}
        >
          <div className="flex items-start gap-3">
            <div
              className={`mt-0.5 shrink-0 w-8 h-8 rounded-full flex items-center justify-center ${
                notif.type === "issue" ? "bg-red-100 text-red-600" : "bg-blue-100 text-blue-600"
              }`}
            >
              {notif.type === "issue" ? (
                <AlertCircle className="w-4 h-4" />
              ) : (
                <Bell className="w-4 h-4" />
              )}
            </div>
            <div className="flex-1 min-w-0">
              <p className="text-[13px] font-medium leading-snug text-slate-900 mb-1">
                {notif.title}
              </p>
              <p className="text-[12px] text-slate-500 truncate mb-1">{notif.target}</p>
              <span className="text-[11px] text-slate-400 whitespace-nowrap block mt-1">
                {notif.time}
              </span>
            </div>
          </div>
        </div>
      ))}
    </div>
  );
};
