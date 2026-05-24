import { useEffect, useRef, useState, type RefObject } from "react";
import { Inbox } from "lucide-react";
import { InboxList } from "./InboxList";

interface InboxPopupProps {
  isOpen: boolean;
  onClose: () => void;
  anchorRef: RefObject<HTMLElement | null>;
  placement?: 'below' | 'right';
}

export const InboxPopup = ({ isOpen, onClose, anchorRef, placement = 'below' }: InboxPopupProps) => {
  const popupRef = useRef<HTMLDivElement>(null);
  const [pos, setPos] = useState<{ top: number; left: number } | null>(null);

  useEffect(() => {
    if (isOpen && anchorRef.current) {
      const rect = anchorRef.current.getBoundingClientRect();
      const popupWidth = 320;
      const popupHeight = 450;
      let top: number;
      let left: number;

      if (placement === 'right') {
        left = rect.right + 8;
        top = rect.top;
        if (left + popupWidth > window.innerWidth) {
          left = rect.left - popupWidth - 8;
        }
        if (top + popupHeight > window.innerHeight) {
          top = window.innerHeight - popupHeight - 8;
        }
        if (top < 0) top = 8;
      } else {
        top = rect.bottom + 8;
        left = rect.left;
        if (top + popupHeight > window.innerHeight) {
          top = rect.top - popupHeight - 8;
        }
        if (left + popupWidth > window.innerWidth) {
          left = window.innerWidth - popupWidth - 8;
        }
        if (left < 0) left = 8;
      }

      setPos({ top, left });
    }
  }, [isOpen, anchorRef, placement]);

  useEffect(() => {
    if (!isOpen) return;
    const handleMouseDown = (e: MouseEvent) => {
      if (
        popupRef.current &&
        !popupRef.current.contains(e.target as Node) &&
        anchorRef.current &&
        !anchorRef.current.contains(e.target as Node)
      ) {
        onClose();
      }
    };
    document.addEventListener("mousedown", handleMouseDown);
    return () => document.removeEventListener("mousedown", handleMouseDown);
  }, [isOpen, onClose, anchorRef]);

  if (!isOpen || !pos) return null;

  return (
    <div
      ref={popupRef}
      className="fixed z-[100] w-80 bg-white border border-slate-200 shadow-xl rounded-lg overflow-hidden flex flex-col"
      style={{ top: pos.top, left: pos.left }}
    >
      <div className="flex items-center px-4 py-3 border-b border-slate-100 bg-slate-50/50">
        <Inbox className="w-4 h-4 text-slate-700" />
        <span className="font-semibold text-sm text-slate-900 ml-2">Inbox</span>
      </div>
      <div className="max-h-[400px] overflow-y-auto">
        <InboxList onClose={onClose} />
      </div>
    </div>
  );
};
