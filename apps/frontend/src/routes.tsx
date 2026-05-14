import { createBrowserRouter, Navigate } from 'react-router-dom'
import { Layout } from './components/Layout'
import { DependencyGraph } from './pages/DependencyGraph'
import { DocumentView } from './pages/DocumentView'
import { WorkspaceSelection } from './pages/WorkspaceSelection'

export const router = createBrowserRouter([
  {
    path: '/workspaces',
    element: <WorkspaceSelection />,
  },
  {
    path: '/w/:workspaceId',
    element: <Layout />,
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
    ],
  },
  {
    path: '*',
    element: <Navigate to="/workspaces" replace />,
  },
])