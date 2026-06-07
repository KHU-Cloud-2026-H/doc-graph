import { useEffect } from "react";
import { Outlet, useParams } from "react-router-dom";
import { Sidebar } from "./Sidebar";
import { useAppStore } from "../store";
import { useWorkspaces } from "../hooks/useWorkspaces";
import { useProjects } from "../hooks/useProjects";

export const Layout = () => {
  const { isSidebarOpen, setWorkspaces, setProjects } = useAppStore();
  const { workspaceId } = useParams<{ workspaceId: string }>();
  const numericWorkspaceId = workspaceId ? Number(workspaceId) : undefined;

  const { workspaces } = useWorkspaces();
  const { projects } = useProjects(numericWorkspaceId);

  useEffect(() => {
    if (workspaces.length > 0) setWorkspaces(workspaces);
  }, [workspaces, setWorkspaces]);

  useEffect(() => {
    if (projects.length > 0 && numericWorkspaceId) {
      setProjects(projects.map((p) => ({
        id: p.id,
        name: p.name,
        workspaceId: numericWorkspaceId,
        notionRootPageEmoji: p.rootPageIcon?.type === 'EMOJI' ? p.rootPageIcon.value : null,
      })));
    }
  }, [projects, numericWorkspaceId, setProjects]);

  return (
    <div className="flex h-screen overflow-hidden text-slate-900 bg-slate-50 font-sans">
      <Sidebar />
      <div className={`flex-1 flex flex-col min-w-0 transition-all duration-300 ${isSidebarOpen ? 'md:ml-[288px]' : ''}`}>
        <Outlet />
      </div>
    </div>
  );
};
