import { ExternalLink, CheckCircle, Check } from "lucide-react";
import { useState, useEffect, useRef, Fragment } from "react";
import { Link, useParams, useSearchParams, useNavigate } from "react-router-dom";
import { RightSidebar } from "../components/RightSidebar";
import { useAppStore, type DocumentType, type DocumentNode } from '../store';
import type { IntegrityIssue } from '../store';
import { formatRelativeTime } from '../lib/timeAgo';
import { BlockRenderer } from "../features/document/BlockRenderer";
import { useDocument } from "../hooks/useDocument";
import { useWorkspaceDetail } from "../hooks/useWorkspaceDetail";
import { useProjectDocuments } from "../hooks/useProjectDocuments";
import { useProjectTypeAssignees } from "../hooks/useProjectDetail";
import { useInbox } from "../hooks/useInbox";

const findRootDocument = (docs: DocumentNode[]): DocumentNode | null => {
  // 트리에서 최상위 루트 문서를 찾음
  // 가장 먼저 children을 가진 문서를 루트로 간주
  if (!docs || docs.length === 0) return null;

  for (const doc of docs) {
    if (doc.children && doc.children.length > 0) {
      return doc;
    }
  }

  // children이 없으면 첫 번째 문서를 반환
  return docs[0] || null;
};

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

function resolveCategoryType(roots: DocumentNode[], docId: number): DocumentType | null {
  const containsId = (node: DocumentNode, id: number): boolean =>
    node.id === id || (node.children ?? []).some((c) => containsId(c, id));

  const rootDoc = findRootDocument(roots);
  if (!rootDoc) return null;

  const categories = rootDoc.children ?? [];
  for (const cat of categories) {
    if (containsId(cat, docId)) return cat.type ?? null;
  }
  return null;
}

// 브레드크럼 표시 폭: 한글(완성형/자모/호환자모)은 폭 2, 그 외 문자는 폭 1로 계산
// (영어 40자 ≈ 한글 20자 라는 요구사항을 "폭 40" 하나의 기준으로 통일하기 위함)
const isHangul = (ch: string): boolean => {
  const code = ch.codePointAt(0) ?? 0;
  return (code >= 0xac00 && code <= 0xd7a3)
    || (code >= 0x1100 && code <= 0x11ff)
    || (code >= 0x3130 && code <= 0x318f);
};

const displayWidth = (text: string): number => {
  let width = 0;
  for (const ch of text) width += isHangul(ch) ? 2 : 1;
  return width;
};

const truncateToWidth = (text: string, maxWidth: number): string => {
  let width = 0;
  let result = '';
  for (const ch of text) {
    const chWidth = isHangul(ch) ? 2 : 1;
    if (width + chWidth > maxWidth) break;
    width += chWidth;
    result += ch;
  }
  return result;
};

const ellipsisIfOver = (text: string, limit: number, cut: number): string =>
  displayWidth(text) > limit ? `${truncateToWidth(text, cut)}...` : text;

type BreadcrumbCrumb = {
  key: string;
  title: string;
  emoji?: string;
  to?: string;
};

type BreadcrumbRenderItem =
  | { type: 'crumb'; crumb: BreadcrumbCrumb; title: string }
  | { type: 'ellipsis'; key: string };

const BREADCRUMB_SEPARATOR_WIDTH = 7;
const BREADCRUMB_PROTECTED_RULE = { limit: 40, cut: 38 };
const BREADCRUMB_PROTECTED_RULE_TIGHT = { limit: 20, cut: 18 };
const BREADCRUMB_MIDDLE_RULE = { limit: 10, cut: 8 };
// 주의: 아래 두 임계값은 실제 컨테이너 폭을 동적으로 측정하는 게 아니라
// "이름이 비정상적으로 길어서 창 크기와 무관하게 줄바꿈 위험이 큰 경우"에 대비한 안전장치 성격의 정적 기준값.
const BREADCRUMB_COLLAPSE_THRESHOLD = 800;
const BREADCRUMB_TIGHTEN_THRESHOLD = 200;

