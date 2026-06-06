
import { createBrowserRouter, Navigate } from 'react-router-dom';
import AuthGuard from './components/AuthGuard';
import { Layout } from './components/Layout';
import { DependencyGraph } from './pages/DependencyGraph';
import { DocumentView } from './pages/DocumentView';
import { WorkspaceSelection } from './pages/WorkspaceSelection';
import WorkspaceHome from './pages/WorkspaceHome';
import WorkspaceSettings from './pages/WorkspaceSettings';
import NewProjectWizard from './pages/NewProjectWizard';
import ProjectSettings from './pages/ProjectSettings';
import LandingPage from './pages/LandingPage';

export const router = createBrowserRouter([
  {
    path: '/',
    element: <LandingPage />,
  },
  {
    element: <AuthGuard />,
    children: [
      {
        path: '/workspaces',
        element: <WorkspaceSelection />,
      },
      {
        path: '/w/:workspaceId',
        children: [
          {
            // WorkspaceHome — Sidebar 없이 독립 페이지
            index: true,
            element: <WorkspaceHome />,
          },
          {
            // pathless layout route — Sidebar가 필요한 하위 페이지 전체 래핑
            element: <Layout />,
            children: [
              {
                path: 'settings',
                element: <WorkspaceSettings />,
              },
              {
                path: 'new-project',
                element: <NewProjectWizard />,
              },
              {
                path: 'p/:projectId',
                children: [
                  {
                    index: true,
                    element: <Navigate to="graph" replace />,
                  },
                  {
                    path: 'graph',
                    element: <DependencyGraph />,
                  },
                  {
                    path: 'docs/:docId',
                    element: <DocumentView />,
                  },
                  {
                    path: 'settings',
                    element: <ProjectSettings />,
                  },
                ],
              },
            ],
          },
        ],
      },
    ],
  },
  {
    path: '*',
    element: <Navigate to="/workspaces" replace />,
  },
]);
