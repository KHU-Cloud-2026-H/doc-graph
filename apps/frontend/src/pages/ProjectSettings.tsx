import React, { useState, useEffect } from 'react';
import { Link, useParams } from 'react-router-dom';
import { X, Loader2 } from 'lucide-react';
import { useAppStore } from '../store';
import { useProjectDetail, useProjectCategories, useProjectTypeAssignees } from '../hooks/useProjectDetail';
import { useWorkspaceDetail } from '../hooks/useWorkspaceDetail';
import { useNotionPageChildren, useNotionPageMetadata } from '../hooks/useNotionPages';
import {
  useDeleteProjectMember,
  useUpdateProjectMemberRole,
  useAddProjectCategory,
  useUpdateProjectCategoryType,
  useDeleteProjectCategory,
  useUpdateTypeAssignees,
} from '../hooks/useProjectSettingsMutations';

const DOCUMENT_TYPES = [
  'meeting_notes', 'planning', 'requirements', 'design', 'research',
] as const;
type DocumentType = typeof DOCUMENT_TYPES[number];

const selectCls =
  'h-9 rounded-md border border-gray-300 bg-white px-3 py-1 text-sm ' +
  'focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 cursor-pointer';
const btnPrimary =
  'inline-flex items-center justify-center rounded-md bg-blue-600 px-4 py-2 ' +
  'text-sm font-medium text-white hover:bg-blue-700 ' +
  'disabled:opacity-50 disabled:cursor-not-allowed transition-colors';
const thCls = 'h-10 px-2 text-left text-sm font-medium text-gray-700 whitespace-nowrap';
const tdCls = 'p-2 align-middle text-sm whitespace-nowrap';

