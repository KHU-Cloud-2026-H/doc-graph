import { Outlet } from "react-router-dom";
import { Sidebar } from "./Sidebar";
import { useAppStore } from "../store";

export const Layout = () => {
  const { isSidebarOpen } = useAppStore();

  return (
    <div className="flex h-screen overflow-hidden text-slate-900 bg-slate-50 font-sans">
      <Sidebar />
      <div className={`flex-1 flex flex-col min-w-0 transition-all duration-300 ${isSidebarOpen ? 'md:ml-[288px]' : ''}`}>
        <Outlet />
      </div>
    </div>
  );
};