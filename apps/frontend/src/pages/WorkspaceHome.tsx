import { useNavigate, useParams } from 'react-router-dom';
import { useAppStore } from '../store';
import { Settings, Plus } from 'lucide-react';
import { TopAppBar } from '../components/TopAppBar';

const WorkspaceHome = () => {
  const { workspaceId } = useParams<{ workspaceId: string }>();
  const navigate = useNavigate();
  const { workspaces, projects } = useAppStore();

  const workspace = workspaces.find((w) => w.id === Number(workspaceId));
  const workspaceProjects = projects.filter((p) => p.workspaceId === Number(workspaceId));

  return (
    <div className="bg-slate-50 text-slate-900 min-h-screen flex flex-col font-sans">
      <TopAppBar centerLabel={workspace?.name ?? '워크스페이스'} />

      <main className="flex-1 w-full max-w-[1200px] mx-auto px-6 py-10">

        {/* Section 1: 워크스페이스 아이콘 + 이름 */}
        <div className="flex items-center gap-4 mb-3">
          <div className="w-16 h-16 flex items-center justify-center bg-blue-100 text-blue-700 rounded-xl text-3xl font-bold shrink-0">
            {workspace?.name[0] ?? 'W'}
          </div>
          <h1 className="text-3xl font-bold leading-[1.2] tracking-tight text-slate-900">
            {workspace?.name ?? '워크스페이스'}
          </h1>
        </div>

        {/* Section 2: 워크스페이스 홈 + 관리 버튼 */}
        <div className="flex items-center gap-3 mb-12">
          <span className="text-base text-slate-500 font-medium">워크스페이스 홈</span>
          <button
            onClick={() => navigate(`/w/${workspaceId}/settings`)}
            className="flex items-center gap-1.5 bg-white text-slate-600 border border-slate-200 px-3 py-1.5 rounded-md hover:bg-slate-50 transition-colors font-medium text-sm shadow-sm"
          >
            <Settings className="w-3.5 h-3.5" />
            워크스페이스 관리
          </button>
        </div>

        {/* Section 3 & 4: 프로젝트 목록 */}
        <section>
          <div className="flex items-center justify-between mb-6">
            <h2 className="text-xl font-bold text-slate-800">프로젝트 목록</h2>
            <button
              onClick={() => navigate(`/w/${workspaceId}/new-project`)}
              className="flex items-center gap-2 bg-blue-600 text-white px-5 py-2.5 rounded hover:bg-blue-700 transition-colors font-medium text-sm shadow-sm"
            >
              <Plus className="w-4 h-4" />
              새 프로젝트
            </button>
          </div>

          <div className="grid grid-cols-3 gap-6">
            {workspaceProjects.map((project) => (
              <div
                key={project.id}
                onClick={() => navigate(`/w/${workspaceId}/p/${project.id}/graph`)}
                className="bg-white border border-slate-200 rounded-xl p-5 hover:shadow-md hover:border-blue-300 transition-all cursor-pointer group flex flex-col"
              >
                <div className="w-10 h-10 flex items-center justify-center bg-blue-100 text-blue-700 rounded-lg text-lg font-bold mb-4 group-hover:scale-110 transition-transform origin-bottom-left">
                  {project.name[0]}
                </div>
                <h3 className="text-lg font-bold text-slate-900">{project.name}</h3>
              </div>
            ))}

            <div
              onClick={() => navigate(`/w/${workspaceId}/new-project`)}
              className="bg-white border border-dashed border-slate-300 rounded-xl p-5 hover:shadow-md hover:border-blue-300 transition-all cursor-pointer flex flex-col items-center justify-center min-h-[120px] gap-2"
            >
              <Plus className="w-8 h-8 text-slate-400" />
              <span className="text-sm text-slate-500">새 프로젝트</span>
            </div>
          </div>
        </section>
      </main>

      <footer className="w-full mt-auto bg-slate-100 border-t border-slate-200">
        <div className="max-w-[1200px] mx-auto py-8 px-6 flex flex-col md:flex-row justify-between items-center">
          <div className="font-bold text-slate-500 text-sm mb-4 md:mb-0">© 2026 DocGraph. </div>
          <div className="flex gap-6">
            <a className="text-slate-500 hover:text-blue-600 text-sm transition-colors duration-200" href="#">
              개인정보 처리방침
            </a>
            <a className="text-slate-500 hover:text-blue-600 text-sm transition-colors duration-200" href="#">
              이용약관
            </a>
            <a className="text-slate-500 hover:text-blue-600 text-sm transition-colors duration-200" href="#">
              고객센터
            </a>
          </div>
        </div>
      </footer>
    </div>
  );
};

export default WorkspaceHome;
