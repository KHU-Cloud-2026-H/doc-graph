import { ExternalLink, CheckCircle, Check } from "lucide-react";
import { useState, useEffect } from "react";
import { useParams, useSearchParams, useNavigate } from "react-router-dom";
import { RightSidebar } from "../components/RightSidebar";
import { useAppStore } from '../store';
import type { IntegrityIssue } from '../store';

export const DocumentView = () => {
  const { docId: id } = useParams();
  const [searchParams, setSearchParams] = useSearchParams();
  const navigate = useNavigate();
  const workspace = useAppStore((state) => state.workspace);
  const documents = useAppStore((state) => state.documents);

  // Flatten documents tree to find the active one
  const flattenDocs = (docs: any[]): any[] => {
    return docs.reduce((acc, doc) => {
      if (doc.isFolder) {
        return [...acc, doc, ...flattenDocs(doc.children || [])];
      }
      return [...acc, doc];
    }, []);
  };
  
  const allDocs = flattenDocs(documents);
  const activeDoc = allDocs.find((d) => d.id === id) || allDocs[0];
  
  const [showRightSidebar, setShowRightSidebar] = useState(false);

  useEffect(() => {
    window.scrollTo(0, 0);
    if (searchParams.get("openIssues") === "true") {
      setShowRightSidebar(true);
      // Clean up the URL parameter without triggering a reload
      searchParams.delete("openIssues");
      setSearchParams(searchParams, { replace: true });
    } else {
      setShowRightSidebar(false);
    }
  }, [activeDoc?.id, searchParams]);

  let processedHtml = activeDoc?.contentHtml || '';

  // Transform Notion-like child page links
  processedHtml = processedHtml.replaceAll(
    'class="flex items-center gap-2 p-2 hover:bg-slate-50 rounded border border-slate-200 text-blue-600 transition-colors"',
    'class="flex items-center w-fit gap-2 px-1.5 py-1 hover:bg-[#efefef] rounded-[4px] text-[#37352f] transition-colors cursor-pointer text-[15px] border-none outline-none"'
  );

  if (activeDoc?.hasIssue && activeDoc.issues) {
    activeDoc.issues.forEach((issue: IntegrityIssue) => {
      if (showRightSidebar) {
        const diffHtml = `
          <div class="block border border-slate-200 rounded-md my-2 shadow-sm text-sm overflow-hidden font-sans bg-white">
            <div class="flex items-start gap-2 bg-red-50 text-slate-700 p-2.5 border-b border-slate-200">
              <span class="bg-red-600 text-white rounded-full flex items-center justify-center w-4 h-4 text-[12px] font-bold shrink-0 mt-0.5">-</span>
              <span class="line-through decoration-red-500 whitespace-pre-wrap leading-relaxed">${issue.currentText}</span>
            </div>
            <div class="flex items-start gap-2 bg-green-50 text-slate-700 p-2.5">
              <span class="bg-green-600 text-white rounded-full flex items-center justify-center w-4 h-4 text-[12px] font-bold shrink-0 mt-0.5">+</span>
              <span class="whitespace-pre-wrap leading-relaxed">${issue.suggestedText}</span>
            </div>
          </div>
        `;
        
        const exactSpan1 = `<span class="bg-red-50 text-red-700 px-1 rounded">${issue.currentText}</span>`;
        const exactSpan2 = `<span class="bg-red-200 text-red-900 px-1">${issue.currentText}</span>`;
        const exactSpan3 = `<span class="bg-red-50 text-red-700 px-1 rounded">'오전 9시 10분 59초'까지 시스템에 기록된 출근 데이터는 정상 출근(PRESENT)으로 인정한다. 정확히 09:11:00의 기록부터 지각(LATE) 상태로 산정되어 리포트에 기록된다.</span>`;
        
        if (processedHtml.includes(exactSpan1)) processedHtml = processedHtml.replace(exactSpan1, diffHtml);
        else if (processedHtml.includes(exactSpan2)) processedHtml = processedHtml.replace(exactSpan2, diffHtml);
        else if (issue.id === 'err-8' && processedHtml.includes(exactSpan3)) processedHtml = processedHtml.replace(exactSpan3, diffHtml);
        else {
           const fallbackSpan = new RegExp(`<span[^>]*bg-red[^>]*>${issue.currentText.replace(/[-\/\\^$*+?.()|[\]{}]/g, '\\$&')}</span>`, 'g');
           processedHtml = processedHtml.replace(fallbackSpan, diffHtml);
        }
      } else {
        const clickableHtml = `<span class="issue-target bg-red-50 text-red-700 px-1 rounded border-b border-red-400 cursor-pointer hover:bg-red-100 transition-colors" title="클릭하여 이슈 확인">${issue.currentText}</span>`;
        
        const exactSpan1 = `<span class="bg-red-50 text-red-700 px-1 rounded">${issue.currentText}</span>`;
        const exactSpan2 = `<span class="bg-red-200 text-red-900 px-1">${issue.currentText}</span>`;
        const exactSpan3 = `<span class="bg-red-50 text-red-700 px-1 rounded">'오전 9시 10분 59초'까지 시스템에 기록된 출근 데이터는 정상 출근(PRESENT)으로 인정한다. 정확히 09:11:00의 기록부터 지각(LATE) 상태로 산정되어 리포트에 기록된다.</span>`;
        
        if (processedHtml.includes(exactSpan1)) processedHtml = processedHtml.replace(exactSpan1, clickableHtml);
        else if (processedHtml.includes(exactSpan2)) processedHtml = processedHtml.replace(exactSpan2, clickableHtml);
        else if (issue.id === 'err-8' && processedHtml.includes(exactSpan3)) processedHtml = processedHtml.replace(exactSpan3, clickableHtml);
        else {
           const fallbackSpan = new RegExp(`<span[^>]*bg-red[^>]*>${issue.currentText.replace(/[-\/\\^$*+?.()|[\]{}]/g, '\\$&')}</span>`, 'g');
           processedHtml = processedHtml.replace(fallbackSpan, clickableHtml);
        }
      }
    });
  }

  return (
    <main className="flex-1 flex h-screen overflow-hidden bg-white font-sans">
      <div className="flex-1 flex flex-col h-full relative min-w-0">
        <header className="h-14 flex items-center justify-between px-6 border-b border-slate-200 bg-white shrink-0">
          <div className="flex items-center text-sm text-slate-500 flex-1">
            <span className="px-2 py-1 rounded hover:bg-slate-100 cursor-pointer transition-colors hover:text-slate-900">{workspace}</span>
            <span className="mx-1 text-[14px] opacity-40">/</span>
            <span className="px-2 py-1 rounded hover:bg-slate-100 cursor-pointer transition-colors hover:text-slate-900">팀스페이스</span>
            <span className="mx-1 text-[14px] opacity-40">/</span>
            <span className="px-2 py-1 rounded hover:bg-slate-100 cursor-pointer transition-colors text-slate-900 font-medium truncate max-w-[300px]">{activeDoc?.title}</span>
            <div className="ml-3 flex items-center gap-1 px-2 py-0.5 bg-slate-100 rounded text-xs text-slate-500">
              <CheckCircle className="w-3.5 h-3.5" />
              Saved
            </div>
          </div>
          <button className="flex items-center gap-1.5 px-3 py-1 border border-slate-200 rounded hover:bg-slate-50 transition-colors text-xs font-medium text-slate-600">
            <ExternalLink className="w-4 h-4" />
            Edit in Notion
          </button>
        </header>

        <div className="flex-1 overflow-y-auto px-8 py-12 flex justify-center">
          <div className="w-full max-w-[800px]">
            {!activeDoc?.isFolder && (
              <>
                <h1 className="text-3xl font-bold text-slate-900 mb-8 outline-none tracking-tight">
                  {activeDoc?.title}
                </h1>

                <div className="mb-8 space-y-3 text-sm">
                  <div className="flex items-center">
                    <span className="w-32 text-slate-500">Status</span>
                    <span className="bg-blue-50 text-blue-700 px-2 py-0.5 rounded text-xs font-medium">🏃 진행 중</span>
                  </div>
                  <div className="flex items-center">
                    <span className="w-32 text-slate-500">Assignee</span>
                    <div className="flex items-center gap-2">
                      <div className="flex items-center gap-1.5 bg-slate-100 px-2 py-0.5 rounded text-slate-900">
                        <img alt="Author" className="w-4 h-4 rounded-full object-cover" src="https://images.unsplash.com/photo-1599566150163-29194dcaad36?q=80&w=300&auto=format&fit=crop" />
                        <span className="text-sm">서동현</span>
                      </div>
                    </div>
                  </div>
                  <div className="flex items-center">
                    <span className="w-32 text-slate-500">Date Created</span>
                    <span className="text-slate-900">2026년 4월 10일</span>
                  </div>
                </div>

                <hr className="border-slate-200 mb-8"/>
              </>
            )}

            <div 
              className="text-base text-slate-700 space-y-6 outline-none leading-relaxed"
              onClick={(e) => {
                const target = e.target as HTMLElement;
                const link = target.closest('a');
                const href = link?.getAttribute('href');
                if (href && (href.startsWith('/docs/') || href.startsWith('/w/sample-workspace/docs/'))) {
                  e.preventDefault();
                  navigate(href);
                  return;
                }
                if (target.closest('.issue-target')) {
                  setShowRightSidebar(true);
                }
              }}
            >
              {activeDoc?.contentHtml ? (
                <div dangerouslySetInnerHTML={{ __html: processedHtml }} />
              ) : (
                <p className="text-slate-500 italic">This document is currently empty or loading from Notion...</p>
              )}
            </div>
          </div>
        </div>

        {/* Floating Integrity Badge */}
        {!showRightSidebar && activeDoc?.hasIssue && (
          <div className="absolute top-20 right-8 z-30">
            <button 
              onClick={() => setShowRightSidebar(true)}
              className="flex items-center gap-2.5 bg-white p-1.5 rounded-full shadow-lg border border-slate-200 hover:shadow-xl transition-shadow"
            >
              <div className="flex items-center justify-center w-8 h-8 bg-red-600 text-white rounded-full font-bold text-lg shrink-0">
                {activeDoc.issues?.length || 1}
              </div>
              <div className="flex flex-col items-start leading-tight pr-3 ml-[-2px]">
                <span className="text-slate-900 font-bold text-[11px] tracking-tight">INTEGRITY</span>
                <span className="text-slate-900 font-bold text-[11px] tracking-tight">ISSUES</span>
              </div>
            </button>
          </div>
        )}
        
        {!showRightSidebar && !activeDoc?.hasIssue && (
          <div className="absolute top-20 right-8 z-30">
            <div className="flex items-center gap-2.5 bg-white p-1.5 rounded-full shadow-md border border-slate-200 opacity-60">
              <div className="flex items-center justify-center w-8 h-8 bg-green-500 text-white rounded-full shrink-0">
                <Check className="w-5 h-5 stroke-[3]" />
              </div>
              <div className="flex flex-col items-start leading-tight pr-3 ml-[-2px]">
                <span className="text-slate-900 font-bold text-[11px] tracking-tight">INTEGRITY</span>
                <span className="text-slate-500 text-[10px] tracking-tight">CLEAR</span>
              </div>
            </div>
          </div>
        )}
      </div>

      {showRightSidebar && activeDoc?.hasIssue && <RightSidebar document={activeDoc} onClose={() => setShowRightSidebar(false)} />}
    </main>
  );
};