// 브레드크럼 절단/축약 규칙:
// 1) 보호 대상 3개(루트=첫 항목, 1단계 상위=끝에서 두 번째, 현재 페이지=마지막)는 폭 40 초과 시 38까지 표시
// 2) 그 외(2단계 이상 상위)는 폭 10 초과 시 8까지 표시
// 3) (1)~(2) 적용 후 표시 폭 합(구분자 ' / '는 7로 계산)이 BREADCRUMB_COLLAPSE_THRESHOLD를 넘으면
//    루트와 1단계 상위 사이의 항목들을 클릭 불가능한 '...' 하나로 축약
// 4) 그래도 합이 BREADCRUMB_TIGHTEN_THRESHOLD를 넘으면 보호 대상 3개에는 더 엄격한 기준(폭 20 초과 시 18까지)을 적용
const buildBreadcrumbItems = (crumbs: BreadcrumbCrumb[]): BreadcrumbRenderItem[] => {
  const titles = crumbs.map((c) => c.title);
  const lastIndex = titles.length - 1;
  const parentIndex = lastIndex - 1;
  const isProtected = (i: number) => i === 0 || i === parentIndex || i === lastIndex;

  const sumWidth = (arr: string[]) =>
    arr.reduce((sum, t) => sum + displayWidth(t), 0)
    + BREADCRUMB_SEPARATOR_WIDTH * Math.max(arr.length - 1, 0);

  const truncateAll = (protectedRule: { limit: number; cut: number }) =>
    titles.map((t, i) => {
      const rule = isProtected(i) ? protectedRule : BREADCRUMB_MIDDLE_RULE;
      return ellipsisIfOver(t, rule.limit, rule.cut);
    });

  // 루트(0)와 1단계 상위(parentIndex) 사이에 항목이 1개 이상 있을 때만 축약 가능
  const collapseRange: [number, number] | null = parentIndex > 1 ? [1, parentIndex - 1] : null;
  const withCollapse = (arr: string[]) =>
    collapseRange
      ? [...arr.slice(0, collapseRange[0]), '...', ...arr.slice(collapseRange[1] + 1)]
      : arr;

  const lenientTitles = truncateAll(BREADCRUMB_PROTECTED_RULE);
  const shouldCollapse = collapseRange !== null
    && sumWidth(lenientTitles) > BREADCRUMB_COLLAPSE_THRESHOLD;

  const widthAfterCollapseDecision = shouldCollapse
    ? sumWidth(withCollapse(lenientTitles))
    : sumWidth(lenientTitles);

  const finalTitles = widthAfterCollapseDecision > BREADCRUMB_TIGHTEN_THRESHOLD
    ? truncateAll(BREADCRUMB_PROTECTED_RULE_TIGHT)
    : lenientTitles;

  const items: BreadcrumbRenderItem[] = [];
  for (let i = 0; i < crumbs.length; i++) {
    if (shouldCollapse && collapseRange) {
      const [start, end] = collapseRange;
      if (i > start && i <= end) continue;
      if (i === start) {
        items.push({ type: 'ellipsis', key: `ellipsis-${crumbs[i].key}` });
        continue;
      }
    }
    items.push({ type: 'crumb', crumb: crumbs[i], title: finalTitles[i] });
  }
  return items;
};

