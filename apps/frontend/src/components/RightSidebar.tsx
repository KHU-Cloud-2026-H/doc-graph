import { X, Calendar, User, RefreshCw, Lightbulb, ChevronUp } from "lucide-react";
import { Link } from "react-router-dom";
import type { DocumentNode } from "../store";

export const RightSidebar = ({ document: doc, onClose }: { document: DocumentNode, onClose: () => void }) => {
  return (
    <aside className="w-[320px] bg-white border-l border-slate-200 h-screen hidden lg:flex flex-col shrink-0 shadow-[-4px_0_15px_-5px_rgba(0,0,0,0.05)] z-30 font-sans">
      <div className="border-b border-slate-200 flex items-center gap-2 h-14 px-4 shrink-0">
        <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="text-slate-400">
          <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
          <path d="M22 4L12 14.01l-3-3"/>
        </svg>
        <h3 className="font-semibold text-sm text-slate-900 flex items-center gap-2">
          INTEGRITY ISSUES
          {doc.issues && doc.issues.length > 0 && (
            <span className="bg-red-600 text-white rounded-full w-5 h-5 flex items-center justify-center text-[11px] font-bold">
              {doc.issues.length}
            </span>
          )}
        </h3>
        <button 
          onClick={onClose}
          className="ml-auto p-1 rounded-full hover:bg-slate-100 text-slate-400 hover:text-slate-900 transition-colors flex items-center justify-center"
        >
          <X className="w-5 h-5" />
        </button>
      </div>

      <div className="flex-1 overflow-y-auto">
        {/* Mini Graph Area */}
        <div className="h-[250px] bg-slate-50 border-b border-slate-200 relative flex items-center justify-center p-4">
          <div className="relative w-full h-full">
            <svg className="absolute inset-0 w-full h-full pointer-events-none z-0">
              <line stroke="#dc2626" strokeWidth="2" x1="50%" x2="20%" y1="50%" y2="20%"></line>
              <line stroke="#cbd5e1" strokeWidth="1.5" x1="50%" x2="80%" y1="50%" y2="30%"></line>
              <line stroke="#cbd5e1" strokeDasharray="4" strokeWidth="1.5" x1="50%" x2="70%" y1="50%" y2="80%"></line>
            </svg>
            
            {/* Center Node */}
            <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-5 h-5 rounded-full bg-blue-600 ring-4 ring-blue-600/10 z-20 shadow-lg cursor-pointer"></div>
            <div className="absolute top-1/2 left-1/2 translate-x-4 translate-y-4 text-[11px] font-bold text-blue-600 z-20 leading-tight w-24">
              {doc.title}
            </div>

            {/* Conflict Node */}
            <div className="absolute top-[20%] left-[20%] -translate-x-1/2 -translate-y-1/2 w-4 h-4 rounded-full bg-red-100 border-2 border-red-600 z-10 cursor-pointer shadow-sm hover:scale-110 transition-transform"></div>
            <div className="absolute top-[20%] left-[20%] -translate-x-1/2 -translate-y-6 text-[10px] font-semibold text-red-600 whitespace-nowrap z-10 bg-white/80 px-1 rounded">
              {doc.issues?.[0]?.targetDocumentTitle.substring(0, 15)}...
            </div>

            {/* Parent Node */}
            <div className="absolute top-[30%] left-[80%] -translate-x-1/2 -translate-y-1/2 w-3.5 h-3.5 rounded-full bg-white border-2 border-slate-300 z-10 cursor-pointer hover:border-blue-600 transition-all shadow-sm"></div>
            <div className="absolute top-[30%] left-[80%] translate-x-3 -translate-y-3 text-[10px] font-medium text-slate-500 whitespace-nowrap z-10 bg-white/80 px-1 rounded">
              WorkSync<br/>Project
            </div>

            {/* Child Node */}
            <div className="absolute top-[80%] left-[70%] -translate-x-1/2 -translate-y-1/2 w-3.5 h-3.5 rounded-full bg-white border-2 border-slate-300 z-10 cursor-pointer hover:border-blue-600 transition-all shadow-sm"></div>
            <div className="absolute top-[80%] left-[70%] -translate-x-1/2 translate-y-3 text-[10px] font-medium text-slate-500 whitespace-nowrap z-10 text-center bg-white/80 px-1 rounded">
              Other<br/>Docs
            </div>
          </div>
        </div>

        {/* Document Details */}
        <div className="p-4 space-y-6">
          <div>
            <div className="text-xs font-semibold text-slate-400 uppercase tracking-wider mb-3">Properties</div>
            <div className="space-y-3">
              <div className="flex items-center justify-between text-sm">
                <span className="text-slate-500 flex items-center gap-2"><Calendar className="w-4 h-4" /> Last Edited</span>
                <span className="text-slate-900">Today, 14:30</span>
              </div>
              <div className="flex items-center justify-between text-sm">
                <span className="text-slate-500 flex items-center gap-2"><User className="w-4 h-4" /> Edited By</span>
                <div className="flex items-center gap-1.5 bg-slate-100 px-2 py-0.5 rounded text-slate-900">
                  <img alt="Author" className="w-4 h-4 rounded-full object-cover" src="https://images.unsplash.com/photo-1599566150163-29194dcaad36?q=80&w=300&auto=format&fit=crop" />
                  <span className="text-xs">서동현</span>
                </div>
              </div>
              <div className="flex items-center justify-between text-sm">
                <span className="text-slate-500 flex items-center gap-2">
                  <span className="w-4 h-4 text-center text-red-500 font-bold">!</span> Integrity
                </span>
                <div className="flex items-center gap-1 text-xs text-red-600 font-medium">
                  <RefreshCw className="w-3.5 h-3.5 cursor-pointer hover:text-red-800" />
                  <span>{doc.issues?.length} Issues Found</span>
                </div>
              </div>
            </div>
          </div>

          <div>
            <div className="text-xs font-semibold text-slate-400 uppercase tracking-wider mb-3">Integrity Issues</div>
            <ul className="space-y-4">
              {doc.issues?.map((issue, index) => (
                <li key={index} className="bg-white p-4 rounded-xl border border-slate-200 shadow-sm">
                  <div className="flex flex-col gap-4">
                    <div className="flex items-center justify-between cursor-pointer group">
                      <span className="text-sm font-semibold text-slate-900">{issue.title}</span>
                      <ChevronUp className="w-5 h-5 text-slate-400 group-hover:text-slate-900 transition-colors" />
                    </div>

                    <div className="space-y-4">
                      <div className="space-y-3">
                        <div className="text-xs leading-relaxed">
                          <span className="text-slate-900 font-semibold block mb-1">현재 작성된 내용</span>
                          <p className="text-slate-600 bg-red-50 p-2 border-l-2 border-red-400">"{issue.currentText}"</p>
                        </div>
                        <div className="text-xs leading-relaxed">
                          <span className="text-slate-900 font-semibold mb-1 inline-block">충돌 원인 파악</span>
                          <span className="inline-block ml-2 px-1.5 py-0.5 text-[9px] font-bold text-white bg-gradient-to-r from-blue-600 to-purple-600 leading-none align-middle rounded-full relative -top-[1px]">AI</span>
                          <p className="text-slate-600 mt-1">{issue.description}</p>
                        </div>
                      </div>

                      <div className="space-y-1">
                        <div className="text-[11px] font-semibold text-slate-900 uppercase tracking-wider">충돌 대상 문서 정보</div>
                        <div className="text-xs text-slate-900 flex items-center gap-1">
                          Target: <Link to={`/w/sample-workspace/docs/${issue.targetDocumentId}`} className="text-blue-600 hover:underline flex items-center gap-0.5">🔗 {issue.targetDocumentTitle}</Link>
                        </div>
                      </div>

                      <div className="bg-blue-50 p-3 rounded-lg border border-blue-200 space-y-2">
                        <div className="flex items-center gap-1.5 text-[11px] font-bold text-blue-700">
                          <Lightbulb className="w-4 h-4" />
                          DocGraph AI 제안
                        </div>
                        <p className="text-xs text-slate-900 font-medium">{issue.aiSuggestion}</p>
                        <div className="text-[11px] text-slate-600 leading-relaxed">
                          <span className="opacity-60 block">수정될 텍스트 초안:</span>
                          <span className="bg-green-50 px-1 border-l-2 border-green-400 block mt-1 py-1 text-slate-800">"{issue.suggestedText}"</span>
                        </div>
                      </div>

                      <div className="flex items-center justify-end gap-2 pt-1">
                        <button className="px-3 py-1.5 text-xs font-semibold text-slate-600 hover:bg-slate-100 rounded transition-colors">Ignore</button>
                        <button className="px-3 py-1.5 text-xs font-bold text-white bg-blue-600 hover:bg-blue-700 rounded shadow-sm transition-colors">Apply Fix</button>
                      </div>
                    </div>
                  </div>
                </li>
              ))}
            </ul>
          </div>
        </div>
      </div>
    </aside>
  );
};