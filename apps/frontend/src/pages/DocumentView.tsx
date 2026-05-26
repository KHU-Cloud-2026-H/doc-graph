import { ExternalLink, CheckCircle, Check } from "lucide-react";
import { useState, useEffect, useRef } from "react";
import { Link, useParams, useSearchParams, useNavigate } from "react-router-dom";
import { RightSidebar } from "../components/RightSidebar";
import { useAppStore } from '../store';
import type { IntegrityIssue } from '../store';
import { formatRelativeTime } from '../lib/timeAgo';

const findParentPage = (docs: any[], targetId: string): any | null => {
  const numId = Number(targetId);
  for (const doc of docs) {
    if (doc.children?.some((c: any) => c.id === numId)) return doc;
    if (doc.children) {
      const found = findParentPage(doc.children, targetId);
      if (found) return found;
    }
  }
  return null;
};

export const DocumentView = () => {
  const { docId: id, workspaceId, projectId } = useParams();
  const [searchParams, setSearchParams] = useSearchParams();
  const navigate = useNavigate();
  const workspaces = useAppStore((state) => state.workspaces);
  const projects = useAppStore((state) => state.projects);
  const documents = useAppStore((state) => state.documents);
  const workspaceName = workspaces.find((w) => w.id === Number(workspaceId))?.name ?? workspaceId;
  const projectName = projects.find((p) => p.id === Number(projectId))?.name ?? '';

  // Flatten documents tree to find the active one
  const flattenDocs = (docs: any[]): any[] => {
    return docs.reduce((acc, doc) => {
      if (doc.children?.length) {
        return [...acc, doc, ...flattenDocs(doc.children)];
      }
      return [...acc, doc];
    }, []);
  };
  
  const allDocs = flattenDocs(documents);
  const activeDoc = allDocs.find((d) => d.id === Number(id)) || allDocs[0];
  const parentPage = id ? findParentPage(documents, id) : null;
  
  // lazy initialization: 마운트 시점의 URL을 직접 읽어 초기 상태 결정.
  // useState(() => ...) 형태는 StrictMode의 이중 실행에도 항상 같은 URL을 읽으므로 안전.
  const [showRightSidebar, setShowRightSidebar] = useState(
    () => new URLSearchParams(window.location.search).get("openIssues") === "true"
  );
  // 슬라이드 애니메이션용. showRightSidebar는 마운트/언마운트, isPanelOpen은 visual open/close.
  // 둘을 분리해 슬라이드 아웃이 끝난 다음에 언마운트되도록 한다.
  const [isPanelOpen, setIsPanelOpen] = useState(
    () => new URLSearchParams(window.location.search).get("openIssues") === "true"
  );
  const SLIDE_DURATION_MS = 225;

  // 알약 버튼 클릭 / 본문 issue-target 클릭 시 호출
  const openPanel = () => {
    setShowRightSidebar(true);
    // 다음 frame에 isPanelOpen=true → 슬라이드 인 + 본문 mr transition 동시 시작
    setTimeout(() => setIsPanelOpen(true), 20);
  };

  // X 버튼 클릭 시 호출
  const closePanel = () => {
    setIsPanelOpen(false);
    // 슬라이드 아웃 + 본문 mr transition 완료 후 언마운트
    setTimeout(() => setShowRightSidebar(false), SLIDE_DURATION_MS);
  };

  // openIssues 쿼리 파라미터 감지 → 패널 열기 + 파라미터 제거.
  // searchParams를 deps로 두어 같은 페이지에서 Inbox 클릭 시(remount 없이 searchParams만 변경)도 동작.
  // StrictMode 이중 실행: 2번째 실행 시 이미 파라미터가 제거돼 있으므로 no-op.
  useEffect(() => {
    if (searchParams.get("openIssues") === "true") {
      setShowRightSidebar(true);
      setIsPanelOpen(true);
      setSearchParams(prev => {
        const next = new URLSearchParams(prev);
        next.delete("openIssues");
        return next;
      }, { replace: true });
    }
  }, [searchParams, setSearchParams]);

  // 문서 전환 시 패널 닫기 + 스크롤 초기화.
  // lastDocIdRef로 실제 docId 변경 여부를 판별 →
  // 마운트 직후와 StrictMode 이중 실행에서는 닫지 않고, 진짜 전환 시에만 닫음.
  const lastDocIdRef = useRef<number | undefined>(undefined);
  useEffect(() => {
    window.scrollTo(0, 0);
    if (lastDocIdRef.current !== undefined && lastDocIdRef.current !== activeDoc?.id) {
      setShowRightSidebar(false);
      setIsPanelOpen(false);
    }
    lastDocIdRef.current = activeDoc?.id;
  }, [activeDoc?.id]);

  let processedHtml = activeDoc?.contentHtml || '';

  // Transform Notion-like child page links
  processedHtml = processedHtml.replaceAll(
    'class="flex items-center gap-2 p-2 hover:bg-slate-50 rounded border border-slate-200 text-blue-600 transition-colors"',
    'class="flex items-center w-fit gap-2 px-1.5 py-1 hover:bg-[#efefef] rounded-[4px] text-[#37352f] transition-colors cursor-pointer text-[15px] border-none outline-none"'
  );

  if (activeDoc?.hasIssue && activeDoc.issues) {
    activeDoc.issues.forEach((issue: IntegrityIssue) => {
      if (showRightSidebar) {
        // 본문 인라인 diff. 외부 div에 style="font: inherit;"를 박아 부모(<h1>/<p>/<li> 등)의
        // 폰트 크기·볼드·이탤릭을 그대로 상속받게 함. 텍스트 색상만 명시.
        // 마이너스/플러스 배지 18px로 RightSidebar와 일관성 유지.
        const diffHtml = `
          <div class="block border border-slate-200 rounded-md my-2 shadow-sm overflow-hidden bg-white" style="font: inherit;">
            <div class="flex items-start gap-2 bg-red-50 p-2.5 border-b border-slate-200">
              <span class="bg-red-600 text-white rounded-full flex items-center justify-center w-[18px] h-[18px] text-[12px] font-bold shrink-0 mt-0.5">-</span>
              <span class="whitespace-pre-wrap leading-relaxed text-red-900">${issue.currentText}</span>
            </div>
            <div class="flex items-start gap-2 bg-green-50 p-2.5">
              <span class="bg-green-600 text-white rounded-full flex items-center justify-center w-[18px] h-[18px] text-[12px] font-bold shrink-0 mt-0.5">+</span>
              <span class="whitespace-pre-wrap leading-relaxed text-green-900">${issue.newText}</span>
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
    <main className="flex-1 flex h-screen overflow-hidden bg-white font-sans relative">
      <div className={`flex-1 flex flex-col h-full relative min-w-0 transition-[margin-right] duration-[225ms] ease-out ${isPanelOpen ? 'mr-[400px]' : 'mr-0'}`}>
        <header className="h-14 flex items-center justify-between px-6 border-b border-slate-200 bg-white shrink-0">
          <div className="flex items-center text-sm text-slate-500 flex-1">
            <Link
              to={`/w/${workspaceId}`}
              className="px-2 py-1 rounded hover:bg-slate-100 cursor-pointer transition-colors hover:text-slate-900"
            >
              {workspaceName}
            </Link>
            <span className="mx-1 text-[14px] opacity-40">/</span>
            <Link
              to={`/w/${workspaceId}/p/${projectId}/graph`}
              className="px-2 py-1 rounded hover:bg-slate-100 cursor-pointer transition-colors hover:text-slate-900"
            >
              {projectName}
            </Link>
            {parentPage && (
              <>
                <span className="mx-1 text-[14px] opacity-40">/</span>
                <Link
                  to={`/w/${workspaceId}/p/${projectId}/docs/${parentPage.id}`}
                  className="px-2 py-1 rounded hover:bg-slate-100 cursor-pointer transition-colors hover:text-slate-900 flex items-center gap-1"
                >
                  {parentPage.emoji && <span>{parentPage.emoji}</span>}
                  <span>{parentPage.title}</span>
                </Link>
              </>
            )}
            <span className="mx-1 text-[14px] opacity-40">/</span>
            <span className="px-2 py-1 rounded hover:bg-slate-100 cursor-pointer transition-colors text-slate-900 font-medium truncate max-w-[300px] flex items-center gap-1">
              {activeDoc?.emoji && <span>{activeDoc.emoji}</span>}
              <span>{activeDoc?.title}</span>
            </span>
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
          <div className="w-full max-w-[800px] pb-24">
            {!activeDoc?.children?.length && (
              <>
                <h1 className="text-3xl font-bold text-slate-900 mb-8 outline-none tracking-tight">
                  {activeDoc?.title}
                </h1>

                {/* 메타데이터 영역
                    변경 사항:
                    - 프로젝트: 클릭하면 카테고리 페이지로 이동(Link), 회색 배경(담당자와 동일), text-sm
                    - 담당자: 프로필 사진 제거, 7명 팀원 이름 나열
                    - 최근 수정: 날짜 텍스트 호버 시 자연어 툴팁("1시간 전" 등) 표시. 프로필 사진 제거.
                    TODO(API): 프로젝트 = ProjectSummary.name + Link to ProjectDetail 화면.
                               담당자 = DocumentDetail.assigneeMemberId(단수) + ProjectDetail.members[] 조합. 백엔드 협의 후보.
                               최근 수정 시각 = DocumentDetail.notionLastEditedAt. 자연어 표시는 dayjs.fromNow() 등으로 계산.
                               최근 수정자(이름)는 현재 API에 없음. 백엔드 추가 요청 후보. */}
                <div className="mb-8 space-y-3 text-sm">
                  <div className="flex items-center">
                    <span className="w-32 text-slate-500">프로젝트</span>
                    {projectName ? (
                      <Link
                        to={`/w/${workspaceId}/p/${projectId}/graph`}
                        className="flex items-center gap-1.5 bg-slate-100 px-2 py-0.5 rounded text-slate-900 hover:bg-slate-200 transition-colors"
                      >
                        <span className="text-sm">{projectName}</span>
                      </Link>
                    ) : (
                      <span className="text-slate-500">—</span>
                    )}
                  </div>
                  <div className="flex items-start">
                    <span className="w-32 text-slate-500 shrink-0 leading-6">담당자</span>
                    <div className="flex items-center flex-wrap gap-1.5">
                      {['박관우', '서영채', '신정환', '전현준', '이창민', '김연길', '이주안'].map((name) => (
                        <div key={name} className="flex items-center bg-slate-100 px-2 py-0.5 rounded text-slate-900">
                          <span className="text-sm">{name}</span>
                        </div>
                      ))}
                    </div>
                  </div>
                  {/* 최근 수정 줄 — 좌측 정렬을 위 두 줄 칩의 왼쪽 경계와 일치시키기 위해
                      "라벨 + 콘텐츠 div" 2단 구조로 변경 (기존 부모 gap-2가 정렬을 어긋나게 했음).
                      mock: lastEditedAt = 1시간 전 가짜 timestamp.
                      TODO(API): DocumentDetail.notionLastEditedAt 사용 + 최근 수정자 이름 (현재 API에 없음 — 백엔드 추가 요청 후보) */}
                  <div className="flex items-center">
                    <span className="w-32 text-slate-500 shrink-0">최근 수정</span>
                    <div className="flex items-center flex-wrap gap-2">
                      <div className="relative group">
                        <span className="text-slate-900 cursor-default">2026년 5월 21일 14:30</span>
                        <div className="absolute bottom-full left-1/2 -translate-x-1/2 mb-1.5 px-2 py-1 bg-slate-900 text-white text-xs rounded opacity-0 group-hover:opacity-100 transition-opacity pointer-events-none whitespace-nowrap z-10">
                          {formatRelativeTime(new Date(Date.now() - 60 * 60 * 1000))}
                          <div className="absolute top-full left-1/2 -translate-x-1/2 border-4 border-transparent border-t-slate-900"></div>
                        </div>
                      </div>
                      <div className="flex items-center bg-slate-100 px-2 py-0.5 rounded text-slate-900">
                        <span className="text-sm">박관우</span>
                      </div>
                    </div>
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
                if (href) {
                  if (href.startsWith('/docs/')) {
                    e.preventDefault();
                    navigate(`/w/${workspaceId}/p/${projectId}${href}`);
                    return;
                  }
                  const oldDocMatch = href.match(/^\/w\/[^/]+\/docs\/(.+)$/);
                  if (oldDocMatch) {
                    e.preventDefault();
                    navigate(`/w/${workspaceId}/p/${projectId}/docs/${oldDocMatch[1]}`);
                    return;
                  }
                }
                if (target.closest('.issue-target')) {
                  // openPanel을 호출해야 isPanelOpen도 true로 전환되어 슬라이드 인 + 본문 mr transition이 동시 발동된다.
                  // setShowRightSidebar(true)만 호출하면 마운트는 되지만 isPanelOpen=false 상태라
                  // translate-x-full로 화면 밖에 머문다.
                  openPanel();
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

        {/* Floating Integrity Badge: 이슈 있을 때 (빨간) */}
        {!isPanelOpen && activeDoc?.hasIssue && (
          <div className="absolute top-20 right-8 z-30">
            <button
              onClick={openPanel}
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
        
        {/* Floating Integrity Badge: 이슈 없을 때 (초록, 이제 클릭 가능) */}
        {!isPanelOpen && !activeDoc?.hasIssue && (
          <div className="absolute top-20 right-8 z-30">
            <button
              onClick={openPanel}
              className="flex items-center gap-2.5 bg-white p-1.5 rounded-full shadow-md border border-slate-200 opacity-60 hover:opacity-100 hover:shadow-lg transition-all"
            >
              <div className="flex items-center justify-center w-8 h-8 bg-green-500 text-white rounded-full shrink-0">
                <Check className="w-5 h-5 stroke-[3]" />
              </div>
              <div className="flex flex-col items-start leading-tight pr-3 ml-[-2px]">
                <span className="text-slate-900 font-bold text-[11px] tracking-tight">INTEGRITY</span>
                <span className="text-slate-500 text-[10px] tracking-tight">CLEAR</span>
              </div>
            </button>
          </div>
        )}
      </div>

      {/* RightSidebar: hasIssue 조건 제거 — 이슈 0개일 때도 빈 상태로 표시 */}
      {showRightSidebar && activeDoc && (
        <RightSidebar document={activeDoc} isOpen={isPanelOpen} onClose={closePanel} />
      )}
    </main>
  );
};