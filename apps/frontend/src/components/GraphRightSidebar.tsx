import { X, AlertTriangle, FileText, Lightbulb, ExternalLink } from "lucide-react";
import { Link } from "react-router-dom";

export const GraphRightSidebar = ({ onClose }: { onClose: () => void }) => {
  return (
    <aside className="w-[420px] h-full bg-white border-l border-slate-200 shadow-2xl flex flex-col shrink-0 z-50 font-sans absolute right-0 top-0">
      <div className="border-b border-slate-200 flex items-center justify-between h-14 px-4 shrink-0 bg-white">
        <div className="flex items-center gap-2">
          <AlertTriangle className="w-4 h-4 text-red-600" />
          <h3 className="font-bold text-sm text-slate-900 tracking-tight">INTEGRITY ISSUES</h3>
        </div>
        <button 
          onClick={onClose}
          className="p-1.5 rounded-md hover:bg-slate-100 text-slate-400 hover:text-slate-900 transition-colors flex items-center justify-center"
        >
          <X className="w-4 h-4" />
        </button>
      </div>

      <div className="flex-1 overflow-y-auto p-5">
        {/* Section 1: AI 충돌 원인 파악 */}
        <div className="mb-8">
          <div className="flex items-center gap-2 mb-3">
            <h4 className="text-sm font-semibold text-slate-900">충돌 원인 파악</h4>
            <span className="px-1.5 py-0.5 text-[10px] font-bold text-white bg-gradient-to-r from-blue-600 to-purple-600 rounded">AI</span>
          </div>
          <div className="bg-slate-50 p-4 rounded-lg border border-slate-200">
            <p className="text-sm font-semibold text-slate-800 leading-relaxed">
              최근 주간 회의록에서 수수료 최적화 이슈로 Toss Payments를 우선 연동하는 것으로 결정되었습니다.
            </p>
          </div>
        </div>

        {/* Section 2: 문서 대조 분석 */}
        <div className="mb-8">
          <h4 className="text-sm font-semibold text-slate-900 mb-3">문서 대조 분석</h4>
          <div className="space-y-4">
            
            {/* PRD Document */}
            <div className="border border-red-200 rounded-lg overflow-hidden shadow-sm">
              <div className="bg-red-50 px-3 py-2 flex items-center justify-between border-b border-red-100">
                <div className="flex items-center gap-1.5">
                  <FileText className="w-3.5 h-3.5 text-red-600" />
                  <span className="text-xs font-bold text-red-900">[PRD] v2.0 결제 시스템 요구사항</span>
                </div>
                <Link to="/w/sample-workspace/docs/prd" className="text-[10px] font-bold text-red-600 hover:underline flex items-center gap-0.5">
                  바로가기 <ExternalLink className="w-3 h-3" />
                </Link>
              </div>
              <div className="bg-white p-3">
                <p className="text-xs text-slate-700 leading-relaxed font-medium">
                  "PG사 연동 시 기존 인프라와의 호환성을 고려하여 <span className="bg-red-100 text-red-700 font-bold px-1 rounded mx-0.5">KCP</span>를 최우선으로 적용하여 개발을 진행한다."
                </p>
              </div>
            </div>

            {/* Meeting Notes Document */}
            <div className="border border-amber-200 rounded-lg overflow-hidden shadow-sm">
              <div className="bg-amber-50 px-3 py-2 flex items-center justify-between border-b border-amber-100">
                <div className="flex items-center gap-1.5">
                  <FileText className="w-3.5 h-3.5 text-amber-600" />
                  <span className="text-xs font-bold text-amber-900">[주간회의록] 4월 4주차 백엔드 싱크</span>
                </div>
                <Link to="/w/sample-workspace/docs/meet-urgent" className="text-[10px] font-bold text-amber-600 hover:underline flex items-center gap-0.5">
                  바로가기 <ExternalLink className="w-3 h-3" />
                </Link>
              </div>
              <div className="bg-white p-3">
                <p className="text-xs text-slate-700 leading-relaxed font-medium">
                  "...백엔드 연동 관련하여 수수료 최적화 및 빠른 정산 주기를 확보하기 위해 <span className="bg-amber-100 text-amber-800 font-bold px-1 rounded mx-0.5">Toss Payments</span>를 우선적으로 연동하기로 결정함."
                </p>
              </div>
            </div>
            
          </div>
        </div>

        {/* Section 3: DocGraph AI 제안 */}
        <div className="mb-4">
          <div className="flex items-center gap-2 mb-3">
            <h4 className="text-sm font-semibold text-slate-900">DocGraph AI 제안</h4>
            <Lightbulb className="w-4 h-4 text-blue-600" />
          </div>
          
          <div className="rounded-lg border border-slate-200 overflow-hidden text-xs font-mono shadow-sm">
            <div className="bg-red-50 text-red-800 px-3 py-2.5 flex gap-3 items-start">
              <span className="text-red-400 select-none font-bold mt-0.5">-</span>
              <span className="leading-relaxed">PG사 연동 시 기존 인프라와의 호환성을 고려하여 <span className="bg-red-200 px-0.5 rounded">KCP</span>를 최우선으로 적용하여 개발을 진행한다.</span>
            </div>
            <div className="bg-green-50 text-green-800 px-3 py-2.5 flex gap-3 items-start border-t border-slate-200">
              <span className="text-green-500 select-none font-bold mt-0.5">+</span>
              <span className="leading-relaxed">PG사 연동 시 수수료 최적화 및 정산 주기를 고려하여 <span className="bg-green-200 px-0.5 rounded">Toss Payments</span>를 최우선으로 적용하여 개발을 진행한다.</span>
            </div>
          </div>
        </div>
      </div>

      {/* Section 4: Buttons */}
      <div className="border-t border-slate-200 p-4 bg-white mt-auto shrink-0 flex flex-col gap-2.5">
        <button className="w-full py-2.5 bg-blue-600 hover:bg-blue-700 text-white text-xs font-bold rounded-md shadow-sm transition-colors flex items-center justify-center gap-2">
          제안 적용하기
        </button>
        <Link 
          to="/w/sample-workspace/docs/prd"
          className="w-full py-2.5 bg-white border border-slate-200 hover:bg-slate-50 text-slate-700 text-xs font-bold rounded-md transition-colors flex items-center justify-center"
        >
          원본 문서 열기
        </Link>
      </div>
    </aside>
  );
};