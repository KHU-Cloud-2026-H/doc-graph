import { Link } from "react-router-dom";
import { Settings, FileText, Star, ChevronDown, Plus, RefreshCw, Search } from "lucide-react";
import { useState } from "react";

export const WorkspaceSelection = () => {
  const [isFavoritesOpen, setIsFavoritesOpen] = useState(true);
  const [isConnectedOpen, setIsConnectedOpen] = useState(true);
  const [isUnconnectedOpen, setIsUnconnectedOpen] = useState(true);

  return (
    <div className="bg-slate-50 text-slate-900 min-h-screen flex flex-col font-sans">
      {/* TopAppBar */}
      <header className="flex justify-between items-center w-full px-6 h-16 sticky top-0 z-50 bg-white border-b border-slate-200 antialiased">
        <div className="flex items-center gap-6">
          <div className="font-bold tracking-tight text-slate-900 text-2xl">
            DocGraph
          </div>
        </div>
        <div className="flex justify-center max-w-md mx-4 absolute left-1/2 -translate-x-1/2">
          <button className="flex items-center gap-2 bg-slate-50 hover:bg-slate-100 px-4 py-2 rounded-full border border-slate-200 transition-colors text-sm font-medium">
            <img alt="Notion logo" className="w-5 h-5 object-contain opacity-80" src="https://upload.wikimedia.org/wikipedia/commons/thumb/e/e9/Notion-logo.svg/3840px-Notion-logo.svg.png" />
            <span className="text-slate-700">artboi@khu.ac.kr</span>
            <ChevronDown className="w-4 h-4 text-slate-400" />
          </button>
        </div>
        <div className="flex items-center text-slate-600 font-medium gap-2">
          <button className="hover:bg-slate-100 transition-colors duration-200 p-2 rounded-full hover:text-slate-900 text-slate-600 flex items-center justify-center">
            <Settings className="w-5 h-5" />
          </button>
          <button className="hover:bg-slate-100 transition-colors duration-200 p-2 rounded-full hover:text-slate-900 text-slate-600 flex items-center justify-center relative">
            <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9"/><path d="M10.3 21a1.94 1.94 0 0 0 3.4 0"/></svg>
            <span className="absolute top-1.5 right-1.5 w-2 h-2 bg-blue-600 rounded-full"></span>
          </button>
          <button className="hover:bg-slate-100 transition-colors duration-200 p-1 rounded-full flex items-center justify-center border border-slate-200">
            <img alt="User profile" className="w-8 h-8 rounded-full object-cover" src="https://images.unsplash.com/photo-1599566150163-29194dcaad36?q=80&w=300&auto=format&fit=crop" />
          </button>
        </div>
      </header>

      {/* Main Content */}
      <main className="flex-1 w-full max-w-[1200px] mx-auto px-6 py-10">
        <div className="flex justify-between items-end mb-10">
          <div>
            <h1 className="text-3xl font-bold leading-[1.2] tracking-tight text-slate-900 mb-2">Workspaces</h1>
            <p className="text-base text-slate-500">Notion에서 연동된 모든 워크스페이스 목록입니다.</p>
          </div>
          <div className="flex items-center gap-2">
            <button className="flex items-center gap-2 bg-white text-slate-700 border border-slate-200 px-5 py-2.5 rounded hover:bg-slate-50 transition-colors font-medium text-sm shadow-sm">
              <RefreshCw className="w-4 h-4" />
              새로고침
            </button>
            <button className="flex items-center gap-2 bg-blue-600 text-white px-5 py-2.5 rounded hover:bg-blue-700 transition-colors font-medium text-sm shadow-sm">
              <Plus className="w-4 h-4" />
              새 워크스페이스 연동
            </button>
          </div>
        </div>

        {/* Favorites Section */}
        <section className="mb-12">
          <div className="flex items-center gap-2 mb-6 cursor-pointer group w-fit" onClick={() => setIsFavoritesOpen(!isFavoritesOpen)}>
            <ChevronDown className={`w-5 h-5 text-slate-400 transition-transform ${isFavoritesOpen ? "" : "-rotate-90"}`} />
            <h2 className="text-xl font-bold text-slate-800">즐겨찾기</h2>
          </div>
          
          {isFavoritesOpen && (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              {/* TODO: 실제 workspaceId로 교체 필요 (API 연동 시) */}
              <Link to="/w/sample-workspace/graph" className="bg-white border border-slate-200 rounded-xl p-5 hover:shadow-md hover:border-blue-300 transition-all cursor-pointer group flex flex-col relative block ring-2 ring-blue-500/20">
                <button className="absolute top-4 right-4 text-blue-500 hover:text-blue-700 transition-colors z-10">
                  <Star className="w-5 h-5 fill-current" />
                </button>
                <div className="w-10 h-10 flex items-center justify-center bg-blue-100 text-blue-700 rounded-lg text-lg font-bold mb-4 group-hover:scale-110 transition-transform origin-bottom-left">
                  엔
                </div>
                <h3 className="text-base font-bold text-slate-900 mb-1 flex items-center">
                  엔터프라이즈 근태관리 B2B SaaS 프로젝트
                </h3>
                <p className="text-sm text-slate-500 mb-4 truncate">4명의 멤버</p>
                <div className="mt-auto flex items-center justify-between text-xs font-medium text-slate-400 border-t border-slate-100 pt-3">
                  <span className="flex items-center gap-1.5"><FileText className="w-3.5 h-3.5" /> 25개의 소스</span>
                  <span>1분 전</span>
                </div>
              </Link>
            </div>
          )}
        </section>

        {/* Connected Workspaces Section */}
        <section className="mb-12">
          <div className="flex items-center justify-between mb-6">
            <div className="flex items-center gap-2 cursor-pointer group" onClick={() => setIsConnectedOpen(!isConnectedOpen)}>
              <ChevronDown className={`w-5 h-5 text-slate-400 transition-transform ${isConnectedOpen ? "" : "-rotate-90"}`} />
              <h2 className="text-xl font-bold text-slate-800 flex items-center gap-2">
                연동됨 <span className="bg-slate-200 text-slate-600 text-xs py-0.5 px-2 rounded-full font-medium">1</span>
              </h2>
            </div>
            <div className="relative w-64">
              <Search className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
              <input className="w-full bg-white border border-slate-200 rounded-md pl-9 pr-3 py-1.5 text-sm text-slate-900 focus:outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500 transition-colors placeholder:text-slate-400 shadow-sm" placeholder="워크스페이스 검색" type="text" />
            </div>
          </div>
          
          {isConnectedOpen && (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
              {/* TODO: 실제 workspaceId로 교체 필요 (API 연동 시) */}
              <Link to="/w/sample-workspace/graph" className="bg-white border border-slate-200 rounded-xl p-5 hover:shadow-md hover:border-blue-300 transition-all cursor-pointer group flex flex-col relative block">
                <button className="absolute top-4 right-4 text-slate-300 hover:text-blue-500 transition-colors z-10">
                  <Star className="w-5 h-5" />
                </button>
                <div className="text-[32px] leading-none mb-3 group-hover:scale-110 transition-transform origin-bottom-left">🕸️</div>
                <h3 className="text-base font-bold text-slate-900 mb-1 flex items-center">
                  DocGraph Team 
                  <span className="inline-flex items-center justify-center w-5 h-5 ml-2 text-[10px] font-bold text-white bg-red-500 rounded-full">2</span>
                </h3>
                <p className="text-sm text-slate-500 mb-4">7명의 멤버</p>
                <div className="mt-auto flex items-center justify-between text-xs font-medium text-slate-400 border-t border-slate-100 pt-3">
                  <span className="flex items-center gap-1.5"><FileText className="w-3.5 h-3.5" /> 18개의 소스</span>
                  <span>5시간 전</span>
                </div>
              </Link>
            </div>
          )}
        </section>

        {/* Unconnected Workspaces Section */}
        <section>
          <div className="flex items-center gap-2 mb-6 cursor-pointer group w-fit" onClick={() => setIsUnconnectedOpen(!isUnconnectedOpen)}>
            <ChevronDown className={`w-5 h-5 text-slate-400 transition-transform ${isUnconnectedOpen ? "" : "-rotate-90"}`} />
            <h2 className="text-xl font-bold text-slate-800 flex items-center gap-2">
              연동되지 않음 <span className="bg-slate-200 text-slate-600 text-xs py-0.5 px-2 rounded-full font-medium">2</span>
            </h2>
          </div>
          
          {isUnconnectedOpen && (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 xl:grid-cols-5 gap-4">
              <div className="bg-white border border-slate-200 border-dashed rounded-xl p-4 hover:shadow-sm hover:border-blue-300 transition-all cursor-pointer group flex flex-col relative opacity-80 hover:opacity-100">
                <div className="text-[28px] leading-none mb-2 grayscale group-hover:grayscale-0 transition-all">🧪</div>
                <h3 className="text-sm font-bold text-slate-700 mb-1 line-clamp-1">상민의 워크스페이스</h3>
                <p className="text-xs text-slate-400 mb-3">1명의 멤버</p>
                <button className="mt-auto w-full py-1.5 bg-slate-50 hover:bg-blue-50 text-slate-600 hover:text-blue-600 text-xs font-bold rounded border border-slate-200 hover:border-blue-200 transition-colors">
                  연동하기
                </button>
              </div>

              <div className="bg-white border border-slate-200 border-dashed rounded-xl p-4 hover:shadow-sm hover:border-blue-300 transition-all cursor-pointer group flex flex-col relative opacity-80 hover:opacity-100">
                <div className="text-[28px] leading-none mb-2 grayscale group-hover:grayscale-0 transition-all">👨‍💻</div>
                <h3 className="text-sm font-bold text-slate-700 mb-1 line-clamp-1">소프트웨어공학 13조</h3>
                <p className="text-xs text-slate-400 mb-3">5명의 멤버</p>
                <button className="mt-auto w-full py-1.5 bg-slate-50 hover:bg-blue-50 text-slate-600 hover:text-blue-600 text-xs font-bold rounded border border-slate-200 hover:border-blue-200 transition-colors">
                  연동하기
                </button>
              </div>
            </div>
          )}
        </section>
      </main>

      {/* Footer */}
      <footer className="w-full mt-auto bg-slate-100 border-t border-slate-200">
        <div className="max-w-[1200px] mx-auto py-8 px-6 flex flex-col md:flex-row justify-between items-center">
          <div className="font-bold text-slate-500 text-sm mb-4 md:mb-0">© 2026 DocGraph. </div>
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