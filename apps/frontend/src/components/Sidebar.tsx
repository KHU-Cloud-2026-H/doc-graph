import { Link, useLocation, useNavigate } from "react-router-dom";
import { useAppStore } from "../store";
import { ChevronDown, ChevronRight, Inbox, GitMerge, FileText, Settings, Layout, ArrowRight, Home, AlertCircle, Bell, CheckCircle } from "lucide-react";
import { useState, useRef, useEffect } from "react";

// TODO(@floating-ui/react): Inbox 오버레이 위치·오프셋은 @floating-ui/react(useFloating 등)로 이전하세요.
// react-popper는 팀 정책상 사용하지 않으며, 아래 usePopper 호출은 임시 비활성화했습니다.
// import { usePopper } from "react-popper";

const DocumentTreeItem = ({ node, level = 0 }: { node: any; level?: number }) => {
  const location = useLocation();
  const navigate = useNavigate();
  const isActive = location.pathname === `/w/sample-workspace/docs/${node.id}`;
  const hasChildren = node.children && node.children.length > 0;
  // 폴더이거나 하위 요소가 있으면 기본적으로 열림 상태로 시작
  const [isOpen, setIsOpen] = useState(node.isFolder || hasChildren);

  return (
    <li>
      <div 
        className={`flex items-center justify-between py-1 px-2 rounded-sm group cursor-pointer ${
          isActive ? "bg-blue-50 text-blue-700 font-medium" : "hover:bg-slate-100 text-slate-700 font-medium"
        }`}
        style={{ paddingLeft: `${level * 16 + 8}px` }}
        onClick={() => navigate(`/w/sample-workspace/docs/${node.id}`)}
      >
        <div className="flex items-center gap-1.5 flex-1 min-w-0 relative">
          {/* 토글 아이콘 & 이모지 영역 */}
          <div className="w-5 h-5 flex items-center justify-center shrink-0 relative">
            {/* Hover 시 보여질 토글 화살표 */}
            <div 
              className="absolute inset-0 flex items-center justify-center rounded-sm hover:bg-slate-200/80 transition-colors opacity-0 group-hover:opacity-100 z-10 cursor-pointer"
              onClick={(e) => {
                e.preventDefault();
                e.stopPropagation();
                setIsOpen(!isOpen);
              }}
            >
              {isOpen ? <ChevronDown className="w-3.5 h-3.5 text-slate-500" /> : <ChevronRight className="w-3.5 h-3.5 text-slate-500" />}
            </div>
            
            {/* 기본으로 보여질 이모지 또는 문서 아이콘 */}
            <div className="absolute inset-0 flex items-center justify-center group-hover:opacity-0 transition-opacity">
              {node.emoji ? (
                <span className="text-[14px] leading-none">{node.emoji}</span>
              ) : (
                <FileText className={`w-4 h-4 ${isActive ? "text-blue-600" : "text-slate-400"}`} />
              )}
            </div>
          </div>
          
          <div className="flex items-center gap-1.5 flex-1 truncate min-w-0">
            <span className="truncate text-[13px]">{node.title}</span>
          </div>
        </div>
        
        {node.hasIssue && (
          <span className="bg-red-600 text-white text-[10px] font-bold px-1.5 py-0 rounded-full shrink-0">
            1
          </span>
        )}
      </div>

      {isOpen && (
        <ul className="space-y-0.5">
          {hasChildren ? (
            node.children.map((child: any) => (
              <DocumentTreeItem key={child.id} node={child} level={level + 1} />
            ))
          ) : (
            <li>
              <div 
                className="text-slate-400 text-[13px] py-1 cursor-default"
                style={{ paddingLeft: `${level * 16 + 34}px` }}
              >
                하위 페이지 없음
              </div>
            </li>
          )}
        </ul>
      )}
    </li>
  );
};

