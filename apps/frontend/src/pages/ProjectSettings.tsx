import React, { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { X } from 'lucide-react';
import { useAppStore } from '../store';

// ── Mock 데이터 ────────────────────────────────────────────────
// TODO(API): GET /projects/{id} → ProjectDetail.notionRootPageId 사용.
//            notionRootPageId는 Notion UUID string이며, 페이지 title/icon은
//            현재 API 미제공 → 백엔드에 ProjectDetail에 notionRootPageTitle/
//            notionRootPageIcon 필드 추가 요청 예정.
const MOCK_NOTION_ROOT_PAGES = [
  { id: 0, emoji: '🏠', title: '프로젝트' },
] as const;

// TODO(API): GET /projects/{id}/categories → CategoryResponse[] 사용.
//            CategoryResponse에 title/icon 미제공 → 백엔드에 추가 요청 예정.
const MOCK_CATEGORY_PAGES = [
  { id: 1, emoji: '💡', title: '기획' },
  { id: 6, emoji: '📅', title: '회의록' },
  { id: 11, emoji: '🔷', title: '설계' },
  { id: 16, emoji: '🔍', title: 'QA및테스트' },
  { id: 21, emoji: '⚙️', title: '프론트엔드' },
] as const;

const DOCUMENT_TYPES = [
  'meeting_notes', 'planning', 'requirements', 'design', 'research',
] as const;
type DocumentType = typeof DOCUMENT_TYPES[number];

// TODO(API): GET /workspaces/{id} → WorkspaceDetail.members[] (WorkspaceMemberSummary) 사용.
const MOCK_WORKSPACE_MEMBERS = [
  { id: 1, name: '박관우' },
  { id: 2, name: '서영채' },
  { id: 3, name: '신정환' },
  { id: 4, name: '전현준' },
  { id: 5, name: '이창민' },
  { id: 6, name: '김연길' },
  { id: 7, name: '이주안' },
] as const;
type WorkspaceMember = { id: number; name: string };

// ── 스타일 상수 (NewProjectWizard.tsx와 동일) ───────────────────
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
  const workspaces = useAppStore((state) => state.workspaces);
  const projects = useAppStore((state) => state.projects);
  const workspaceName = workspaces.find((w) => w.id === Number(workspaceId))?.name ?? '워크스페이스';
  const projectName = projects.find((p) => p.id === Number(projectId))?.name ?? '프로젝트';

  type TabId = 'info' | 'members' | 'categories' | 'assignees';
  const [activeTab, setActiveTab] = useState<TabId>('info');

  // ── 탭 2: 프로젝트 멤버 ─────────────────────────────────────────
  // TODO(API): GET /projects/{id} → ProjectDetail.members[] (ProjectMemberSummary) 로 초기화.
  // TODO(API): POST   /projects/{id}/members         (AssignMemberRequest: workspaceMemberId, role)
  // TODO(API): DELETE /projects/{id}/members/{memberId}
  // TODO(API): PATCH  /projects/{id}/members/{memberId} (UpdateProjectMemberRoleRequest: role)
  const [projectMembers, setProjectMembers] = useState<
    Array<{ memberId: number; name: string; role: 'ADMIN' | 'MEMBER' }>
  >([
    { memberId: 1, name: '박관우', role: 'ADMIN' },
    { memberId: 2, name: '서영채', role: 'MEMBER' },
  ]);
  const [pendingMemberId, setPendingMemberId] = useState<number | null>(null);
  const [pendingRole, setPendingRole] = useState<'ADMIN' | 'MEMBER'>('MEMBER');

  const availableMembers = (MOCK_WORKSPACE_MEMBERS as readonly WorkspaceMember[]).filter(
    (m) => !projectMembers.some((pm) => pm.memberId === m.id)
  );

  const handleAddMember = () => {
    const member = MOCK_WORKSPACE_MEMBERS.find((m) => m.id === pendingMemberId);
    if (!member) return;
    setProjectMembers((prev) => [...prev, { memberId: member.id, name: member.name, role: pendingRole }]);
    setPendingMemberId(null);
    setPendingRole('MEMBER');
  };

  // ── 탭 3: 카테고리 등록 ─────────────────────────────────────────
  // TODO(API): GET /projects/{id}/categories → CategoryResponse[] (id, notionPageId, documentType) 로 초기화.
  // TODO(API): POST   /projects/{id}/categories        (RegisterCategoryRequest: notionPageId, documentType)
  // TODO(API): DELETE /projects/{id}/categories/{categoryId}
  // TODO(API): PATCH  /projects/{id}/categories/{categoryId} (ChangeCategoryTypeRequest: documentType)
  const [categoryMap, setCategoryMap] = useState<Record<number, DocumentType | null>>({
    1: 'planning', 6: 'meeting_notes', 11: 'design', 16: 'research', 21: null,
  });

  // ── 탭 4: 카테고리별 담당자 지정 ────────────────────────────────
  // TODO(API): GET /projects/{id}/type-assignees → TypeAssigneeResponse[] (documentType, assigneeMemberId) 로 초기화.
  // TODO(API): PUT /projects/{id}/type-assignees (UpdateTypeAssigneesRequest: assignees[])
  const [typeAssignees, setTypeAssignees] = useState<Record<DocumentType, number | null>>(
    Object.fromEntries(DOCUMENT_TYPES.map((t) => [t, null])) as Record<DocumentType, number | null>
  );

  // ── 렌더 함수: 탭 1 — 기본 정보 ─────────────────────────────────
  const renderTabInfo = () => (
    <div className="max-w-lg space-y-4">
      {/* 프로젝트 이름 */}
      <div className="flex items-center gap-4">
        <label className="w-40 text-sm text-slate-500 shrink-0">프로젝트 이름</label>
        <div className="group relative flex items-center flex-1">
          <div className="flex-1 border border-slate-200 rounded-md px-3 h-9 flex items-center bg-slate-50 text-sm text-slate-800 cursor-not-allowed select-none">
            {projectName}
          </div>
        </div>
      </div>

      {/* Notion 루트 페이지 */}
      <div className="flex items-center gap-4">
        <label className="w-40 text-sm text-slate-500 shrink-0">Notion 루트 페이지</label>
        <div className="group relative flex items-center flex-1">
          <div className="flex-1 border border-slate-200 rounded-md px-3 h-9 flex items-center bg-slate-50 text-sm text-slate-800 cursor-not-allowed select-none">
            {MOCK_NOTION_ROOT_PAGES[0].emoji} {MOCK_NOTION_ROOT_PAGES[0].title}
          </div>
        </div>
      </div>
    </div>
  );

  // ── 렌더 함수: 탭 2 — 프로젝트 멤버 ─────────────────────────────
  const renderTabMembers = () => (
    <>
      <div className="flex items-end gap-3 mb-6">
        <select
          value={pendingMemberId !== null ? String(pendingMemberId) : ''}
          onChange={(e) => setPendingMemberId(e.target.value === '' ? null : Number(e.target.value))}
          className={`${selectCls} w-40`}
        >
          <option value="" disabled>멤버 선택</option>
          {availableMembers.map((m) => (
            <option key={m.id} value={String(m.id)}>{m.name}</option>
          ))}
        </select>
        <span className="text-sm text-gray-600 self-center">를</span>
        <select
          value={pendingRole}
          onChange={(e) => setPendingRole(e.target.value as 'ADMIN' | 'MEMBER')}
          className={`${selectCls} w-28`}
        >
          <option value="ADMIN">ADMIN</option>
          <option value="MEMBER">MEMBER</option>
        </select>
        <span className="text-sm text-gray-600 self-center">으로</span>
        <button
          className={btnPrimary}
          disabled={pendingMemberId === null}
          onClick={handleAddMember}
        >
          추가하기
        </button>
      </div>

      <div className="border-t border-gray-200 my-6" />

      <h3 className="text-base font-semibold text-gray-800 mb-3">현재 프로젝트 멤버</h3>

      <div className="w-full overflow-x-auto">
        <table className="w-full caption-bottom text-sm">
          <thead className="[&_tr]:border-b">
            <tr className="border-b">
              <th className={thCls}>이름</th>
              <th className={thCls}>역할</th>
              <th className={`${thCls} w-px text-right`}> </th>
            </tr>
          </thead>
          <tbody className="[&_tr:last-child]:border-0">
            {projectMembers.map((row) => (
              <tr key={row.memberId} className="border-b hover:bg-gray-50 transition-colors">
                <td className={tdCls}>{row.name}</td>
                <td className={tdCls}>
                  <select
                    value={row.role}
                    onChange={(e) =>
                      setProjectMembers((prev) =>
                        prev.map((m) =>
                          m.memberId === row.memberId
                            ? { ...m, role: e.target.value as 'ADMIN' | 'MEMBER' }
                            : m
                        )
                      )
                    }
                    className={`${selectCls} w-28`}
                  >
                    <option value="ADMIN">ADMIN</option>
                    <option value="MEMBER">MEMBER</option>
                  </select>
                </td>
                <td className={`${tdCls} w-px text-right`}>
                  <button
                    onClick={() =>
                      setProjectMembers((prev) => prev.filter((m) => m.memberId !== row.memberId))
                    }
                    className="inline-flex items-center justify-center w-7 h-7 rounded-md bg-red-500 hover:bg-red-600 transition-colors cursor-pointer"
                  >
                    <X size={14} className="text-white" strokeWidth={2.5} />
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {projectMembers.length === 0 && (
        <p className="text-sm text-gray-400 text-center py-6">추가된 멤버가 없습니다.</p>
      )}

      <div className="flex gap-3 mt-8">
        <button className={btnPrimary} onClick={() => alert('저장되었습니다.')}>
          적용하기
        </button>
      </div>
    </>
  );

  // ── 렌더 함수: 탭 3 — 카테고리 등록 ─────────────────────────────
  const renderTabCategories = () => (
    <>
      <div className="w-full overflow-x-auto">
        <table className="w-full caption-bottom text-sm">
          <thead className="[&_tr]:border-b">
            <tr className="border-b">
              <th className={thCls}>페이지</th>
              <th className={thCls}>카테고리</th>
            </tr>
          </thead>
          <tbody className="[&_tr:last-child]:border-0">
            {MOCK_CATEGORY_PAGES.map((page) => (
              <tr key={page.id} className="border-b hover:bg-gray-50 transition-colors">
                <td className={tdCls}>{page.emoji} {page.title}</td>
                <td className={tdCls}>
                  <select
                    value={categoryMap[page.id] === null ? 'none' : categoryMap[page.id]!}
                    onChange={(e) => {
                      const value = e.target.value;
                      if (value === 'none') {
                        setCategoryMap((prev) => ({ ...prev, [page.id]: null }));
                      } else {
                        setCategoryMap((prev) => ({ ...prev, [page.id]: value as DocumentType }));
                      }
                    }}
                    className={`${selectCls} w-48`}
                  >
                    <option value="none">카테고리 선정 안 함</option>
                    {DOCUMENT_TYPES.map((dt) => (
                      <option key={dt} value={dt}>{dt}</option>
                    ))}
                  </select>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="flex gap-3 mt-8">
        <button className={btnPrimary} onClick={() => alert('저장되었습니다.')}>
          적용하기
        </button>
      </div>
    </>
  );

  // ── 렌더 함수: 탭 4 — 카테고리별 담당자 지정 ────────────────────
  const renderTabAssignees = () => (
    <>
      <div className="w-full overflow-x-auto">
        <table className="w-full caption-bottom text-sm">
          <thead className="[&_tr]:border-b">
            <tr className="border-b">
              <th className={thCls}>카테고리</th>
              <th className={thCls}>담당자</th>
            </tr>
          </thead>
          <tbody className="[&_tr:last-child]:border-0">
            {DOCUMENT_TYPES.map((type) => (
              <tr key={type} className="border-b hover:bg-gray-50 transition-colors">
                <td className={tdCls}>{type}</td>
                <td className={tdCls}>
                  <select
                    value={typeAssignees[type] === null ? 'none' : String(typeAssignees[type])}
                    onChange={(e) => {
                      const value = e.target.value;
                      if (value === 'none') {
                        setTypeAssignees((prev) => ({ ...prev, [type]: null }));
                      } else {
                        setTypeAssignees((prev) => ({ ...prev, [type]: Number(value) }));
                      }
                    }}
                    className={`${selectCls} w-40`}
                  >
                    <option value="none">담당자 없음</option>
                    {(MOCK_WORKSPACE_MEMBERS as readonly WorkspaceMember[]).map((m) => (
                      <option key={m.id} value={String(m.id)}>{m.name}</option>
                    ))}
                  </select>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="flex gap-3 mt-8">
        <button className={btnPrimary} onClick={() => alert('저장되었습니다.')}>
          적용하기
        </button>
      </div>
    </>
  );

  return (
    <main className="flex-1 flex flex-col h-screen overflow-hidden bg-white font-sans">
      {/* 브레드크럼 헤더 */}
      <header className="h-14 flex items-center px-6 border-b border-slate-200 bg-white shrink-0">
        <div className="flex items-center text-sm text-slate-500">
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
          <span className="mx-1 text-[14px] opacity-40">/</span>
          <span className="px-2 py-1 text-slate-900 font-medium">Project Settings</span>
        </div>
      </header>

      {/* 본문: 좌측 탭 네비 + 우측 콘텐츠 */}
      <div className="flex-1 overflow-y-auto">
        <div className="mx-auto w-full max-w-7xl px-8">
          <div className="flex">
            {/* 좌측 탭 네비게이션 */}
            <nav className="w-52 shrink-0 pt-8 pb-16">
              <h1 className="text-2xl font-bold text-slate-900 mb-6">프로젝트 설정</h1>
              <ul className="space-y-1">
                {(
                  [
                    { id: 'info' as const, label: '기본 정보' },
                    { id: 'members' as const, label: '프로젝트 멤버' },
                    { id: 'categories' as const, label: '카테고리 등록' },
                    { id: 'assignees' as const, label: '카테고리별 담당자 지정' },
                  ]
                ).map((tab) => (
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

            {/* 우측 콘텐츠 */}
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