const ProjectSettings: React.FC = () => {
  const { workspaceId, projectId } = useParams<{ workspaceId: string; projectId: string }>();
  const numericProjectId = projectId ? Number(projectId) : undefined;
  const numericWorkspaceId = workspaceId ? Number(workspaceId) : undefined;

  const workspaces = useAppStore((state) => state.workspaces);
  const projects = useAppStore((state) => state.projects);
  const workspaceName = workspaces.find((w) => w.id === numericWorkspaceId)?.name ?? '워크스페이스';
  const projectName = projects.find((p) => p.id === numericProjectId)?.name ?? '프로젝트';

  const { project } = useProjectDetail(numericProjectId);
  const { categories } = useProjectCategories(numericProjectId);
  const { typeAssignees } = useProjectTypeAssignees(numericProjectId);
  const { workspace } = useWorkspaceDetail(numericWorkspaceId);
  const { children: notionCategoryPages } = useNotionPageChildren(
    numericWorkspaceId,
    project?.notionRootPageId ?? null,
  );

  type TabId = 'info' | 'members' | 'categories' | 'assignees';
  const [activeTab, setActiveTab] = useState<TabId>('info');

  // ── 탭 1: 루트 페이지 메타데이터 ───────────────────────────────
  const { page: rootPageMeta } = useNotionPageMetadata(numericWorkspaceId, project?.notionRootPageId);

  // ── 탭 2 mutations ─────────────────────────────────────────────
  const deleteMember = useDeleteProjectMember(numericProjectId);
  const updateRole = useUpdateProjectMemberRole(numericProjectId);

  // ── 탭 3 로컬 상태 + mutations ─────────────────────────────────
  const addCategory = useAddProjectCategory(numericProjectId);
  const updateCategoryType = useUpdateProjectCategoryType(numericProjectId);
  const deleteCategory = useDeleteProjectCategory(numericProjectId);

  // 카테고리 로컬 편집 상태: notionPageId → documentType | null
  const [categoryDraft, setCategoryDraft] = useState<Record<string, DocumentType | null>>({});

  useEffect(() => {
    const map: Record<string, DocumentType | null> = {};
    for (const page of notionCategoryPages) {
      if (!page.notionPageId) continue;
      const existing = categories.find((c) => c.notionPageId === page.notionPageId);
      map[page.notionPageId] = (existing?.documentType as DocumentType) ?? null;
    }
    setCategoryDraft(map);
  }, [notionCategoryPages, categories]);

  const handleCategoryTypeChange = (notionPageId: string, value: DocumentType | null) => {
    setCategoryDraft((prev) => ({ ...prev, [notionPageId]: value }));
    const existing = categories.find((c) => c.notionPageId === notionPageId);
    if (value === null) {
      if (existing) deleteCategory.mutate(existing.id);
    } else if (existing) {
      updateCategoryType.mutate({ categoryId: existing.id, documentType: value });
    } else {
      addCategory.mutate({ notionPageId, documentType: value });
    }
  };

  // ── 탭 4 로컬 상태 + mutation ─────────────────────────────────
  const updateTypeAssignees = useUpdateTypeAssignees(numericProjectId);
  const [assigneeDraft, setAssigneeDraft] = useState<Record<DocumentType, number | null>>(
    Object.fromEntries(DOCUMENT_TYPES.map((t) => [t, null])) as Record<DocumentType, number | null>,
  );

  useEffect(() => {
    if (typeAssignees.length === 0) return;
    setAssigneeDraft(
      Object.fromEntries(
        DOCUMENT_TYPES.map((t) => {
          const found = typeAssignees.find((a) => a.documentType === t);
          return [t, found?.assigneeMemberId ?? null];
        }),
      ) as Record<DocumentType, number | null>,
    );
  }, [typeAssignees]);

  const handleSaveAssignees = () => {
    updateTypeAssignees.mutate(
      DOCUMENT_TYPES.map((t) => ({ documentType: t, assigneeMemberId: assigneeDraft[t] })),
    );
  };

  // ── 렌더: 탭 1 기본 정보 ──────────────────────────────────────
  const renderTabInfo = () => (
    <div className="max-w-lg space-y-4">
      <div className="flex items-center gap-4">
        <label className="w-40 text-sm text-slate-500 shrink-0">프로젝트 이름</label>
        <div className="flex-1 border border-slate-200 rounded-md px-3 h-9 flex items-center bg-slate-50 text-sm text-slate-800 cursor-not-allowed select-none">
          {projectName}
        </div>
      </div>
      <div className="flex items-center gap-4">
        <label className="w-40 text-sm text-slate-500 shrink-0">Notion 루트 페이지</label>
        <div className="flex-1 border border-slate-200 rounded-md px-3 h-9 flex items-center bg-slate-50 text-sm text-slate-800 cursor-not-allowed select-none gap-1.5 truncate">
          {rootPageMeta?.icon?.type === 'EMOJI' && (
            <span>{rootPageMeta.icon.value}</span>
          )}
          <span>{rootPageMeta?.title ?? project?.notionRootPageId ?? '—'}</span>
        </div>
      </div>
    </div>
  );

  // ── 렌더: 탭 2 프로젝트 멤버 ─────────────────────────────────
  const renderTabMembers = () => {
    const members = project?.members ?? [];
    return (
      <>
        <p className="text-xs text-slate-500 mb-4">
          멤버 추가는 워크스페이스 멤버 ID가 필요하며 현재 API에서 제공되지 않습니다.
          삭제 및 역할 변경은 가능합니다.
        </p>
        <div className="w-full overflow-x-auto">
          <table className="w-full caption-bottom text-sm">
            <thead>
              <tr className="border-b">
                <th className={thCls}>이름</th>
                <th className={thCls}>역할</th>
                <th className={`${thCls} w-px`}> </th>
              </tr>
            </thead>
            <tbody>
              {members.map((row) => (
                <tr key={row.id} className="border-b hover:bg-gray-50 transition-colors">
                  <td className={tdCls}>{row.name}</td>
                  <td className={tdCls}>
                    <select
                      value={row.role}
                      onChange={(e) =>
                        updateRole.mutate({ memberId: row.id, role: e.target.value as 'ADMIN' | 'MEMBER' })
                      }
                      disabled={updateRole.isPending}
                      className={`${selectCls} w-28`}
                    >
                      <option value="ADMIN">ADMIN</option>
                      <option value="MEMBER">MEMBER</option>
                    </select>
                  </td>
                  <td className={`${tdCls} w-px`}>
                    <button
                      onClick={() => deleteMember.mutate(row.id)}
                      disabled={deleteMember.isPending}
                      className="inline-flex items-center justify-center w-7 h-7 rounded-md bg-red-500 hover:bg-red-600 transition-colors disabled:opacity-50"
                    >
                      {deleteMember.isPending ? (
                        <Loader2 size={14} className="text-white animate-spin" />
                      ) : (
                        <X size={14} className="text-white" strokeWidth={2.5} />
                      )}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {members.length === 0 && (
            <p className="text-sm text-gray-400 text-center py-6">멤버가 없습니다.</p>
          )}
        </div>
      </>
    );
  };

  // ── 렌더: 탭 3 카테고리 ───────────────────────────────────────
  const renderTabCategories = () => {
    const isMutating = addCategory.isPending || updateCategoryType.isPending || deleteCategory.isPending;
    return (
      <>
        {isMutating && (
          <p className="text-xs text-blue-600 mb-3 flex items-center gap-1">
            <Loader2 className="w-3 h-3 animate-spin" /> 저장 중...
          </p>
        )}
        <div className="w-full overflow-x-auto">
          <table className="w-full caption-bottom text-sm">
            <thead>
              <tr className="border-b">
                <th className={thCls}>페이지</th>
                <th className={thCls}>카테고리</th>
              </tr>
            </thead>
            <tbody>
              {notionCategoryPages.map((page) => {
                const pageId = page.notionPageId ?? '';
                const emoji = page.icon?.type === 'EMOJI' ? page.icon.value : '';
                return (
                  <tr key={pageId} className="border-b hover:bg-gray-50 transition-colors">
                    <td className={tdCls}>{emoji} {page.title}</td>
                    <td className={tdCls}>
                      <select
                        value={categoryDraft[pageId] ?? 'none'}
                        onChange={(e) => {
                          const v = e.target.value;
                          handleCategoryTypeChange(pageId, v === 'none' ? null : v as DocumentType);
                        }}
                        disabled={isMutating}
                        className={`${selectCls} w-48`}
                      >
                        <option value="none">카테고리 선정 안 함</option>
                        {DOCUMENT_TYPES.map((dt) => (
                          <option key={dt} value={dt}>{dt}</option>
                        ))}
                      </select>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
          {notionCategoryPages.length === 0 && (
            <p className="text-sm text-gray-400 text-center py-6">Notion 페이지를 불러오는 중입니다...</p>
          )}
        </div>
      </>
    );
  };

  // ── 렌더: 탭 4 담당자 ─────────────────────────────────────────
  const renderTabAssignees = () => {
    const members = workspace?.members ?? [];
    return (
      <>
        <div className="w-full overflow-x-auto">
          <table className="w-full caption-bottom text-sm">
            <thead>
              <tr className="border-b">
                <th className={thCls}>카테고리</th>
                <th className={thCls}>담당자</th>
              </tr>
            </thead>
            <tbody>
              {DOCUMENT_TYPES.map((type) => (
                <tr key={type} className="border-b hover:bg-gray-50 transition-colors">
                  <td className={tdCls}>{type}</td>
                  <td className={tdCls}>
                    <select
                      value={assigneeDraft[type] === null ? 'none' : String(assigneeDraft[type])}
                      onChange={(e) => {
                        const v = e.target.value;
                        setAssigneeDraft((prev) => ({ ...prev, [type]: v === 'none' ? null : Number(v) }));
                      }}
                      className={`${selectCls} w-40`}
                    >
                      <option value="none">담당자 없음</option>
                      {members.map((m) => (
                        <option key={m.userId} value={String(m.userId)}>{m.name}</option>
                      ))}
                    </select>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <div className="flex gap-3 mt-8">
          <button
            className={btnPrimary}
            onClick={handleSaveAssignees}
            disabled={updateTypeAssignees.isPending}
          >
            {updateTypeAssignees.isPending ? (
              <><Loader2 className="w-4 h-4 mr-2 animate-spin" />저장 중...</>
            ) : '적용하기'}
          </button>
        </div>
      </>
    );
  };

  return (
    <main className="flex-1 flex flex-col h-screen overflow-hidden bg-white font-sans">
      <header className="h-14 flex items-center px-6 border-b border-slate-200 bg-white shrink-0">
        <div className="flex items-center text-sm text-slate-500">
          <Link to={`/w/${workspaceId}`} className="px-2 py-1 rounded hover:bg-slate-100 cursor-pointer transition-colors hover:text-slate-900">
            {workspaceName}
          </Link>
          <span className="mx-1 text-[14px] opacity-40">/</span>
          <Link to={`/w/${workspaceId}/p/${projectId}/graph`} className="px-2 py-1 rounded hover:bg-slate-100 cursor-pointer transition-colors hover:text-slate-900">
            {projectName}
          </Link>
          <span className="mx-1 text-[14px] opacity-40">/</span>
          <span className="px-2 py-1 text-slate-900 font-medium">Project Settings</span>
        </div>
      </header>

      <div className="flex-1 overflow-y-auto">
        <div className="mx-auto w-full max-w-7xl px-8">
          <div className="flex">
            <nav className="w-52 shrink-0 pt-8 pb-16">
              <h1 className="text-2xl font-bold text-slate-900 mb-6">프로젝트 설정</h1>
              <ul className="space-y-1">
                {([
                  { id: 'info' as const, label: '기본 정보' },
                  { id: 'members' as const, label: '프로젝트 멤버' },
                  { id: 'categories' as const, label: '카테고리 등록' },
                  { id: 'assignees' as const, label: '카테고리별 담당자 지정' },
                ] as const).map((tab) => (
                  <li key={tab.id}>
                    <button
                      onClick={() => setActiveTab(tab.id)}
                      className={`w-full text-left px-3 py-2 rounded-md text-sm transition-colors ${
                        activeTab === tab.id
                          ? 'bg-slate-100 text-slate-900 font-medium'
                          : 'text-slate-600 hover:bg-slate-50 hover:text-slate-900'
                      }`}
                    >
                      {tab.label}
                    </button>
                  </li>
                ))}
              </ul>
            </nav>

            <div className="flex-1 pt-8 pb-16 pl-8">
              <h2 className="text-2xl text-slate-900 mb-6">
                {activeTab === 'info' && '기본 정보'}
                {activeTab === 'members' && '프로젝트 멤버'}
                {activeTab === 'categories' && '카테고리 등록'}
                {activeTab === 'assignees' && '카테고리별 담당자 지정'}
              </h2>
              {activeTab === 'info' && renderTabInfo()}
              {activeTab === 'members' && renderTabMembers()}
              {activeTab === 'categories' && renderTabCategories()}
              {activeTab === 'assignees' && renderTabAssignees()}
            </div>
          </div>
        </div>
      </div>
    </main>
  );
};

export default ProjectSettings;
