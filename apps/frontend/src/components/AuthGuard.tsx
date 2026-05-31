import { Outlet } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'
import LoginPage from '../features/auth/LoginPage'

export default function AuthGuard() {
  const { isLoading, isLoggedIn } = useAuth()

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="w-6 h-6 border-2 border-gray-300 border-t-gray-900 rounded-full animate-spin" />
      </div>
    )
  }

  if (!isLoggedIn) {
    return <LoginPage />
  }

  return <Outlet />
}
