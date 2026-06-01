import { useNavigate, useParams } from 'react-router-dom';
import { useQueryClient, useMutation } from '@tanstack/react-query';
import { Plus, UserPlus, CheckCircle2, X, Users, Layers, FileText, Clock, AlertTriangle } from 'lucide-react';
import { TopAppBar } from '../components/TopAppBar';
import { useState } from 'react';
import { useWorkspaceDetail } from '../hooks/useWorkspaceDetail';
import { useProjects } from '../hooks/useProjects';
import { apiClient } from '../lib/apiClient';

const formatJoinedAt = (iso: string): string => {
  const d = new Date(iso);
  return `${d.getFullYear()}년 ${d.getMonth() + 1}월 ${d.getDate()}일`;
};

const formatRelativeTime = (iso: string | null | undefined): string => {
  if (!iso) return '동기화 전';
  const diff = Date.now() - new Date(iso).getTime();
  const mins = Math.floor(diff / 60000);
  if (mins < 1) return '방금 전';
  if (mins < 60) return `${mins}분 전`;
  const hours = Math.floor(mins / 60);
  if (hours < 24) return `${hours}시간 전`;
  const days = Math.floor(hours / 24);
  return `${days}일 전`;
};

const WorkspaceHome = () => {
  const { workspaceId } = useParams<{ workspaceId: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const wsId = workspaceId ? Number(workspaceId) : undefined;

  const { workspace, isLoading: wsLoading } = useWorkspaceDetail(wsId);
  const { projects, isLoading: projLoading } = useProjects(wsId);

  const [isInviteOpen, setIsInviteOpen] = useState(false);
  const [inviteEmail, setInviteEmail] = useState('');
  const [inviteSuccess, setInviteSuccess] = useState(false);

  const inviteMutation = useMutation({
    mutationFn: (email: string) =>
      apiClient.POST(`/api/workspaces/${wsId}/members`, { email }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['workspace', wsId] });
      setInviteSuccess(true);
      setTimeout(() => {
        setInviteSuccess(false);
        setInviteEmail('');
        setIsInviteOpen(false);
      }, 1800);
    },
  });

  const removeMutation = useMutation({
    mutationFn: (memberId: number) =>
      apiClient.DELETE(`/api/workspaces/${wsId}/members/${memberId}`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['workspace', wsId] });
    },
  });

  const handleInvite = () => {
    if (!inviteEmail.trim()) return;
    inviteMutation.mutate(inviteEmail.trim());
  };

  const handleRemoveMember = (userId: number) => {
    removeMutation.mutate(userId);
  };

  if (wsLoading || projLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="w-6 h-6 border-2 border-gray-300 border-t-gray-900 rounded-full animate-spin" />
      </div>
    );
  }

  const members = workspace?.members ?? [];

  return (
    <div className="bg-slate-50 text-slate-900 min-h-screen flex flex-col font-sans">
      <TopAppBar centerLabel={workspace?.name ?? '워크스페이스'} />

      <main className="flex-1 w-full max-w-[1200px] mx-auto px-6 py-10">

        {/* Section 1: 워크스페이스 아이콘 + 이름 */}
        <div className="flex items-center gap-4 mb-3">
          <div className="w-16 h-16 flex items-center justify-center bg-blue-100 text-blue-700 rounded-xl text-3xl font-bold shrink-0">
            {workspace?.name[0] ?? 'W'}
          </div>
          <h1 className="text-3xl font-bold leading-[1.2] tracking-tight text-slate-900">
            {workspace?.name ?? '워크스페이스'}
          </h1>
        </div>

        {/* Section 2: 워크스페이스 홈 + 통계 */}
        <div className="flex items-center gap-3 mb-12 flex-wrap">
          <span className="text-base text-slate-500 font-medium">워크스페이스 홈</span>

          <span className="text-slate-300 select-none">·</span>
          <span className="flex items-center gap-1.5 text-sm text-slate-500">
            <Users className="w-3.5 h-3.5 shrink-0" />
            {workspace?.memberCount ?? 0}명의 멤버
          </span>

          <span className="text-slate-300 select-none">·</span>
          <span className="flex items-center gap-1.5 text-sm text-slate-500">
            <Layers className="w-3.5 h-3.5 shrink-0" />
            {workspace?.projectCount ?? 0}개의 프로젝트
          </span>

          <span className="text-slate-300 select-none">·</span>
          <span className="flex items-center gap-1.5 text-sm text-slate-500">
            <FileText className="w-3.5 h-3.5 shrink-0" />
            {workspace?.documentCount ?? 0}개의 페이지
          </span>
        </div>

        {/* Section 3 & 4: 프로젝트 목록 */}
        <section>
          <div className="flex items-center justify-between mb-6">
            <h2 className="text-2xl font-bold text-slate-800">프로젝트 목록</h2>
            <button
              onClick={() => navigate(`/w/${workspaceId}/new-project`)}
              className="flex items-center gap-2 bg-blue-600 text-white px-5 py-2.5 rounded hover:bg-blue-700 transition-colors font-medium text-sm shadow-sm"
            >
              <Plus className="w-4 h-4" />
              새 프로젝트
            </button>
          </div>

          <div className="grid grid-cols-3 gap-6">
            {projects.map((project) => (
              <div
                key={project.id}
                onClick={() => navigate(`/w/${workspaceId}/p/${project.id}/graph`)}
                className="bg-white border border-slate-200 rounded-xl p-5 hover:shadow-md hover:border-blue-300 transition-all cursor-pointer group flex flex-col"
              >
                {/* 상단: 아이콘 + 충돌 알약 */}
                <div className="flex items-start justify-between mb-3">
                  {project.rootPageIcon?.type === 'EMOJI' ? (
                    <span className="text-3xl leading-none group-hover:scale-110 transition-transform origin-bottom-left inline-block">
                      {project.rootPageIcon.value}
                    </span>
                  ) : project.rootPageIcon?.type === 'EXTERNAL' || project.rootPageIcon?.type === 'FILE' ? (
                    <img
                      src={project.rootPageIcon.value}
                      alt=""
                      className="w-10 h-10 rounded-lg object-cover group-hover:scale-110 transition-transform origin-bottom-left"
                    />
                  ) : (
                    <div className="w-10 h-10 flex items-center justify-center bg-blue-100 text-blue-700 rounded-lg text-lg font-bold group-hover:scale-110 transition-transform origin-bottom-left">
                      {project.name[0]}
                    </div>
                  )}

                  {(project.unresolvedConflictCount ?? 0) > 0 && (
                    <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full bg-red-100 text-red-600 text-xs font-semibold shrink-0">
                      <AlertTriangle className="w-3 h-3" />
                      충돌 {project.unresolvedConflictCount}건
                    </span>
                  )}
                </div>

                {/* 프로젝트 이름 */}
                <h3 className="text-base font-bold text-slate-900 mb-4 flex-1">{project.name}</h3>

                {/* 하단: 멤버 수 · 페이지 수 / 최근 변경 시각 */}
                <div className="border-t border-slate-100 pt-3 flex items-center justify-between">
                  <div className="flex items-center gap-3 text-xs text-slate-400 font-medium">
                    <span className="flex items-center gap-1">
                      <Users className="w-3.5 h-3.5" />
                      멤버 {project.memberCount}명
                    </span>
                    <span className="flex items-center gap-1">
                      <FileText className="w-3.5 h-3.5" />
                      페이지 {project.documentCount}개
                    </span>
                  </div>
                  <span className="flex items-center gap-1 text-xs text-slate-400 font-medium">
                    <Clock className="w-3.5 h-3.5" />
                    {formatRelativeTime(project.lastNotionChangedAt)}
                  </span>
                </div>
              </div>
            ))}

            <div
              onClick={() => navigate(`/w/${workspaceId}/new-project`)}
              className="bg-white border border-dashed border-slate-300 rounded-xl p-5 hover:shadow-md hover:border-blue-300 transition-all cursor-pointer flex flex-col items-center justify-center min-h-[120px] gap-2"
            >
              <Plus className="w-8 h-8 text-slate-400" />
              <span className="text-sm text-slate-500">새 프로젝트</span>
            </div>
          </div>
        </section>

        {/* 워크스페이스 멤버 섹션 */}
        <section className="mt-14">
          <div className="flex items-center justify-between mb-6">
            <h2 className="text-2xl font-bold text-slate-800">워크스페이스 멤버</h2>
            <button
              onClick={() => setIsInviteOpen(true)}
              className="flex items-center gap-2 bg-white text-slate-700 border border-slate-200 px-4 py-2 rounded-lg hover:bg-slate-50 hover:border-slate-300 transition-all font-medium text-sm shadow-sm"
            >
              <UserPlus className="w-4 h-4" />
              멤버 초대하기
            </button>
          </div>

          <div className="bg-white border border-slate-200 rounded-xl overflow-hidden">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-slate-100 bg-slate-50">
                  <th className="text-left px-5 py-3 font-semibold text-slate-500 text-xs uppercase tracking-wide">이름</th>
                  <th className="text-left px-5 py-3 font-semibold text-slate-500 text-xs uppercase tracking-wide">이메일</th>
                  <th className="text-left px-5 py-3 font-semibold text-slate-500 text-xs uppercase tracking-wide">가입일</th>
                  <th className="whitespace-nowrap px-5 py-3" />
                </tr>
              </thead>
              <tbody>
                {members.map((member, idx) => (
                  <tr
                    key={member.userId}
                    className={`${idx !== members.length - 1 ? 'border-b border-slate-100' : ''} hover:bg-slate-50 transition-colors`}
                  >
                    <td className="px-5 py-3.5 font-medium text-slate-900">{member.name}</td>
                    <td className="px-5 py-3.5 text-slate-500">{member.email}</td>
                    <td className="px-5 py-3.5 text-slate-400">{formatJoinedAt(member.joinedAt)}</td>
                    <td className="px-5 py-3.5 text-right">
                      <button
                        onClick={() => handleRemoveMember(member.userId)}
                        className="text-xs text-red-400 bg-red-50 hover:text-red-600 hover:bg-red-100 whitespace-nowrap px-2.5 py-1 rounded-md transition-colors font-medium"
                      >
                        내보내기
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>

        {/* 멤버 초대 모달 */}
        {isInviteOpen && (
          <div
            className="fixed inset-0 z-50 flex items-center justify-center"
            style={{ backgroundColor: 'rgba(0,0,0,0.45)' }}
            onClick={(e) => { if (e.target === e.currentTarget) setIsInviteOpen(false); }}
          >
            <div className="bg-white rounded-2xl shadow-2xl w-full max-w-lg mx-4 p-8 relative flex flex-col items-center gap-5">

              <button
                onClick={() => { setIsInviteOpen(false); setInviteEmail(''); setInviteSuccess(false); }}
                className="absolute top-4 right-4 text-slate-400 hover:text-slate-600 transition-colors"
              >
                <X className="w-5 h-5" />
              </button>

              <div className="w-16 h-16 rounded-2xl bg-blue-50 flex items-center justify-center">
                <UserPlus className="w-8 h-8 text-blue-600" />
              </div>

              <div className="text-center">
                <h3 className="text-lg font-bold text-slate-900 leading-snug">
                  이 워크스페이스에서 함께할 멤버를 초대해보세요!
                </h3>
                <p className="text-sm text-slate-500 mt-1.5">
                  Notion에서 이 워크스페이스에 가입된 이메일로만 초대할 수 있습니다.
                </p>
              </div>

              <div className="w-full flex gap-2">
                <input
                  type="email"
                  value={inviteEmail}
                  onChange={(e) => setInviteEmail(e.target.value)}
                  onKeyDown={(e) => { if (e.key === 'Enter') handleInvite(); }}
                  placeholder="Notion 이메일 입력"
                  className="flex-1 h-10 rounded-lg border border-slate-200 px-3 text-sm placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition"
                />
                <button
                  onClick={handleInvite}
                  disabled={!inviteEmail.trim() || inviteSuccess || inviteMutation.isPending}
                  className="h-10 px-4 rounded-lg bg-blue-600 text-white text-sm font-semibold hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                >
                  초대
                </button>
              </div>

              {inviteSuccess && (
                <div className="flex items-center gap-2 text-emerald-600 text-sm font-medium bg-emerald-50 w-full justify-center py-2.5 rounded-lg">
                  <CheckCircle2 className="w-4 h-4" />
                  초대를 보냈습니다!
                </div>
              )}
            </div>
          </div>
        )}
      </main>

      <footer className="w-full mt-auto bg-slate-100 border-t border-slate-200">
        <div className="max-w-[1200px] mx-auto py-8 px-6 flex flex-col md:flex-row justify-between items-center">
          <div className="font-bold text-slate-500 text-sm mb-4 md:mb-0">© 2026 DocGraph. </div>
          <div className="flex gap-6">
            <a className="text-slate-500 hover:text-blue-600 text-sm transition-colors duration-200" href="#">
              개인정보 처리방침
            </a>
            <a className="text-slate-500 hover:text-blue-600 text-sm transition-colors duration-200" href="#">
              이용약관
            </a>
            <a className="text-slate-500 hover:text-blue-600 text-sm transition-colors duration-200" href="#">
              고객센터
            </a>
          </div>
        </div>
      </footer>
    </div>
  );
};

export default WorkspaceHome;
