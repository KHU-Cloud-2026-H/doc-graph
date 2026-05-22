import React from 'react';
import { Link, useParams } from 'react-router-dom';
import { useAppStore } from '../store';

const WorkspaceHome: React.FC = () => {
  const { workspaceId } = useParams<{ workspaceId: string }>();
  const { projects } = useAppStore();

  const workspaceProjects = projects.filter(
    (p) => p.workspaceId === Number(workspaceId)
  );

  return (
    <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '60vh', fontSize: 24 }}>
      <div>
        <div>이 페이지는 다음 PR에서 구현 예정입니다</div>
        <div style={{ marginTop: 16, fontWeight: 600 }}>WorkspaceHome</div>
        <div style={{ marginTop: 24, display: 'flex', flexDirection: 'column', gap: 8 }}>
          {workspaceProjects.map((project) => (
            <Link
              key={project.id}
              to={`/w/${workspaceId}/p/${project.id}/graph`}
              className="text-blue-600 underline text-base"
            >
              → {project.name} 그래프 보기
            </Link>
          ))}
        </div>
      </div>
    </div>
  );
};

export default WorkspaceHome;
