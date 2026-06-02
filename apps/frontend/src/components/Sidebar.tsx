import { Link, useLocation, useNavigate, useParams } from "react-router-dom";
import { useAppStore } from "../store";
import { ChevronDown, ChevronRight, Inbox, GitMerge, FileText, Layout, Home, Plus, ArrowRight } from "lucide-react";
import { useState, useRef, useEffect, useLayoutEffect } from "react";
import { InboxPopup } from "./InboxPopup";
import { useInboxActiveCount } from "./InboxList";
import { TopAppBar } from "./TopAppBar";
import { UserProfilePopup, UserAvatar } from "./UserProfilePopup";


const DocumentTreeItem = ({ node, level = 0 }: { node: any; level?: number }) => {
  const location = useLocation();
  const navigate = useNavigate();
  const { workspaceId, projectId } = useParams();
  const isActive = location.pathname === `/w/${workspaceId}/p/${projectId}/docs/${node.id}`;
  const hasChildren = node.children && node.children.length > 0;
  const [isOpen, setIsOpen] = useState(hasChildren);

  return (
    <li>
      <div
        className={`flex items-center gap-1.5 py-1 px-2 rounded-sm group cursor-pointer overflow-hidden ${isActive ? "bg-blue-50 text-blue-700 font-medium" : "hover:bg-slate-100 text-slate-700 font-medium"
          }`}
        style={{ paddingLeft: `${level * 16 + 8}px` }}
        onClick={() => {
          if (workspaceId && projectId) {
            navigate(`/w/${workspaceId}/p/${projectId}/docs/${node.id}`);
          }
        }}
      >
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

        <span className="truncate text-[13px] flex-1 min-w-0">{node.title}</span>

        {(() => {
          const activeCount = (node.issues ?? []).filter((i: any) => i.status !== 'IGNORED').length;
          return activeCount > 0 ? (
            <span className="bg-red-600 text-white text-[10px] font-bold px-1.5 py-0 rounded-full shrink-0">
              {activeCount}
            </span>
          ) : null;
        })()}
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
  const { documents, projects, workspaces, setCurrentProject, isSidebarOpen, toggleSidebar } = useAppStore();
  const inboxActiveCount = useInboxActiveCount();
  const location = useLocation();
  const navigate = useNavigate();
  const { workspaceId, projectId } = useParams();
  const isGraphActive = location.pathname === `/w/${workspaceId}/p/${projectId}/graph`;
  const [isProjectDropdownOpen, setIsProjectDropdownOpen] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);

  const [isInboxOpen, setIsInboxOpen] = useState(false);
  const inboxButtonRef = useRef<HTMLButtonElement>(null);
  const [isProfileOpen, setIsProfileOpen] = useState(false);
  const profileButtonRef = useRef<HTMLButtonElement>(null);
  const sidebarClosedByRoute = useRef(false);

  const isFullPageRoute = /\/w\/[^/]+\/(settings|new-project)$/.test(location.pathname);

  // 페인트 전에 동기적으로 사이드바 상태를 조정 — Layout의 md:ml-[288px] 제거/복원
  useLayoutEffect(() => {
    if (isFullPageRoute && isSidebarOpen) {
      toggleSidebar();
      sidebarClosedByRoute.current = true;
    }
    return () => {
      if (sidebarClosedByRoute.current) {
        toggleSidebar();
        sidebarClosedByRoute.current = false;
      }
    };
  }, [isFullPageRoute]); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setIsProjectDropdownOpen(false);
      }
    };
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  // full-page route: flex flow 밖에 fixed로 TopAppBar 렌더링
  if (isFullPageRoute) {
    const workspaceName = workspaces.find((w) => w.id === Number(workspaceId))?.name ?? '워크스페이스';
    return (
      <div className="fixed inset-x-0 top-0 z-50">
        <TopAppBar centerLabel={workspaceName} />
      </div>
    );
  }

  if (!isSidebarOpen) return null;

  return (
    <nav className="fixed left-0 top-0 h-screen w-[288px] border-r border-slate-200 bg-white flex flex-col gap-2 z-40 hidden md:flex font-sans">
      {/* Project Switcher */}
      <div className="relative w-full" ref={dropdownRef}>
        <div
          className="h-14 flex items-center justify-between px-4 border-b border-slate-200/60 cursor-pointer hover:bg-slate-50 transition-colors w-full"
          onClick={() => setIsProjectDropdownOpen(!isProjectDropdownOpen)}
        >
          <div className="flex items-center gap-3">
            {(() => {
              const currentProject = projects.find((p) => p.id === Number(projectId));
              return currentProject?.notionRootPageEmoji ? (
                <span className="text-base leading-none w-5 h-5 flex items-center justify-center">
                  {currentProject.notionRootPageEmoji}
                </span>
              ) : (
                <div className="w-5 h-5 flex items-center justify-center bg-blue-100 text-blue-700 rounded text-xs font-bold">
                  {currentProject?.name?.[0] || 'P'}
                </div>
              );
            })()}
            <span className="font-semibold text-slate-900 text-sm tracking-tight truncate max-w-[180px]">
              {projects.find((p) => p.id === Number(projectId))?.name || '프로젝트 선택'}
            </span>
          </div>
          <ChevronDown className={`w-5 h-5 transition-colors ${isProjectDropdownOpen ? "text-slate-900" : "text-slate-400 hover:text-slate-900"}`} />
        </div>

        {/* Dropdown Menu */}
        {isProjectDropdownOpen && (
          <div className="absolute left-2 top-14 w-[300px] bg-white border border-slate-200 shadow-xl rounded-lg z-50 overflow-hidden">
            {/* 1. 현재 워크스페이스명 */}
            <div
              className="px-4 py-3 cursor-pointer hover:bg-slate-50 transition-colors"
              onClick={() => {
                navigate(`/w/${workspaceId}`);
                setIsProjectDropdownOpen(false);
              }}
            >
              <div className="flex items-start gap-2.5">
                <div className="w-5 h-5 flex items-center justify-center bg-blue-100 text-blue-700 rounded text-xs font-bold shrink-0 mt-0.5">
                  {(workspaces.find((w) => w.id === Number(workspaceId))?.name ?? 'W')[0]}
                </div>
                <div className="min-w-0">
                  <p className="text-base font-bold text-slate-900 leading-snug truncate">
                    {workspaces.find((w) => w.id === Number(workspaceId))?.name ?? '워크스페이스'}
                  </p>
                  <p className="flex items-center gap-0.5 text-xs text-slate-400 mt-0.5">
                    워크스페이스 홈
                    <ArrowRight className="w-3 h-3" />
                  </p>
                </div>
              </div>
            </div>
            {/* 2. 구분선 */}
            <hr className="border-slate-100" />
            {/* 3. 프로젝트 목록 헤더 + 새 프로젝트 버튼 */}
            <div className="px-3 py-2 flex items-center justify-between">
              <span className="text-xs font-medium text-slate-500">프로젝트 목록</span>
              <button
                className="flex items-center gap-1 text-[11px] font-semibold text-blue-600 hover:text-blue-700 transition-colors"
                onClick={() => {
                  navigate(`/w/${workspaceId}/new-project`);
                  setIsProjectDropdownOpen(false);
                }}
              >
                <Plus className="w-3 h-3" />
                새 프로젝트
              </button>
            </div>
            {/* 4. 프로젝트 목록 */}
            <div className="px-2 pb-2 space-y-0.5">
              {projects.filter((p) => p.workspaceId === Number(workspaceId)).map((p) => (
                <div
                  key={p.id}
                  className={`flex items-center justify-between px-2 py-1.5 hover:bg-slate-50 rounded cursor-pointer ${p.id === Number(projectId) ? 'bg-blue-50' : ''}`}
                  onClick={() => {
                    setCurrentProject(p.id);
                    navigate(`/w/${workspaceId}/p/${p.id}/graph`);
                    setIsProjectDropdownOpen(false);
                  }}
                >
                  <div className="flex items-center gap-2">
                    {p.notionRootPageEmoji ? (
                      <span className="text-base leading-none w-5 h-5 flex items-center justify-center">
                        {p.notionRootPageEmoji}
                      </span>
                    ) : (
                      <div className="w-5 h-5 flex items-center justify-center bg-blue-100 text-blue-700 rounded text-xs font-bold">
                        {p.name[0]}
                      </div>
                    )}
                    <span className="text-[13px] font-medium text-slate-900">{p.name}</span>
                  </div>
                  {p.id === Number(projectId) && <div className="w-1.5 h-1.5 rounded-full bg-blue-600" />}
                </div>
              ))}
            </div>
            {/* 5. 구분선 */}
            <hr className="border-slate-100" />
            {/* 6. DocGraph Main 링크 */}
            <div className="px-2 py-2">
              <Link
                to="/workspaces"
                onClick={() => setIsProjectDropdownOpen(false)}
                className="flex items-center gap-2 px-3 py-2 hover:bg-slate-50 rounded-md text-slate-600 hover:text-slate-900 transition-colors text-[13px] font-medium"
              >
                <Home className="w-4 h-4" />
                DocGraph Main
              </Link>
            </div>
          </div>
        )}
      </div>

      {/* Primary Links */}
      <div className="mx-2 mt-2 space-y-1 mb-3 relative">
        <Link
          to={workspaceId && projectId ? `/w/${workspaceId}/p/${projectId}/graph` : '#'}
          className={`flex items-center justify-between px-3 py-2 rounded-md group transition-colors text-[14px] ${isGraphActive ? "bg-blue-50 text-blue-700 font-medium" : "hover:bg-slate-100 text-slate-700 font-medium"
            }`}
        >
          <div className="flex items-center gap-2.5">
            <GitMerge className={`w-4 h-4 ${isGraphActive ? "text-blue-600" : "text-slate-500"}`} />
            <span>Dependency Graph</span>
          </div>
        </Link>
        <Link
          to={workspaceId && projectId ? `/w/${workspaceId}/p/${projectId}/settings` : '#'}
          className="flex w-full items-center justify-between px-3 py-2 rounded-md hover:bg-slate-100 text-slate-700 group transition-colors"
        >
          <div className="flex items-center gap-2.5">
            <Layout className="w-4 h-4 text-slate-500" />
            <span className="text-[14px] font-medium">Project Settings</span>
          </div>
        </Link>
      </div>

      {/* Document Tree */}
      {projectId ? (
        <div className="flex-1 overflow-y-auto mx-2 scrollbar-hide">
          <div className="text-[11px] font-bold text-slate-400 uppercase tracking-wider mb-1 px-2">
            {projects.find((p) => p.id === Number(projectId))?.name || '팀스페이스'}
          </div>
          <ul className="space-y-0.5">
            {documents.map((doc) => (
              <DocumentTreeItem key={doc.id} node={doc} />
            ))}
          </ul>
        </div>
      ) : (
        <div className="flex-1 mx-2 flex items-center justify-center text-slate-400 text-xs">프로젝트를 선택하세요</div>
      )}

      {/* Footer + Inbox/Settings */}
      <div className="mt-auto border-t border-slate-200 space-y-0.5 mx-2 pt-2 pb-6">
        <button
          ref={inboxButtonRef}
          onClick={() => setIsInboxOpen(!isInboxOpen)}
          className={`flex w-full items-center justify-between px-2 py-1.5 rounded-sm transition-colors ${isInboxOpen ? 'bg-slate-100 text-slate-900' : 'hover:bg-slate-100 text-slate-700'} group`}
        >
          <div className="flex items-center gap-2">
            <Inbox className={`w-[18px] h-[18px] ${isInboxOpen ? 'text-slate-700' : 'text-slate-500'}`} />
            <span className="text-[14px] font-medium">Inbox</span>
          </div>
          <span className="bg-red-600 text-white text-[10px] font-bold px-1.5 py-0 rounded-full">
            {inboxActiveCount}
          </span>
        </button>
        <InboxPopup
          isOpen={isInboxOpen}
          onClose={() => setIsInboxOpen(false)}
          anchorRef={inboxButtonRef}
          placement="right"
        />
        <button
          ref={profileButtonRef}
          onClick={() => setIsProfileOpen(!isProfileOpen)}
          className="flex items-center gap-2 w-full px-2 py-1.5 rounded-sm hover:bg-slate-100 text-slate-700 transition-colors mt-1 font-medium"
        >
          <UserAvatar size="xs" />
          <span className="text-[14px]">김상민</span>
        </button>
        <UserProfilePopup
          isOpen={isProfileOpen}
          onClose={() => setIsProfileOpen(false)}
          anchorRef={profileButtonRef}
          placement="above"
        />
        <div
          className="text-left font-bold text-slate-800 text-2xl tracking-tight px-2 mt-4 cursor-pointer hover:text-blue-600 transition-colors"
          onClick={() => navigate('/workspaces')}
        >DocGraph</div>
        <div className="text-[10px] text-slate-400 px-2 mt-0.5 pb-2">DocGraph는 실수할 수 있습니다.</div>
      </div>

    </nav>
  );
};