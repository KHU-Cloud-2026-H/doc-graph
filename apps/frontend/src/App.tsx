import { useState } from 'react'
import { hasToken, setToken, clearToken } from './lib/tokenStorage'

function App() {
  const [tokenStatus, setTokenStatus] = useState(hasToken())

  const handleSetToken = () => {
    setToken('test-token-12345')
    setTokenStatus(hasToken())
  }

  const handleClearToken = () => {
    clearToken()
    setTokenStatus(hasToken())
  }

  return (
    <div className="min-h-screen bg-gray-100 flex items-center justify-center">
      <div className="bg-white p-8 rounded-lg shadow-lg space-y-4 max-w-md">
        <h1 className="text-3xl font-bold text-blue-600">DocGraph</h1>
        
        <div className="space-y-2">
          <p className="text-gray-700">
            토큰 상태: 
            <span className={`ml-2 font-bold ${tokenStatus ? 'text-green-600' : 'text-red-600'}`}>
              {tokenStatus ? '✓ 있음' : '✗ 없음'}
            </span>
          </p>
          
          <p className="text-sm text-gray-500">
            API URL: {import.meta.env.VITE_API_BASE_URL || '(설정 안 됨)'}
          </p>
        </div>

        <div className="space-x-2">
          <button
            onClick={handleSetToken}
            className="bg-blue-500 text-white px-4 py-2 rounded hover:bg-blue-600 transition"
          >
            토큰 저장
          </button>
          <button
            onClick={handleClearToken}
            className="bg-red-500 text-white px-4 py-2 rounded hover:bg-red-600 transition"
          >
            토큰 삭제
          </button>
        </div>

        <p className="text-xs text-gray-400">
          ⓘ 인프라 테스트용 화면. 곧 진짜 화면으로 교체될 예정.
        </p>
      </div>
    </div>
  )
}

export default App