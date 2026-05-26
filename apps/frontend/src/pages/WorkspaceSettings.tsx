import { Navigate, useParams } from 'react-router-dom';

const WorkspaceSettings: React.FC = () => {
  const { workspaceId } = useParams<{ workspaceId: string }>();
  return <Navigate to={`/w/${workspaceId}`} replace />;
};

export default WorkspaceSettings;
