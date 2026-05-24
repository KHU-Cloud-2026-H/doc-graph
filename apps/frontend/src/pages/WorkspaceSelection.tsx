import { useNavigate } from "react-router-dom";
import { Plus } from "lucide-react";
import { useAppStore } from '../store';
import { TopAppBar } from '../components/TopAppBar';
import { InboxList, inboxNotifications } from '../components/InboxList';

export const WorkspaceSelection = () => {
  const navigate = useNavigate();
  const workspaces = useAppStore((s) => s.workspaces);

  return (
    <div className="bg-slate-50 text-slate-900 min-h-screen flex flex-col font-sans">
      <TopAppBar centerLabel="artboi@khu.ac.kr" />

      <main className="flex-1 w-full max-w-[1200px] mx-auto px-6 py-10">
        <div className="flex gap-16">
          {/* Left: Workspaces */}
          <div className="flex-1 max-w-[calc(100%-430px)]">
            <div className="flex justify-between items-end mb-4">
              <div className="flex items-center">
                <h1 className="text-3xl font-bold text-slate-900">Workspaces</h1>
                <span className="bg-slate-200 text-slate-600 text-xs py-0.5 px-2 rounded-full font-medium ml-2">
                  {workspaces.length}
                </span>
              </div>
              <button className="flex items-center gap-2 bg-blue-600 text-white px-5 py-2.5 rounded hover:bg-blue-700 transition-colors font-medium text-sm shadow-sm">
                <Plus className="w-4 h-4" />
                새 워크스페이스 연동
              </button>
            </div>
            <p className="text-slate-500 text-sm mb-6">Notion에서 연동된 모든 워크스페이스 목록입니다.</p>
            <div className="space-y-3">
              {workspaces.map((ws) => (
                <div
                  key={ws.id}
                  onClick={() => navigate(`/w/${ws.id}`)}
                  className="bg-white border border-slate-200 rounded-xl p-5 hover:shadow-md hover:border-blue-300 transition-all cursor-pointer"
                >
                  <div className="flex items-center gap-4">
                    <div className="bg-blue-100 text-blue-700 rounded-lg w-14 h-14 flex items-center justify-center text-2xl font-bold shrink-0">
                      {ws.name[0]}
                    </div>
                    <span className="text-2xl font-bold text-slate-900">{ws.name}</span>
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Right: Inbox */}
          <div className="w-[400px] shrink-0">
            <div className="flex items-center gap-2 mb-2">
              <h1 className="text-3xl font-bold text-slate-900">Inbox</h1>
              <span className="bg-red-600 text-white text-xs font-bold px-1.5 py-0.5 rounded-full">
                {inboxNotifications.length}
              </span>
            </div>
            <p className="text-sm text-slate-500 mb-4">
              내가 담당하는 모든 프로젝트에서 발생한<br />
              정합성 충돌 알림입니다.
            </p>
            <div className="bg-white border border-slate-200 rounded-xl overflow-hidden">
              <InboxList />
            </div>
          </div>
        </div>
      </main>

      <footer className="w-full mt-auto bg-slate-100 border-t border-slate-200">
        <div className="max-w-[1200px] mx-auto py-8 px-6 flex flex-col md:flex-row justify-between items-center">
          <div className="font-bold text-slate-500 text-sm mb-4 md:mb-0">© 2026 DocGraph.</div>
          <div className="flex gap-6">
            <a className="text-slate-500 hover:text-blue-600 text-sm transition-colors duration-200" href="#">개인정보 처리방침</a>
            <a className="text-slate-500 hover:text-blue-600 text-sm transition-colors duration-200" href="#">이용약관</a>
            <a className="text-slate-500 hover:text-blue-600 text-sm transition-colors duration-200" href="#">고객센터</a>
          </div>
        </div>
      </footer>
    </div>
  );
};
