import { NOTION_LOGIN_URL } from '../../lib/apiClient'

export default function LoginPage() {
  const handleLogin = () => {
    window.location.href = NOTION_LOGIN_URL
  }

  return (
    <div className="min-h-screen bg-gray-50 flex items-center justify-center">
      <div className="bg-white rounded-2xl shadow-lg p-10 flex flex-col items-center gap-6 w-full max-w-sm">
        <div className="flex flex-col items-center gap-2">
          <h1 className="text-2xl font-bold text-gray-900">DocGraph</h1>
          <p className="text-sm text-gray-500 text-center">
            Notion 문서 간 충돌을 감지하고 관리하세요
          </p>
        </div>

        <button
          onClick={handleLogin}
          className="w-full flex items-center justify-center gap-3 bg-black text-white rounded-lg px-4 py-3 text-sm font-medium hover:bg-gray-800 transition-colors"
        >
          <NotionIcon />
          Notion으로 로그인
        </button>

        <p className="text-xs text-gray-400 text-center">
          로그인하면 Notion 워크스페이스에 대한 읽기 권한이 요청됩니다
        </p>
      </div>
    </div>
  )
}

function NotionIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 100 100" fill="none" xmlns="http://www.w3.org/2000/svg">
      <path
        d="M6.017 4.313l55.333-4.087c6.797-.583 8.543-.19 12.817 2.917l17.663 12.443c2.913 2.14 3.883 2.723 3.883 5.053v68.243c0 4.277-1.553 6.807-6.99 7.193L24.467 99.967c-4.08.193-6.023-.387-8.16-3.113L3.147 80.48C.807 77.367 0 75.033 0 72.117V11.113c0-3.497 1.553-6.413 6.017-6.8z"
        fill="#fff"
      />
      <path
        d="M61.35.227l-55.333 4.086C1.553 4.7 0 7.617 0 11.113v61.004c0 2.916.807 5.25 3.147 8.363l13.16 16.374c2.137 2.726 4.08 3.306 8.16 3.113l64.257-3.89c5.433-.387 6.99-2.917 6.99-7.193V29.853c0-2.21-.873-2.847-3.443-4.733L74.167 12.67c-4.274-3.107-6.02-3.5-12.817-2.917z"
        fill="#fff"
      />
      <path
        fillRule="evenodd"
        clipRule="evenodd"
        d="M61.35.227l-55.333 4.086C1.553 4.7 0 7.617 0 11.113v61.004c0 2.916.807 5.25 3.147 8.363l13.16 16.374c2.137 2.726 4.08 3.306 8.16 3.113l64.257-3.89c5.433-.387 6.99-2.917 6.99-7.193V29.853c0-2.21-.873-2.847-3.443-4.733L74.167 12.67c-4.274-3.107-6.02-3.5-12.817-2.917zM25.92 19.523c-5.247.353-6.437.433-9.417-1.99L8.927 11.507c-.77-.78-.387-1.753.98-1.946l53.327-3.89c4.467-.387 6.793 1.167 8.54 2.527l9.123 6.61c.39.197 1.36 1.36.193 1.36l-54.933 3.307-.237.048zM19.153 88.somethingv-57.8c0-2.72.777-3.5 3.5-3.696L86 23.307c2.527-.193 3.5 1.166 3.5 3.693v57.8c0 2.52-.97 4.083-3.5 4.273L22.847 92.8c-2.72.193-3.693-.973-3.693-4.28zm62.333-51.86c.387 1.75 0 3.5-1.75 3.7l-2.913.577v42.773c-2.527 1.36-4.853 2.137-6.797 2.137-3.113 0-3.89-1.166-6.21-3.89l-19.03-29.94v28.967l6.02 1.363s0 3.5-4.853 3.5l-13.373.777c-.387-.777 0-2.72 1.357-3.11l3.497-.97V36.553L30.78 36.17c-.387-1.75.586-4.277 3.3-4.473l14.347-.967 19.8 30.327V33.64l-5.047-.58c-.387-2.143 1.163-3.7 3.103-3.89l13.203-.777z"
        fill="#000"
      />
    </svg>
  )
}