export const Sidebar = () => {
  const { documents, isSidebarOpen } = useAppStore();
  const location = useLocation();
  const navigate = useNavigate();
  const isGraphActive = location.pathname === "/w/sample-workspace/graph";
  const [isWorkspaceDropdownOpen, setIsWorkspaceDropdownOpen] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);

  const [isInboxOpen, setIsInboxOpen] = useState(false);
  const [referenceElement, setReferenceElement] = useState<HTMLButtonElement | null>(null);
  const [popperElement, setPopperElement] = useState<HTMLDivElement | null>(null);
  // const { styles, attributes } = usePopper(referenceElement, popperElement, {
  //   placement: 'right-start',
  //   modifiers: [{ name: 'offset', options: { offset: [0, 8] } }],
  // });

  const inboxNotifications = [
    {
      id: 1,
      type: "issue",
      title: "정합성 문제(결정사항 전파 누락)가 발견되었습니다.",
      target: "📄 제품 요구사항 명세서 (PRD)",
      time: "2시간 전",
      read: false,
      docId: "prd",
    },
    {
      id: 3,
      type: "issue",
      title: "정합성 문제(용어 불일치)가 발견되었습니다.",
      target: "📄 API 명세서 (User & Auth 도메인)",
      time: "5시간 전",
      read: true,
      docId: "design-api-auth",
    },
    {
      id: 4,
      type: "issue",
      title: "정합성 문제(상태 정의 누락)가 발견되었습니다.",
      target: "📄 데이터베이스 스키마 설계서 (ERD 물리 모델)",
      time: "12시간 전",
      read: true,
      docId: "design-erd",
    },
    {
      id: 2,
      type: "invite",
      title: "'frontend' 프로젝트에 초대되었습니다.",
      target: "엔터프라이즈 근태관리 B2B SaaS 프로젝트",
      time: "1일 전",
      read: false,
    }
  ];

  const handleNotificationClick = (notif: any) => {
    if (notif.type === 'issue' && notif.docId) {
      navigate(`/w/sample-workspace/docs/${notif.docId}?openIssues=true`);
      setIsInboxOpen(false);
    }
  };

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setIsWorkspaceDropdownOpen(false);
      }
      if (
        isInboxOpen &&
        popperElement && !popperElement.contains(event.target as Node) &&
        referenceElement && !referenceElement.contains(event.target as Node)
      ) {
        setIsInboxOpen(false);
      }
    };
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, [isInboxOpen, popperElement, referenceElement]);

  if (!isSidebarOpen) return null;

  return (
    <nav className="fixed left-0 top-0 h-screen w-[288px] border-r border-slate-200 bg-white flex flex-col gap-2 z-40 hidden md:flex font-sans">
      {/* Workspace Switcher */}
      <div className="relative w-full" ref={dropdownRef}>
        <div 
          className="h-14 flex items-center justify-between px-4 border-b border-slate-200/60 cursor-pointer hover:bg-slate-50 transition-colors w-full"
          onClick={() => setIsWorkspaceDropdownOpen(!isWorkspaceDropdownOpen)}
        >
          <div className="flex items-center gap-3">
            <div className="w-5 h-5 flex items-center justify-center bg-blue-100 text-blue-700 rounded text-xs font-bold">
              엔
            </div>
            <span className="font-semibold text-slate-900 text-sm tracking-tight truncate max-w-[180px]">엔터프라이즈 근태관리...</span>
          </div>
          <ChevronDown className={`w-5 h-5 transition-colors ${isWorkspaceDropdownOpen ? "text-slate-900" : "text-slate-400 hover:text-slate-900"}`} />
        </div>

        {/* Dropdown Menu */}
        {isWorkspaceDropdownOpen && (
          <div className="absolute left-2 top-14 w-[300px] bg-white border border-slate-200 shadow-xl rounded-lg py-2 z-50">
            <div className="px-3 py-1.5 mb-1 border-b border-slate-100 flex items-center gap-2">
              <span className="text-xs font-medium text-slate-500">artboi@khu.ac.kr</span>
            </div>
            
            <div className="px-3 py-1.5 mt-2">
              <div className="text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-1">연동됨</div>
              <div className="space-y-0.5">
                <div className="flex items-center justify-between px-2 py-1.5 hover:bg-slate-50 rounded cursor-pointer group/item">
                  <div className="flex items-center gap-2">
                    <div className="w-5 h-5 flex items-center justify-center bg-blue-100 text-blue-700 rounded text-xs font-bold">
                      엔
                    </div>
                    <span className="text-[13px] font-medium text-slate-900">엔터프라이즈 근태관리 B2B SaaS 프로젝트</span>
                  </div>
                  <div className="w-1.5 h-1.5 rounded-full bg-blue-600 mr-1" />
                </div>
                <div className="flex items-center gap-2 px-2 py-1.5 hover:bg-slate-50 rounded cursor-pointer group/item">
                  <div className="w-5 h-5 flex items-center justify-center bg-slate-100 text-slate-700 rounded text-xs">
                    🕸️
                  </div>
                  <span className="text-[13px] text-slate-700">DocGraph Team</span>
                </div>
              </div>
            </div>

            <div className="px-3 py-1.5 mt-1">
              <div className="text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-1">연동되지 않음</div>
              <div className="space-y-0.5">
                <div className="flex items-center justify-between px-2 py-1.5 hover:bg-slate-50 rounded cursor-pointer group/item relative">
                  <div className="flex items-center gap-2">
                    <div className="w-5 h-5 flex items-center justify-center bg-slate-100 text-slate-700 opacity-60 rounded text-xs">
                      🧪
                    </div>
                    <span className="text-[13px] text-slate-400 group-hover/item:text-slate-700 transition-colors">상민의 워크스페이스</span>
                  </div>
                  <button className="opacity-0 group-hover/item:opacity-100 absolute right-2 flex items-center justify-center bg-blue-50 hover:bg-blue-100 text-blue-600 text-[10px] font-bold px-2 py-1 rounded transition-all border border-blue-200 shadow-sm z-10">
                    연동하기
                  </button>
                </div>
                <div className="flex items-center justify-between px-2 py-1.5 hover:bg-slate-50 rounded cursor-pointer group/item relative">
                  <div className="flex items-center gap-2">
                    <div className="w-5 h-5 flex items-center justify-center bg-slate-100 text-slate-700 opacity-60 rounded text-xs">
                      👨‍💻
                    </div>
                    <span className="text-[13px] text-slate-400 group-hover/item:text-slate-700 transition-colors">소프트웨어공학 13조</span>
                  </div>
                  <button className="opacity-0 group-hover/item:opacity-100 absolute right-2 flex items-center justify-center bg-blue-50 hover:bg-blue-100 text-blue-600 text-[10px] font-bold px-2 py-1 rounded transition-all border border-blue-200 shadow-sm z-10">
                    연동하기
                  </button>
                </div>
              </div>
            </div>

            <div className="mt-2 pt-2 border-t border-slate-100 px-2 pb-1">
              <Link 
                to="/workspaces" 
                onClick={() => setIsWorkspaceDropdownOpen(false)}
                className="flex items-center justify-between px-3 py-2 bg-slate-50 hover:bg-blue-50 rounded-md text-slate-700 hover:text-blue-700 transition-colors border border-slate-200 hover:border-blue-200"
              >
                <div className="flex items-center gap-2">
                  <Home className="w-4 h-4" />
                  <span className="text-[13px] font-bold">DocGraph Home</span>
                </div>
                <ArrowRight className="w-3.5 h-3.5" />
              </Link>
            </div>
          </div>
        )}
      </div>

      {/* Primary Links */}
      <div className="mx-2 mt-2 space-y-1 mb-3 relative">
        <button 
          ref={setReferenceElement}
          onClick={() => setIsInboxOpen(!isInboxOpen)}
          className={`flex w-full items-center justify-between px-3 py-2 rounded-md transition-colors ${isInboxOpen ? 'bg-slate-100 text-slate-900' : 'hover:bg-slate-100 text-slate-700'} group`}
        >
          <div className="flex items-center gap-2.5">
            <Inbox className={`w-4 h-4 ${isInboxOpen ? 'text-slate-700' : 'text-slate-500'}`} />
            <span className="text-[14px] font-medium">Inbox</span>
          </div>
          <span className="bg-red-600 text-white text-[10px] font-bold px-1.5 py-0 rounded-full">
            2
          </span>
        </button>

        {isInboxOpen && (
          <div
            ref={setPopperElement}
            className="absolute left-full top-0 ml-2 z-[100] w-80 bg-white border border-slate-200 shadow-xl rounded-lg overflow-hidden flex flex-col"
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
                  className={`p-4 border-b border-slate-100 hover:bg-slate-50 transition-colors ${notif.type === 'issue' ? 'cursor-pointer' : 'cursor-default'} ${!notif.read ? "bg-blue-50/30" : ""}`}
                >
                  <div className="flex items-start gap-3">
                    <div className={`mt-0.5 shrink-0 w-8 h-8 rounded-full flex items-center justify-center ${notif.type === 'issue' ? 'bg-red-100 text-red-600' : 'bg-blue-100 text-blue-600'}`}>
                      {notif.type === 'issue' ? <AlertCircle className="w-4 h-4" /> : <Bell className="w-4 h-4" />}
                    </div>
                    <div className="flex-1 min-w-0">
                      <div className="flex justify-between items-start mb-1 gap-2">
                        <p className={`text-[13px] font-medium leading-snug ${!notif.read ? "text-slate-900" : "text-slate-700"}`}>
                          {notif.title}
                        </p>
                      </div>
                      <p className="text-[12px] text-slate-500 truncate mb-1">
                        {notif.target}
                      </p>
                      <span className="text-[11px] text-slate-400 whitespace-nowrap block mt-1">{notif.time}</span>
                    </div>
                    {!notif.read && (
                      <div className="w-2 h-2 bg-blue-600 rounded-full shrink-0 mt-1.5" />
                    )}
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        <Link
          to="/w/sample-workspace/graph"
          className={`flex items-center justify-between px-3 py-2 rounded-md group transition-colors text-[14px] ${
            isGraphActive ? "bg-blue-50 text-blue-700 font-medium" : "hover:bg-slate-100 text-slate-700 font-medium"
          }`}
        >
          <div className="flex items-center gap-2.5">
            <GitMerge className={`w-4 h-4 ${isGraphActive ? "text-blue-600" : "text-slate-500"}`} />
            <span>Dependency Graph</span>
          </div>
        </Link>
        
        <button className="flex w-full items-center justify-between px-3 py-2 rounded-md hover:bg-slate-100 text-slate-700 group transition-colors">
          <div className="flex items-center gap-2.5">
            <Layout className="w-4 h-4 text-slate-500" />
            <span className="text-[14px] font-medium">Projects</span>
          </div>
        </button>
      </div>

      {/* Document Tree */}
      <div className="flex-1 overflow-y-auto mx-2 scrollbar-hide">
        <div className="text-[11px] font-bold text-slate-400 uppercase tracking-wider mb-1 px-2">
          팀스페이스
        </div>
        <ul className="space-y-0.5">
          {documents.map((doc) => (
            <DocumentTreeItem key={doc.id} node={doc} />
          ))}
        </ul>
      </div>

      {/* Footer */}
      <div className="mt-auto border-t border-slate-200 space-y-0.5 mx-2 pt-2 pb-6">
        <button className="flex items-center gap-2 w-full px-2 py-1.5 rounded-sm hover:bg-slate-100 text-slate-700 transition-colors text-[13px] font-medium">
          <Settings className="w-4 h-4 text-slate-500" />
          <span>Workspace Settings</span>
        </button>
        <div className="flex items-center gap-2 w-full px-2 py-1.5 rounded-sm hover:bg-slate-100 text-slate-700 transition-colors cursor-pointer mt-1 font-medium">
          <img
            src="https://images.unsplash.com/photo-1599566150163-29194dcaad36?q=80&w=300&auto=format&fit=crop"
            alt="User"
            className="w-4 h-4 rounded-full object-cover"
          />
          <span className="text-[13px]">김상민</span>
        </div>
        <div className="text-left font-bold text-slate-800 text-2xl tracking-tight px-2 mt-4">
          DocGraph
        </div>
        <div className="text-[10px] text-slate-400 px-2 mt-0.5 pb-2">
          DocGraph는 실수할 수 있습니다.
        </div>
      </div>
    </nav>
  );
};