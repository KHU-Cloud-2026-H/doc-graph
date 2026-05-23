import { useEffect, useRef, useState, type RefObject } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useAppStore } from "../store";
import { Inbox, AlertCircle, Bell, CheckCircle } from "lucide-react";

export const inboxNotifications = [
  {
    id: 1,
    type: "issue",
    title: '"WorkSync 도입 프로젝트"에 정합성 충돌이 의심되는 문서가 있습니다.',
    target: "📄 제품 요구사항 명세서 (PRD)",
    time: "2시간 전",
    read: false,
    docId: 2,
  },
  {
    id: 3,
    type: "issue",
    title: '"WorkSync 도입 프로젝트"에 정합성 충돌이 의심되는 문서가 있습니다.',
    target: "📄 API 명세서 (User & Auth 도메인)",
    time: "5시간 전",
    read: true,
    docId: 13,
  },
  {
    id: 4,
    type: "issue",
    title: '"WorkSync 도입 프로젝트"에 정합성 충돌이 의심되는 문서가 있습니다.',
    target: "📄 데이터베이스 스키마 설계서 (ERD 물리 모델)",
    time: "12시간 전",
    read: true,
    docId: 15,
  },
  {
    id: 2,
    type: "invite",
    title: '"WorkSync 도입 프로젝트" 프로젝트에 초대되었습니다.',
    target: "엔터프라이즈 근태관리 B2B SaaS 프로젝트",
    time: "1일 전",
    read: false,
  },
];

type Notif = (typeof inboxNotifications)[number];

interface InboxPopupProps {
  isOpen: boolean;
  onClose: () => void;
  anchorRef: RefObject<HTMLElement | null>;
  placement?: 'below' | 'right';
}

export const InboxPopup = ({ isOpen, onClose, anchorRef, placement = 'below' }: InboxPopupProps) => {
  const navigate = useNavigate();
  const { workspaceId, projectId: urlProjectId } = useParams();
  const currentProjectId = useAppStore((s) => s.currentProjectId);
  const projectId = urlProjectId ?? (currentProjectId ? String(currentProjectId) : undefined);

  const popupRef = useRef<HTMLDivElement>(null);
  const [pos, setPos] = useState<{ top: number; left: number } | null>(null);

  useEffect(() => {
    if (isOpen && anchorRef.current) {
      const rect = anchorRef.current.getBoundingClientRect();
      const popupWidth = 320;
      const popupHeight = 450;
      let top: number;
      let left: number;

      if (placement === 'right') {
        left = rect.right + 8;
        top = rect.top;
        if (left + popupWidth > window.innerWidth) {
          left = rect.left - popupWidth - 8;
        }
        if (top + popupHeight > window.innerHeight) {
          top = window.innerHeight - popupHeight - 8;
        }
        if (top < 0) top = 8;
      } else {
        top = rect.bottom + 8;
        left = rect.left;
        if (top + popupHeight > window.innerHeight) {
          top = rect.top - popupHeight - 8;
        }
        if (left + popupWidth > window.innerWidth) {
          left = window.innerWidth - popupWidth - 8;
        }
        if (left < 0) left = 8;
      }

      setPos({ top, left });
    }
  }, [isOpen, anchorRef, placement]);

  useEffect(() => {
    if (!isOpen) return;
    const handleMouseDown = (e: MouseEvent) => {
      if (
        popupRef.current &&
        !popupRef.current.contains(e.target as Node) &&
        anchorRef.current &&
        !anchorRef.current.contains(e.target as Node)
      ) {
        onClose();
      }
    };
    document.addEventListener("mousedown", handleMouseDown);
    return () => document.removeEventListener("mousedown", handleMouseDown);
  }, [isOpen, onClose, anchorRef]);

  const handleNotificationClick = (notif: Notif) => {
    if (notif.type === "issue" && notif.docId && workspaceId && projectId) {
      navigate(`/w/${workspaceId}/p/${projectId}/docs/${notif.docId}?openIssues=true`);
      onClose();
    }
  };

  if (!isOpen || !pos) return null;

  return (
    <div
      ref={popupRef}
      className="fixed z-[100] w-80 bg-white border border-slate-200 shadow-xl rounded-lg overflow-hidden flex flex-col"
      style={{ top: pos.top, left: pos.left }}
    >
      <div className="flex items-center justify-between px-4 py-3 border-b border-slate-100 bg-slate-50/50">
        <div className="flex items-center gap-2">
          <Inbox className="w-4 h-4 text-slate-700" />
          <span className="font-semibold text-sm text-slate-900">Inbox</span>
        </div>
        <div className="flex items-center gap-2 text-xs text-slate-500 cursor-pointer hover:text-slate-700">
          <CheckCircle className="w-3.5 h-3.5" />
          <span>모두 읽음 표시</span>
        </div>
      </div>
      <div className="max-h-[400px] overflow-y-auto">
        {inboxNotifications.map((notif) => (
          <div
            key={notif.id}
            onClick={() => handleNotificationClick(notif)}
            className={`p-4 border-b border-slate-100 hover:bg-slate-50 transition-colors ${
              notif.type === "issue" ? "cursor-pointer" : "cursor-default"
            } ${!notif.read ? "bg-blue-50/30" : ""}`}
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
                <div className="flex justify-between items-start mb-1 gap-2">
                  <p
                    className={`text-[13px] font-medium leading-snug ${
                      !notif.read ? "text-slate-900" : "text-slate-700"
                    }`}
                  >
                    {notif.title}
                  </p>
                </div>
                <p className="text-[12px] text-slate-500 truncate mb-1">{notif.target}</p>
                <span className="text-[11px] text-slate-400 whitespace-nowrap block mt-1">
                  {notif.time}
                </span>
              </div>
              {!notif.read && <div className="w-2 h-2 bg-blue-600 rounded-full shrink-0 mt-1.5" />}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};
