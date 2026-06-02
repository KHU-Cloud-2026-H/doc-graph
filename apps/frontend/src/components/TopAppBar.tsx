import { useRef, useState } from "react";
import { Link } from "react-router-dom";
import { Settings, Inbox } from "lucide-react";
import { InboxPopup } from "./InboxPopup";
import { useInboxActiveCount } from "./InboxList";
import { UserProfilePopup, UserAvatar } from "./UserProfilePopup";

interface TopAppBarProps {
  centerLabel: string;
  centerIcon?: string;
}

const NOTION_LOGO =
  "https://upload.wikimedia.org/wikipedia/commons/thumb/e/e9/Notion-logo.svg/3840px-Notion-logo.svg.png";

export const TopAppBar = ({ centerLabel, centerIcon }: TopAppBarProps) => {
  const unreadCount = useInboxActiveCount();
  const [isInboxOpen, setIsInboxOpen] = useState(false);
  const inboxButtonRef = useRef<HTMLButtonElement>(null);
  const [isProfileOpen, setIsProfileOpen] = useState(false);
  const profileButtonRef = useRef<HTMLButtonElement>(null);

  return (
    <header className="flex justify-between items-center w-full px-6 h-16 sticky top-0 z-50 bg-white border-b border-slate-200 antialiased">
      <div className="flex items-center gap-6">
        <Link to="/workspaces" className="font-bold tracking-tight text-slate-900 text-2xl hover:text-blue-600 transition-colors">
          DocGraph
        </Link>
      </div>

      <div className="flex justify-center max-w-md mx-4 absolute left-1/2 -translate-x-1/2">
        <button className="flex items-center gap-2 bg-slate-50 hover:bg-slate-100 px-4 py-2 rounded-full border border-slate-200 transition-colors text-sm font-medium">
          <img
            alt="Notion logo"
            className="w-5 h-5 object-contain opacity-80"
            src={centerIcon ?? NOTION_LOGO}
          />
          <span className="text-slate-700">{centerLabel}</span>
        </button>
      </div>

      <div className="flex items-center text-slate-600 font-medium gap-2">
        <button className="hover:bg-slate-100 transition-colors duration-200 p-2 rounded-full hover:text-slate-900 text-slate-600 flex items-center justify-center">
          <Settings className="w-5 h-5" />
        </button>
        <button
          ref={inboxButtonRef}
          onClick={() => setIsInboxOpen((v) => !v)}
          className="hover:bg-slate-100 transition-colors duration-200 p-2 rounded-full hover:text-slate-900 text-slate-600 flex items-center justify-center relative"
        >
          <Inbox className="w-5 h-5" />
          {unreadCount > 0 && (
            <span className="absolute top-1 right-1 min-w-[16px] h-4 bg-red-600 text-white text-[10px] font-bold px-1 rounded-full flex items-center justify-center leading-none">
              {unreadCount}
            </span>
          )}
        </button>
        <button
          ref={profileButtonRef}
          onClick={() => setIsProfileOpen(!isProfileOpen)}
          className="hover:bg-slate-100 transition-colors duration-200 p-0.5 rounded-full flex items-center justify-center border border-slate-200"
        >
          <UserAvatar size="md" />
        </button>
      </div>

      <InboxPopup
        isOpen={isInboxOpen}
        onClose={() => setIsInboxOpen(false)}
        anchorRef={inboxButtonRef}
      />
      <UserProfilePopup
        isOpen={isProfileOpen}
        onClose={() => setIsProfileOpen(false)}
        anchorRef={profileButtonRef}
        placement="below"
      />
    </header>
  );
};
