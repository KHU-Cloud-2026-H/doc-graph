import { createBrowserRouter, Navigate } from 'react-router-dom'
import AuthGuard from './components/AuthGuard'

// TODO: feat/frontend-ui 머지 후 아래 import 활성화
// import { Layout } from './components/Layout'
// import { WorkspaceSelection } from './pages/WorkspaceSelection'
// import { DependencyGraph } from './pages/DependencyGraph'
// import { DocumentView } from './pages/DocumentView'

export const router = createBrowserRouter([
  {
    // 인증 게이트 — 미로그인 시 LoginPage 렌더, 로그인 시 children 진입
    element: <AuthGuard />,
    children: [
      {
        path: '/workspaces',
        element: <div />, // TODO: <WorkspaceSelection />
      },
      {
        path: '/w/:workspaceId',
        element: <div />, // TODO: <Layout />
        children: [
          { index: true, element: <Navigate to="graph" replace /> },
          { path: 'graph', element: <div /> }, // TODO: <DependencyGraph />
          { path: 'docs/:docId', element: <div /> }, // TODO: <DocumentView />
        ],
      },
    ],
  },
  {
    path: '*',
    element: <Navigate to="/workspaces" replace />,
  },
])