export const DocumentView = () => {
  const { docId: id, workspaceId, projectId } = useParams();
  const [searchParams, setSearchParams] = useSearchParams();
  const navigate = useNavigate();
  const workspaces = useAppStore((state) => state.workspaces);
  const projects = useAppStore((state) => state.projects);
  const workspaceName = workspaces.find((w) => w.id === Number(workspaceId))?.name ?? workspaceId;
  const projectName = projects.find((p) => p.id === Number(projectId))?.name ?? '';
  const { document: docDetail } = useDocument(id ? Number(id) : undefined);
  const { workspace } = useWorkspaceDetail(workspaceId ? Number(workspaceId) : undefined);
  const { typeAssignees } = useProjectTypeAssignees(projectId ? Number(projectId) : undefined);

  const assignedWorkspaceMemberId =
    docDetail?.assigneeMemberId ??
    typeAssignees.find((item) => item.documentType === docDetail?.type)?.assigneeMemberId ??
    null;

  const assignee = workspace?.members?.find(
    (m) => m.workspaceMemberId === assignedWorkspaceMemberId
  );

  // 실 문서 트리 (API)
  const { documents } = useProjectDocuments(projectId ? Number(projectId) : undefined);

  // 현재 문서의 충돌 — inbox에서 targetDocument 기준 필터링
  const { conflicts } = useInbox();
  const docIssues: IntegrityIssue[] = conflicts
    .filter((c) => c.targetDocument.id === Number(id))
    .map((c) => ({
      id: String(c.id),
      status: c.status,
      conflictId: c.id,
      findingId: c.latestFindingId ?? undefined,
      title: c.title,
      sourceDocumentId: c.sourceDocument.id,
      sourceDocumentTitle: c.sourceDocument.title,
    }));

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
  const activeDocBase = allDocs.find((d) => d.id === Number(id));
  // docDetail(API)로 title/emoji 채우고 issues를 inbox 데이터로 덮어씀
  const activeDoc: DocumentNode | undefined = activeDocBase
    ? { ...activeDocBase, title: docDetail?.title ?? activeDocBase.title, issues: docIssues }
    : docDetail
      ? { id: Number(id), title: docDetail.title ?? '', emoji: undefined, issues: docIssues, children: [] }
      : undefined;

  const parentPage = id ? findParentPage(documents, id) : null;
  const rootDocument = findRootDocument(documents);
  const categoryType = resolveCategoryType(documents, activeDoc?.id ?? -1);

  // child_page 블록 → 내부 문서 ID 매핑 (notionPageId 기준)
  const notionPageIdToDocId = new Map<string, number>(
    allDocs
      .filter((d) => d.notionPageId)
      .map((d) => [d.notionPageId as string, d.id as number])
  );

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

  // 브레드크럼 항목 구성: 워크스페이스 / 프로젝트 / [문서트리 루트] / [부모 카테고리] / 현재 문서
  const breadcrumbCrumbs: BreadcrumbCrumb[] = [
    { key: 'workspace', title: workspaceName ?? '', to: `/w/${workspaceId}` },
    { key: 'project', title: projectName, to: `/w/${workspaceId}/p/${projectId}/graph` },
  ];
  if (rootDocument && activeDoc?.id !== rootDocument.id) {
    breadcrumbCrumbs.push({
      key: `root-${rootDocument.id}`,
      title: rootDocument.title,
      emoji: rootDocument.emoji,
      to: `/w/${workspaceId}/p/${projectId}/docs/${rootDocument.id}`,
    });
  }
  if (parentPage && parentPage.id !== rootDocument?.id) {
    breadcrumbCrumbs.push({
      key: `parent-${parentPage.id}`,
      title: parentPage.title,
      emoji: parentPage.emoji,
      to: `/w/${workspaceId}/p/${projectId}/docs/${parentPage.id}`,
    });
  }
  breadcrumbCrumbs.push({
    key: `current-${activeDoc?.id ?? 'unknown'}`,
    title: activeDoc?.title ?? '',
    emoji: activeDoc?.emoji,
  });
  const breadcrumbItems = buildBreadcrumbItems(breadcrumbCrumbs);

  return (
    <main className="flex-1 flex h-screen overflow-hidden bg-white font-sans relative">
      <div className={`flex-1 flex flex-col h-full relative min-w-0 transition-[margin-right] duration-[225ms] ease-out ${isPanelOpen ? 'mr-[400px]' : 'mr-0'}`}>
        <header className="h-14 flex items-center justify-between px-6 border-b border-slate-200 bg-white shrink-0">
          <div className="flex items-center text-sm text-slate-500 flex-1 min-w-0">
            <div className="flex items-center overflow-hidden whitespace-nowrap min-w-0">
              {breadcrumbItems.map((item, idx) => (
                <Fragment key={item.type === 'ellipsis' ? item.key : item.crumb.key}>
                  {idx > 0 && <span className="mx-1 text-[14px] opacity-40 shrink-0">/</span>}
                  {item.type === 'ellipsis' ? (
                    <span className="px-2 py-1 text-slate-400 select-none shrink-0">...</span>
                  ) : item.crumb.to ? (
                    <Link
                      to={item.crumb.to}
                      title={item.crumb.title}
                      className="px-2 py-1 rounded hover:bg-slate-100 cursor-pointer transition-colors hover:text-slate-900 flex items-center gap-1 shrink-0"
                    >
                      {item.crumb.emoji && <span>{item.crumb.emoji}</span>}
                      <span>{item.title}</span>
                    </Link>
                  ) : (
                    <span
                      title={item.crumb.title}
                      className="px-2 py-1 rounded text-slate-900 font-medium flex items-center gap-1 shrink-0"
                    >
                      {item.crumb.emoji && <span>{item.crumb.emoji}</span>}
                      <span>{item.title}</span>
                    </span>
                  )}
                </Fragment>
              ))}
            </div>
            <div className="ml-3 flex items-center gap-1 px-2 py-0.5 bg-slate-100 rounded text-xs text-slate-500 shrink-0">
              <CheckCircle className="w-3.5 h-3.5" />
              Saved
            </div>
          </div>
          <a
            href={docDetail?.notionPageId ? `https://notion.so/${docDetail.notionPageId.replace(/-/g, '')}` : undefined}
            target="_blank"
            rel="noopener noreferrer"
            className={`flex items-center gap-1.5 px-3 py-1 border border-slate-200 rounded hover:bg-slate-50 transition-colors text-xs font-medium text-slate-600 ${!docDetail?.notionPageId ? 'pointer-events-none opacity-40' : ''}`}
          >
            <ExternalLink className="w-4 h-4" />
            Edit in Notion
          </a>
        </header>

        <div className="flex-1 overflow-y-auto px-8 py-12 flex justify-center">
          <div className="w-full max-w-[800px] pb-24">
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
                  <span className="w-32 text-slate-500">카테고리</span>
                  {categoryType ? (
                    <span className="flex items-center bg-slate-100 px-2 py-0.5 rounded text-slate-900">
                      <span className="text-sm">{categoryType}</span>
                    </span>
                  ) : (
                    <span className="text-slate-500">—</span>
                  )}
                </div>
                <div className="flex items-start">
                  <span className="w-32 text-slate-500 shrink-0 leading-6">담당자</span>
                  <div className="flex items-center flex-wrap gap-1.5">
                    {assignee ? (
                      <div className="flex items-center bg-slate-100 px-2 py-0.5 rounded text-slate-900">
                        <span className="text-sm">{assignee.name}</span>
                      </div>
                    ) : (
                      <span className="text-slate-500">—</span>
                    )}
                  </div>
                </div>
                {/* 최근 수정 줄 — 좌측 정렬을 위 두 줄 칩의 왼쪽 경계와 일치시키기 위해
                      "라벨 + 콘텐츠 div" 2단 구조로 변경 (기존 부모 gap-2가 정렬을 어긋나게 했음).
                      mock: lastEditedAt = 1시간 전 가짜 timestamp.
                      TODO(API): DocumentDetail.notionLastEditedAt 사용 + 최근 수정자 이름 (현재 API에 없음 — 백엔드 추가 요청 후보) */}
                <div className="flex items-center">
                  <span className="w-32 text-slate-500 shrink-0">최근 수정</span>
                  <div className="flex items-center flex-wrap gap-2">
                    {docDetail?.notionLastEditedAt ? (
                      <div className="relative group">
                        <span className="text-slate-900 cursor-default">
                          {new Date(docDetail.notionLastEditedAt).toLocaleString('ko-KR', { year: 'numeric', month: 'long', day: 'numeric', hour: '2-digit', minute: '2-digit' })}
                        </span>
                        <div className="absolute bottom-full left-1/2 -translate-x-1/2 mb-1.5 px-2 py-1 bg-slate-900 text-white text-xs rounded opacity-0 group-hover:opacity-100 transition-opacity pointer-events-none whitespace-nowrap z-10">
                          {formatRelativeTime(new Date(docDetail.notionLastEditedAt))}
                          <div className="absolute top-full left-1/2 -translate-x-1/2 border-4 border-transparent border-t-slate-900"></div>
                        </div>
                      </div>
                    ) : (
                      <span className="text-slate-500">—</span>
                    )}
                    {docDetail?.lastEditedByName && (
                      <div className="flex items-center bg-slate-100 px-2 py-0.5 rounded text-slate-900">
                        <span className="text-sm">{docDetail.lastEditedByName}</span>
                      </div>
                    )}
                  </div>
                </div>
              </div>

              <hr className="border-slate-200 mb-8" />
            </>

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
              <BlockRenderer
                blocks={docDetail?.blocks ?? []}
                notionPageIdToDocId={notionPageIdToDocId}
                docBasePath={`/w/${workspaceId}/p/${projectId}/docs`}
              />
            </div>
          </div>
        </div>

        {/* Floating Integrity Badge: ACTIVE 이슈 있을 때 (빨간) */}
        {!isPanelOpen && (() => {
          const activeCount = (activeDoc?.issues ?? []).filter((i: IntegrityIssue) => i.status !== 'IGNORED').length;
          return activeCount > 0 ? (
            <div className="absolute top-20 right-8 z-30">
              <button
                onClick={openPanel}
                className="flex items-center gap-2.5 bg-white p-1.5 rounded-full shadow-lg border border-slate-200 hover:shadow-xl transition-shadow"
              >
                <div className="flex items-center justify-center w-8 h-8 bg-red-600 text-white rounded-full font-bold text-lg shrink-0">
                  {activeCount}
                </div>
                <div className="flex flex-col items-start leading-tight pr-3 ml-[-2px]">
                  <span className="text-slate-900 font-bold text-[11px] tracking-tight">INTEGRITY</span>
                  <span className="text-slate-900 font-bold text-[11px] tracking-tight">ISSUES</span>
                </div>
              </button>
            </div>
          ) : (
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
          );
        })()}
      </div>

      {/* RightSidebar: hasIssue 조건 제거 — 이슈 0개일 때도 빈 상태로 표시 */}
      {showRightSidebar && activeDoc && (
        <RightSidebar document={activeDoc} isOpen={isPanelOpen} onClose={closePanel} />
      )}
    </main>
  );
};