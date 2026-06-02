import { type RefObject, useEffect, useRef, useState } from "react";
import { User, LogOut } from "lucide-react";
import { useAuth, useLogout } from "../hooks/useAuth";

// ── UserAvatar ────────────────────────────────────────────────────────────────
// API의 UserResponse.avatarUrl (Notion 프로필 사진 URL)을 받아 표시.
// avatarUrl이 null/undefined면 기본 아이콘(User) 표시.
type AvatarSize = 'xs' | 'md' | 'lg';

const sizeMap: Record<AvatarSize, string> = {
    xs: 'w-[18px] h-[18px]',
    md: 'w-8 h-8',
    lg: 'w-10 h-10',
};
const iconSizeMap: Record<AvatarSize, string> = {
    xs: 'w-3 h-3',
    md: 'w-5 h-5',
    lg: 'w-6 h-6',
};

export const UserAvatar = ({
    avatarUrl,
    size = 'md',
}: {
    avatarUrl?: string | null;
    size?: AvatarSize;
}) => {
    if (avatarUrl) {
        return (
            <img
                src={avatarUrl}
                alt="프로필"
                className={`${sizeMap[size]} rounded-full object-cover shrink-0`}
            />
        );
    }
    return (
        <div
            className={`${sizeMap[size]} rounded-full bg-slate-200 flex items-center justify-center shrink-0`}
        >
            <User className={`${iconSizeMap[size]} text-slate-500`} />
        </div>
    );
};

// ── UserProfilePopup ──────────────────────────────────────────────────────────

interface UserProfilePopupProps {
    isOpen: boolean;
    onClose: () => void;
    anchorRef: RefObject<HTMLElement | null>;
    placement?: 'above' | 'below';
}

export const UserProfilePopup = ({
    isOpen,
    onClose,
    anchorRef,
    placement = 'above',
}: UserProfilePopupProps) => {
    const popupRef = useRef<HTMLDivElement>(null);
    const [pos, setPos] = useState<{ top: number; left: number } | null>(null);

    useEffect(() => {
        if (!isOpen || !anchorRef.current) return;
        const rect = anchorRef.current.getBoundingClientRect();
        const popupWidth = 232;
        const popupHeight = 130;

        let top = placement === 'above'
            ? rect.top - popupHeight - 8
            : rect.bottom + 8;
        let left = rect.left;

        if (top < 8) top = rect.bottom + 8;
        if (top + popupHeight > window.innerHeight - 8) top = rect.top - popupHeight - 8;
        if (left + popupWidth > window.innerWidth - 8) left = window.innerWidth - popupWidth - 8;
        if (left < 8) left = 8;

        setPos({ top, left });
    }, [isOpen, anchorRef, placement]);

    useEffect(() => {
        if (!isOpen) return;
        const handleMouseDown = (e: MouseEvent) => {
            if (
                popupRef.current && !popupRef.current.contains(e.target as Node) &&
                anchorRef.current && !anchorRef.current.contains(e.target as Node)
            ) {
                onClose();
            }
        };
        document.addEventListener('mousedown', handleMouseDown);
        return () => document.removeEventListener('mousedown', handleMouseDown);
    }, [isOpen, onClose, anchorRef]);

    const { user } = useAuth();
    const logout = useLogout();

    if (!isOpen || !pos) return null;

    return (
        <div
            ref={popupRef}
            className="fixed z-[100] w-[232px] bg-white border border-slate-200 shadow-xl rounded-xl overflow-hidden"
            style={{ top: pos.top, left: pos.left }}
        >
            {/* 프로필 정보 */}
            <div className="flex items-center gap-3 px-4 py-4 border-b border-slate-100">
                <UserAvatar avatarUrl={user?.avatarUrl} size="lg" />
                <div className="min-w-0">
                    <p className="text-sm font-semibold text-slate-900 truncate">
                        {user?.name ?? ''}
                    </p>
                    <p className="text-xs text-slate-500 truncate">{user?.email ?? ''}</p>
                </div>
            </div>

            {/* 로그아웃 버튼 */}
            <div className="px-2 py-2">
                <button
                    onClick={() => { onClose(); logout.mutate(); }}
                    className="flex w-full items-center gap-2.5 px-3 py-2 rounded-lg text-sm text-slate-700 hover:bg-slate-100 transition-colors"
                >
                    <LogOut className="w-4 h-4 text-slate-500" />
                    로그아웃
                </button>
            </div>
        </div>
    );
};
