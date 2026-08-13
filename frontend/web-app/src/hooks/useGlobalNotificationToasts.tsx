// hooks/useGlobalNotificationToasts.tsx
//
// Makes EVERY page in the app pop up a banner the instant a new notification
// arrives — not just while you're on the Notifications page. Call this ONCE,
// somewhere that's always mounted while you're logged in (Layout.tsx).
//
import { useEffect } from 'react';
import toast from 'react-hot-toast';
import { Bell } from 'lucide-react';
import { useNotificationSocket } from './useNotificationSocket';
import { NotificationLog } from '../types/notification';
import { typeConfig } from '../utils/notificationConfig';
import { useAuth } from '../context/AuthContext';

export function useGlobalNotificationToasts() {
  // The server now decides who's allowed to see what (based on this token's real,
  // validated identity) — the frontend no longer needs to work out its own roles.
  const { token } = useAuth();

  // Ask the browser, once, "can this site show OS-level notifications?" — the
  // same permission popup Gmail/Slack ask for. If you already answered before
  // (allow or block), this line does nothing.
  useEffect(() => {
    if ('Notification' in window && Notification.permission === 'default') {
      Notification.requestPermission();
    }
  }, []);

  useNotificationSocket({
    token,
    onMessage: (n: NotificationLog) => {
      const cfg = typeConfig[n.type];

      // 1. In-app popup — always, on every page, the moment it arrives.
      toast.custom(t => (
        <div className={`${t.visible ? 'animate-enter' : 'animate-leave'}
          max-w-sm bg-white shadow-lg rounded-2xl border border-slate-100 p-4 flex items-start gap-3`}>
          <div className={`w-8 h-8 rounded-xl flex items-center justify-center flex-shrink-0
            ${cfg?.iconBg ?? 'bg-slate-100 text-slate-500'}`}>
            {cfg?.icon ?? <Bell size={14} />}
          </div>
          <div className="flex-1 min-w-0">
            <p className="text-xs font-black text-slate-900">{cfg?.label ?? 'Notification'}</p>
            <p className="text-[11px] text-slate-500 font-medium mt-0.5 line-clamp-2">{n.message}</p>
          </div>
        </div>
      ), { duration: 5000 });

      // 2. OS-level banner — only when you're NOT already looking at this tab
      // (otherwise it'd just be a duplicate of the toast above).
      if ('Notification' in window && Notification.permission === 'granted' && document.hidden) {
        new Notification(cfg?.label ?? 'Aerospace ERP', {
          body: n.message,
          tag: `notification-${n.id}`, // replaces any earlier banner instead of stacking up
        });
      }
    },
  });
}
