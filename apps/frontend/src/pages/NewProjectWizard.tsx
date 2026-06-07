import React, { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { X, Loader2 } from 'lucide-react';
import StepIndicator from '../components/wizard/StepIndicator';
import { useNotionRootPages, useNotionPageChildren } from '../hooks/useNotionPages';
import { useWorkspaceDetail } from '../hooks/useWorkspaceDetail';
import { useCreateProject } from '../hooks/useProjectSettingsMutations';
import { apiClient } from '../lib/apiClient';

// 문서 타입 5종 (API 스키마 값과 일치)
const DOCUMENT_TYPES = [
  'meeting_notes',
  'planning',
  'requirements',
  'design',
  'research',
] as const;
type DocumentType = typeof DOCUMENT_TYPES[number];
type WorkspaceMember = { id: number; name: string };

// ── 스타일 상수 ──────────────────────────────────────
const inputCls =
  'h-9 w-full rounded-md border border-gray-300 bg-white px-3 py-2 text-sm placeholder:text-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500';
const selectCls =
  'h-9 rounded-md border border-gray-300 bg-white px-3 py-1 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 cursor-pointer';
const btnPrimary =
  'inline-flex items-center justify-center rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors';
const btnGhost =
  'inline-flex items-center justify-center rounded-md px-4 py-2 text-sm font-medium text-gray-600 hover:bg-gray-100 transition-colors';
const thCls = 'h-10 px-2 text-left text-sm font-medium text-gray-700 whitespace-nowrap';
const tdCls = 'p-2 align-middle text-sm whitespace-nowrap';

const NewProjectWizard: React.FC = () => {
  const { workspaceId } = useParams<{ workspaceId: string }>();
  const navigate = useNavigate();
  const wsId = workspaceId ? Number(workspaceId) : undefined;

  const { pages: notionRootPages } = useNotionRootPages(wsId);
  const { workspace } = useWorkspaceDetail(wsId);
  const workspaceMembers: WorkspaceMember[] = (workspace?.members ?? []).map(m => ({
    id: m.userId ?? 0,
    name: m.name ?? '',
  }));

  const [step, setStep] = useState<1 | 2 | 3 | 4>(1);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const createProject = useCreateProject(wsId);

  // ── 1단계 ──────────────────────────────────────────
  const [projectName, setProjectName] = useState('');
  const [notionRootPageId, setNotionRootPageId] = useState<string | null>(null);

  // ── 2단계 ──────────────────────────────────────────
  const { children: categoryPages } = useNotionPageChildren(wsId, notionRootPageId);
  const [categoryMap, setCategoryMap] = useState<Record<string, DocumentType | null>>({});

  useEffect(() => {
    setCategoryMap(Object.fromEntries(categoryPages.map(p => [p.notionPageId!, null])));
  }, [categoryPages]);

  // ── 3단계 ──────────────────────────────────────────
  const [projectMembers, setProjectMembers] = useState<
    Array<{ memberId: number; name: string; role: 'ADMIN' | 'MEMBER' }>
  >([]);
  const [pendingMemberId, setPendingMemberId] = useState<number | null>(null);
  const [pendingRole, setPendingRole] = useState<'ADMIN' | 'MEMBER'>('MEMBER');

  // ── 4단계 ──────────────────────────────────────────
  const [typeAssignees, setTypeAssignees] = useState<Record<DocumentType, number | null>>(
    Object.fromEntries(DOCUMENT_TYPES.map(t => [t, null])) as Record<DocumentType, number | null>
  );

  // ── 최종 제출 ─────────────────────────────────────────────────
  const handleFinish = async () => {
    if (!wsId || !notionRootPageId || !projectName.trim()) return;
    setIsSubmitting(true);
    let projectId: number | null = null;
    try {
      // 1. 프로젝트 생성 — 실패 시 중단
      const result = await createProject.mutateAsync({ name: projectName.trim(), notionRootPageId });
      projectId = result.id;
    } catch {
      setIsSubmitting(false);
      return;
    }

    // 2~4. 카테고리·담당자·sync — 실패해도 이동은 진행
    try {
      const categoryEntries = Object.entries(categoryMap).filter(([, type]) => type !== null);
      await Promise.all(
        categoryEntries.map(([notionPageId, documentType]) =>
          apiClient.POST(`/api/projects/${projectId}/categories`, { notionPageId, documentType }),
        ),
      );
    } catch { /* 카테고리 등록 실패는 무시 — 나중에 Project Settings에서 재설정 가능 */ }

    try {
      const assigneePayload = DOCUMENT_TYPES.map(t => ({
        documentType: t,
        assigneeMemberId: typeAssignees[t],
      }));
      await apiClient.PUT(`/api/projects/${projectId}/type-assignees`, { assignees: assigneePayload });
    } catch { /* 담당자 설정 실패 무시 */ }

    try {
      await apiClient.POST(`/api/projects/${projectId}/sync`);
    } catch { /* sync 실패 무시 — 그래프 화면에서 수동 sync 가능 */ }

    // sync가 백그라운드에서 실행되는 동안 최소 대기 후 이동
    await new Promise((r) => setTimeout(r, 3000));
    navigate(`/w/${workspaceId}/p/${projectId}/graph`);
  };

  const availableMembers = workspaceMembers.filter(
    m => !projectMembers.some(pm => pm.memberId === m.id)
  );

  const handleAddMember = () => {
    const member = workspaceMembers.find(m => m.id === pendingMemberId);
    if (!member) return;
    setProjectMembers(prev => [...prev, {
      memberId: member.id,
      name: member.name,
      role: pendingRole,
    }]);
    setPendingMemberId(null);
    setPendingRole('MEMBER');
  };

  const renderStep1 = () => (
    <>
      <h2 className="text-2xl font-bold text-gray-900 mb-2">프로젝트 생성</h2>
      <p className="text-sm text-gray-500 mb-8">
        Notion 루트 페이지를 기준으로 프로젝트를 생성할 수 있습니다.
      </p>

      <div className="space-y-6">
        <div className="space-y-2">
          <label htmlFor="projectName" className="block text-sm font-medium text-gray-700">
            새 프로젝트 이름
          </label>
          <input
            id="projectName"
            type="text"
            placeholder="프로젝트 이름을 입력하세요"
            value={projectName}
            onChange={e => setProjectName(e.target.value)}
            className={inputCls}
          />
        </div>

        <div className="space-y-2">
          <label className="block text-sm font-medium text-gray-700">
            Notion 루트 페이지 선택
          </label>
          <select
            value={notionRootPageId !== null ? String(notionRootPageId) : ''}
            onChange={e => setNotionRootPageId(e.target.value === '' ? null : e.target.value)}
            className={`${selectCls} w-full`}
          >
            <option value="" disabled>루트 페이지를 선택하세요</option>
            {notionRootPages.map(page => (
              <option key={page.notionPageId} value={page.notionPageId ?? ''}>
                {page.icon?.type === 'EMOJI' ? page.icon.value : ''} {page.title}
              </option>
            ))}
          </select>
        </div>
      </div>

      <div className="flex gap-3 mt-8">
        <button
          className={btnPrimary}
          disabled={projectName.trim() === '' || notionRootPageId === null}
          onClick={() => setStep(2)}
        >
          생성하기
        </button>
        <button
          className={btnGhost}
          onClick={() => navigate(`/w/${workspaceId}`)}
        >
          취소
        </button>
      </div>
    </>
  );

  const renderStep2 = () => (
    <>
      <h2 className="text-2xl font-bold text-gray-900 mb-2">카테고리 등록</h2>
      <p className="text-sm text-gray-500 mb-8">
        아래의 페이지들에 대해 카테고리를 등록해 주세요.
      </p>

      <div className="w-full overflow-x-auto">
        <table className="w-full caption-bottom text-sm">
          <thead className="[&_tr]:border-b">
            <tr className="border-b">
              <th className={thCls}>페이지</th>
              <th className={thCls}>카테고리</th>
            </tr>
          </thead>
          <tbody className="[&_tr:last-child]:border-0">
            {categoryPages.map(page => (
              <tr key={page.notionPageId} className="border-b hover:bg-gray-50 transition-colors">
                <td className={tdCls}>{page.icon?.type === 'EMOJI' ? page.icon.value : ''} {page.title}</td>
                <td className={tdCls}>
                  <select
                    value={categoryMap[page.notionPageId!] === null ? 'none' : (categoryMap[page.notionPageId!] ?? 'none')}
                    onChange={e => {
                      const value = e.target.value;
                      if (value === 'none') {
                        setCategoryMap(prev => ({ ...prev, [page.notionPageId!]: null }));
                      } else {
                        setCategoryMap(prev => ({ ...prev, [page.notionPageId!]: value as DocumentType }));
                      }
                    }}
                    className={`${selectCls} w-48`}
                  >
                    <option value="none">카테고리 선정 안 함</option>
                    {DOCUMENT_TYPES.map(dt => (
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
        <button className={btnPrimary} onClick={() => setStep(3)}>
          카테고리 등록 완료
        </button>
        <button className={btnGhost} onClick={() => setStep(1)}>
          뒤로가기
        </button>
      </div>
    </>
  );

  const renderStep3 = () => {
    const hasNoMembers = projectMembers.length === 0;
    const hasNoAdmin = projectMembers.length > 0 && !projectMembers.some(m => m.role === 'ADMIN');
    const canProceed = !hasNoMembers && !hasNoAdmin;
    return (
      <>
        <h2 className="text-2xl font-bold text-gray-900 mb-2">프로젝트 멤버 배정</h2>
        <p className="text-sm text-gray-500 mb-6">
          워크스페이스에 있는 멤버 중 이 프로젝트에 참여할 멤버를 배정해 주세요.
        </p>

        <div className="flex items-end gap-3 mb-6">
          <select
            value={pendingMemberId !== null ? String(pendingMemberId) : ''}
            onChange={e => setPendingMemberId(e.target.value === '' ? null : Number(e.target.value))}
            className={`${selectCls} w-40`}
          >
            <option value="" disabled>멤버 선택</option>
            {availableMembers.map(m => (
              <option key={m.id} value={String(m.id)}>{m.name}</option>
            ))}
          </select>
          <span className="text-sm text-gray-600 self-center">를</span>
          <select
            value={pendingRole}
            onChange={e => setPendingRole(e.target.value as 'ADMIN' | 'MEMBER')}
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

        <h3 className="text-base font-semibold text-gray-800 mb-3">프로젝트 멤버</h3>

        {hasNoMembers && (
          <p className="text-xs text-red-500 mb-3">한 명 이상의 멤버를 추가해야 합니다.</p>
        )}
        {hasNoAdmin && (
          <p className="text-xs text-red-500 mb-3">한 명 이상의 ADMIN을 추가해야 합니다.</p>
        )}

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
              {projectMembers.map(row => (
                <tr key={row.memberId} className="border-b hover:bg-gray-50 transition-colors">
                  <td className={tdCls}>{row.name}</td>
                  <td className={tdCls}>
                    <select
                      value={row.role}
                      onChange={e =>
                        setProjectMembers(prev =>
                          prev.map(m =>
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
                        setProjectMembers(prev => prev.filter(m => m.memberId !== row.memberId))
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
          <button className={btnPrimary} disabled={!canProceed} onClick={() => setStep(4)}>
            멤버 배정 완료
          </button>
          <button className={btnGhost} onClick={() => setStep(2)}>
            뒤로가기
          </button>
        </div>
      </>
    );
  };

  const renderStep4 = () => (
    <>
      <h2 className="text-2xl font-bold text-gray-900 mb-2">카테고리별 담당자 배정</h2>
      <p className="text-sm text-gray-500 mb-8">
        아래 5개 카테고리의 담당자를 지정해 주세요. (마지막 단계입니다!)
      </p>

      <div className="w-full overflow-x-auto">
        <table className="w-full caption-bottom text-sm">
          <thead className="[&_tr]:border-b">
            <tr className="border-b">
              <th className={thCls}>카테고리</th>
              <th className={thCls}>담당자</th>
            </tr>
          </thead>
          <tbody className="[&_tr:last-child]:border-0">
            {DOCUMENT_TYPES.map(type => (
              <tr key={type} className="border-b hover:bg-gray-50 transition-colors">
                <td className={tdCls}>{type}</td>
                <td className={tdCls}>
                  <select
                    value={typeAssignees[type] === null ? 'none' : String(typeAssignees[type])}
                    onChange={e => {
                      const value = e.target.value;
                      if (value === 'none') {
                        setTypeAssignees(prev => ({ ...prev, [type]: null }));
                      } else {
                        setTypeAssignees(prev => ({ ...prev, [type]: Number(value) }));
                      }
                    }}
                    className={`${selectCls} w-40`}
                  >
                    <option value="none">담당자 없음</option>
                    {workspaceMembers.map(m => (
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
        <button
          className={btnPrimary}
          disabled={isSubmitting}
          onClick={handleFinish}
        >
          {isSubmitting ? (
            <><Loader2 className="w-4 h-4 mr-2 animate-spin inline" />생성 중...</>
          ) : '배정 완료 및 초기 동기화 실행'}
        </button>
        <button className={btnGhost} onClick={() => setStep(3)} disabled={isSubmitting}>
          뒤로가기
        </button>
      </div>
    </>
  );

  return (
    /*
     * 외부: flex-1로 부모 채우기, relative로 absolute 기준점 생성
     * min-h-0: flex 자식의 기본 min-height:auto 제거
     */
    <div className="flex-1 flex flex-col relative min-h-0 bg-slate-50">

      {/*
       * StepIndicator: absolute 좌측 고정
       * top-16: fixed TopAppBar(64px) 보정 — 그 아래부터 배치
       * bottom-0: 콘텐츠 영역 하단까지
       * items-center: 보정된 가시 영역(top-16 ~ bottom) 기준 세로 정중앙
       * pointer-events-none/auto: absolute 요소가 카드 상호작용 방해하지 않도록
       */}
      <div className="absolute left-0 top-16 bottom-0 w-64 px-8 flex items-center justify-center pointer-events-none">
        <div className="pointer-events-auto">
          <StepIndicator currentStep={step} totalSteps={4} />
        </div>
      </div>

      {/*
       * 스크롤 가능 영역: 전체 너비 차지
       * flex-1 flex flex-col: 높이 채우기 + 내부 flex-1 활성화
       * overflow-y-auto: 내용 길면 스크롤
       * pt-16: fixed TopAppBar(64px) 보정 — 콘텐츠가 TopAppBar 아래부터 시작
       */}
      <div className="flex-1 overflow-y-auto flex flex-col pt-16">

        {/*
         * 센터링 컨테이너
         * flex-1: 스크롤 영역의 전체 높이 차지 → items-center 세로 정중앙 가능
         * py-[50px]: 카드 위아래 50px 여백
         *   - 내용 짧을 때: 카드 세로 정중앙, 위아래 50px 공간 보장
         *   - 내용 길 때: 이 div가 성장 → 부모가 스크롤
         */}
        <div className="flex flex-1 items-center justify-center py-[50px]">

          {/* wizard 카드: 전체 화면 기준 가로 정중앙 */}
          <div className="w-full max-w-xl bg-white rounded-xl border border-gray-200 shadow-sm p-8">
            {step === 1 && renderStep1()}
            {step === 2 && renderStep2()}
            {step === 3 && renderStep3()}
            {step === 4 && renderStep4()}
          </div>

        </div>
      </div>

    </div>
  );
};

export default NewProjectWizard;
