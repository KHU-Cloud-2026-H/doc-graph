import React, { useState, useEffect } from 'react';
import { Link, useParams, useNavigate } from 'react-router-dom';
import { X, Loader2, ExternalLink } from 'lucide-react';
import { useAppStore } from '../store';
import { useProjectDetail, useProjectCategories, useProjectTypeAssignees, useProjectValidation, useProjectAiUsage, useProjectRules } from '../hooks/useProjectDetail';
import type { RuleDetail } from '../hooks/useProjectDetail';
import { useWorkspaceDetail } from '../hooks/useWorkspaceDetail';
import { useNotionPageChildren, useNotionPageMetadata } from '../hooks/useNotionPages';
import {
  useAddProjectMember,
  useDeleteProjectMember,
  useUpdateProjectMemberRole,
  useAddProjectCategory,
  useUpdateProjectCategoryType,
  useDeleteProjectCategory,
  useUpdateTypeAssignees,
} from '../hooks/useProjectSettingsMutations';
import { useDeleteProject } from '../hooks/useDeleteProject';

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
  const navigate = useNavigate();
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

  type TabId = 'info' | 'members' | 'categories' | 'assignees' | 'ai-usage' | 'rules' | 'delete';
  const [activeTab, setActiveTab] = useState<TabId>('info');

  // ── 탭 1: 루트 페이지 메타데이터 + validation 토글 ──────────────
  const { page: rootPageMeta } = useNotionPageMetadata(numericWorkspaceId, project?.notionRootPageId);
  const { enabled: validationEnabled, toggle: validationToggle } = useProjectValidation(numericProjectId);

  // ── 탭 5: AI 사용량 ────────────────────────────────────────────
  const { usage: aiUsage } = useProjectAiUsage(numericProjectId);

  // ── 탭 6: 그래프 룰 ────────────────────────────────────────────
  const { rules, addRule, deleteRule } = useProjectRules(numericProjectId);
  const [newRuleSource, setNewRuleSource] = useState<string>('');
  const [newRuleTarget, setNewRuleTarget] = useState<string>('');
  const [newRuleCriterion, setNewRuleCriterion] = useState<string>('');

  // ── 탭 2 mutations + 로컬 상태 ────────────────────────────────
  const addMember = useAddProjectMember(numericProjectId);
  const deleteMember = useDeleteProjectMember(numericProjectId);
  const updateRole = useUpdateProjectMemberRole(numericProjectId);
  const [pendingMemberId, setPendingMemberId] = useState<number | null>(null);
  const [pendingRole, setPendingRole] = useState<'ADMIN' | 'MEMBER'>('MEMBER');

  // ── 탭 5 삭제 mutation ────────────────────────────────────────
  const deleteProject = useDeleteProject(numericProjectId);

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
          {rootPageMeta?.icon?.type === 'EMOJI' && <span>{rootPageMeta.icon.value}</span>}
          <span>{rootPageMeta?.title ?? project?.notionRootPageId ?? '—'}</span>
        </div>
      </div>
      <div className="flex items-center gap-4">
        <label className="w-40 text-sm text-slate-500 shrink-0">정합성 검증</label>
        <button
          onClick={() => validationToggle.mutate(!validationEnabled)}
          disabled={validationToggle.isPending}
          className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors focus:outline-none ${
            validationEnabled ? 'bg-blue-600' : 'bg-slate-200'
          } disabled:opacity-50`}
        >
          <span className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${
            validationEnabled ? 'translate-x-6' : 'translate-x-1'
          }`} />
        </button>
        <span className="text-sm text-slate-500">{validationEnabled ? '활성' : '비활성'}</span>
      </div>
    </div>
  );

  // ── 렌더: 탭 5 AI 사용량 ─────────────────────────────────────
  const renderTabAiUsage = () => (
    <div className="max-w-lg space-y-6">
      <div className="grid grid-cols-2 gap-4">
        {[
          { label: '총 호출 수', value: aiUsage?.totalCalls ?? 0 },
          { label: '총 토큰', value: (aiUsage?.totalTokens ?? 0).toLocaleString() },
          { label: 'Prompt 토큰', value: (aiUsage?.totalPromptTokens ?? 0).toLocaleString() },
          { label: 'Completion 토큰', value: (aiUsage?.totalCompletionTokens ?? 0).toLocaleString() },
        ].map((item) => (
          <div key={item.label} className="border border-slate-200 rounded-lg p-4 bg-slate-50">
            <p className="text-xs text-slate-500 mb-1">{item.label}</p>
            <p className="text-xl font-bold text-slate-900">{item.value}</p>
          </div>
        ))}
      </div>
      {(aiUsage?.byModel?.length ?? 0) > 0 && (
        <div>
          <h3 className="text-sm font-semibold text-slate-700 mb-3">모델별 분해</h3>
          <table className="w-full text-sm border-collapse">
            <thead>
              <tr className="border-b border-slate-200">
                <th className="text-left py-2 px-2 text-slate-500 font-medium">모델</th>
                <th className="text-right py-2 px-2 text-slate-500 font-medium">호출</th>
                <th className="text-right py-2 px-2 text-slate-500 font-medium">토큰</th>
              </tr>
            </thead>
            <tbody>
              {aiUsage?.byModel?.map((m, i) => (
                <tr key={i} className="border-b border-slate-100 hover:bg-slate-50">
                  <td className="py-2 px-2 font-mono text-xs text-slate-700">{m.model ?? '—'}</td>
                  <td className="py-2 px-2 text-right text-slate-700">{m.calls ?? 0}</td>
                  <td className="py-2 px-2 text-right text-slate-700">
                    {((m.promptTokens ?? 0) + (m.completionTokens ?? 0)).toLocaleString()}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );

  // ── 렌더: 탭 6 그래프 룰 ─────────────────────────────────────
  const renderTabRules = () => (
    <>
      <div className="w-full overflow-x-auto">
        <table className="w-full caption-bottom text-sm">
          <thead>
            <tr className="border-b">
              <th className="h-10 px-2 text-left text-sm font-medium text-gray-700">출발 타입</th>
              <th className="h-10 px-2 text-left text-sm font-medium text-gray-700">도착 타입</th>
              <th className="h-10 px-2 text-left text-sm font-medium text-gray-700">검증 기준</th>
              <th className="h-10 px-2 text-left text-sm font-medium text-gray-700">종류</th>
              <th className="h-10 px-2 w-px"> </th>
            </tr>
          </thead>
          <tbody>
            {rules.map((rule: RuleDetail) => (
              <tr key={rule.id} className="border-b hover:bg-gray-50">
                <td className="p-2 text-sm font-mono">{rule.sourceType}</td>
                <td className="p-2 text-sm font-mono">{rule.targetType}</td>
                <td className="p-2 text-sm text-slate-600 max-w-xs truncate">{rule.validationCriterion}</td>
                <td className="p-2 text-sm">
                  <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${rule.isDefault ? 'bg-slate-100 text-slate-500' : 'bg-blue-100 text-blue-700'}`}>
                    {rule.isDefault ? '기본' : '커스텀'}
                  </span>
                </td>
                <td className="p-2 w-px">
                  {!rule.isDefault && (
                    <button
                      onClick={() => deleteRule.mutate(rule.id)}
                      disabled={deleteRule.isPending}
                      className="inline-flex items-center justify-center w-7 h-7 rounded-md bg-red-500 hover:bg-red-600 transition-colors disabled:opacity-50"
                    >
                      <X size={14} className="text-white" strokeWidth={2.5} />
                    </button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {rules.length === 0 && <p className="text-sm text-gray-400 text-center py-6">룰이 없습니다.</p>}
      </div>

      <div className="mt-6 border-t border-slate-200 pt-6 space-y-3 max-w-lg">
        <h3 className="text-sm font-semibold text-slate-700">커스텀 룰 추가</h3>
        <div className="flex gap-2">
          <select value={newRuleSource} onChange={(e) => setNewRuleSource(e.target.value)}
            className="flex-1 h-9 rounded-md border border-gray-300 bg-white px-3 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500">
            <option value="">출발 타입</option>
            {DOCUMENT_TYPES.map((t) => <option key={t} value={t}>{t}</option>)}
          </select>
          <select value={newRuleTarget} onChange={(e) => setNewRuleTarget(e.target.value)}
            className="flex-1 h-9 rounded-md border border-gray-300 bg-white px-3 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500">
            <option value="">도착 타입</option>
            {DOCUMENT_TYPES.map((t) => <option key={t} value={t}>{t}</option>)}
          </select>
        </div>
        <input
          type="text"
          value={newRuleCriterion}
          onChange={(e) => setNewRuleCriterion(e.target.value)}
          placeholder="검증 기준 (예: 범위 일치 여부)"
          className="w-full h-9 rounded-md border border-gray-300 bg-white px-3 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
        />
        <button
          className={btnPrimary}
          disabled={!newRuleSource || !newRuleTarget || !newRuleCriterion.trim() || addRule.isPending}
          onClick={() => {
            addRule.mutate(
              { sourceType: newRuleSource, targetType: newRuleTarget, validationCriterion: newRuleCriterion.trim() },
              { onSuccess: () => { setNewRuleSource(''); setNewRuleTarget(''); setNewRuleCriterion(''); } },
            );
          }}
        >
          {addRule.isPending ? <><Loader2 className="w-4 h-4 mr-2 animate-spin inline" />추가 중...</> : '룰 추가'}
        </button>
      </div>
    </>
  );

  // ── 렌더: 탭 2 프로젝트 멤버 ─────────────────────────────────
  const renderTabMembers = () => {
    const members = project?.members ?? [];
    return (
      <>
        <p className="text-sm text-slate-500 mb-4 max-w-2xl leading-6">
          워크스페이스에 속해 있는 멤버만 프로젝트에 추가할 수 있습니다.
          <br />
          추가할 멤버가 목록에 표시되지 않는다면 먼저{' '}
          <Link to={`/w/${workspaceId}`} className="inline-flex items-center gap-1 text-blue-600 hover:underline">
            워크스페이스 홈
            <ExternalLink className="w-4 h-4" />
          </Link>
          으로 이동하여 멤버를 추가하세요!
        </p>
        <div className="flex items-end gap-3 mb-6">
          <select
            value={pendingMemberId !== null ? String(pendingMemberId) : ''}
            onChange={(e) => setPendingMemberId(e.target.value === '' ? null : Number(e.target.value))}
            className={`${selectCls} w-40`}
          >
            <option value="" disabled>멤버 선택</option>
            {(workspace?.members ?? [])
              .filter((m) => !members.some((pm) => pm.name === m.name))
              .map((m) => (
                <option key={m.workspaceMemberId} value={String(m.workspaceMemberId)}>{m.name}</option>
              ))}
          </select>
          <select
            value={pendingRole}
            onChange={(e) => setPendingRole(e.target.value as 'ADMIN' | 'MEMBER')}
            className={`${selectCls} w-28`}
          >
            <option value="ADMIN">ADMIN</option>
            <option value="MEMBER">MEMBER</option>
          </select>
          <button
            className={btnPrimary}
            disabled={pendingMemberId === null || addMember.isPending}
            onClick={() => {
              if (!pendingMemberId) return;
              addMember.mutate(
                { workspaceMemberId: pendingMemberId, role: pendingRole },
                { onSuccess: () => { setPendingMemberId(null); setPendingRole('MEMBER'); } },
              );
            }}
          >
            {addMember.isPending ? <><Loader2 className="w-4 h-4 mr-1 animate-spin inline" />추가 중...</> : '추가하기'}
          </button>
        </div>
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
                        <option key={m.workspaceMemberId} value={String(m.workspaceMemberId)}>{m.name}</option>
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

  // ── 렌더: 탭 5 프로젝트 삭제 ───────────────────────
  const renderTabDelete = () => {
    const handleDelete = () => {
      if (window.confirm('정말 삭제하시겠습니까?')) {
        deleteProject.mutate(undefined, {
          onSuccess: () => {
            navigate(`/w/${numericWorkspaceId}`);
          },
        });
      }
    };

    return (
      <div className="max-w-lg space-y-4">
        <div className="rounded-md border border-red-200 bg-red-50 p-4">
          <p className="text-sm text-red-900">
            프로젝트를 삭제하면 모든 문서, 그래프, 충돌 데이터가 영구 삭제됩니다.
          </p>
        </div>
        <button
          onClick={handleDelete}
          disabled={deleteProject.isPending}
          className={`inline-flex items-center justify-center rounded-md bg-red-600 px-4 py-2 
            text-sm font-medium text-white hover:bg-red-700 
            disabled:opacity-50 disabled:cursor-not-allowed transition-colors`}
        >
          {deleteProject.isPending ? (
            <><Loader2 className="w-4 h-4 mr-2 animate-spin" />삭제 중...</>
          ) : '프로젝트 삭제'}
        </button>
      </div>
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
                  { id: 'ai-usage' as const, label: 'AI 사용량' },
                  { id: 'rules' as const, label: '그래프 룰' },
                  { id: 'delete' as const, label: '프로젝트 삭제' },
                ] as const).map((tab) => (
                  <li key={tab.id}>
                    <button
                      onClick={() => setActiveTab(tab.id)}
                      className={`w-full text-left px-3 py-2 rounded-md text-sm transition-colors ${activeTab === tab.id
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
                {activeTab === 'ai-usage' && 'AI 사용량'}
                {activeTab === 'rules' && '그래프 룰'}
                {activeTab === 'delete' && '프로젝트 삭제'}
              </h2>
              {activeTab === 'info' && renderTabInfo()}
              {activeTab === 'members' && renderTabMembers()}
              {activeTab === 'categories' && renderTabCategories()}
              {activeTab === 'assignees' && renderTabAssignees()}
              {activeTab === 'ai-usage' && renderTabAiUsage()}
              {activeTab === 'rules' && renderTabRules()}
              {activeTab === 'delete' && renderTabDelete()}
            </div>
          </div>
        </div>
      </div>
    </main>
  );
};

export default ProjectSettings;
