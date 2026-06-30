import { useEffect, useRef, useState } from 'react';
import { api } from '../api';
import type { NotificationItem } from '../types';
import { disablePush, enablePush, getPushState, type PushState } from '../push';

/**
 * Cloche de notifications : badge des non-lues + panneau des dernières. Permet
 * aussi d'activer les notifications push (pop-up système, même app fermée).
 */
export function NotificationsBell() {
  const [open, setOpen] = useState(false);
  const [items, setItems] = useState<NotificationItem[]>([]);
  const [unread, setUnread] = useState(0);
  const [pushState, setPushState] = useState<PushState>('disabled');
  const [pushBusy, setPushBusy] = useState(false);
  const [pushError, setPushError] = useState<string | null>(null);
  const ref = useRef<HTMLDivElement | null>(null);

  async function refreshCount() {
    try {
      const { count } = await api.unreadNotifications();
      setUnread(count);
    } catch {
      /* hors-ligne / non connecté : on ignore */
    }
  }

  useEffect(() => {
    refreshCount();
    getPushState().then(setPushState);
    const id = window.setInterval(refreshCount, 25000);
    const onFocus = () => refreshCount();
    window.addEventListener('focus', onFocus);
    return () => {
      window.clearInterval(id);
      window.removeEventListener('focus', onFocus);
    };
  }, []);

  useEffect(() => {
    function onDoc(e: MouseEvent) {
      if (open && ref.current && !ref.current.contains(e.target as Node)) {
        setOpen(false);
      }
    }
    document.addEventListener('mousedown', onDoc);
    return () => document.removeEventListener('mousedown', onDoc);
  }, [open]);

  async function toggle() {
    const next = !open;
    setOpen(next);
    if (next) {
      try {
        setItems(await api.listNotifications());
        await api.markNotificationsRead();
        setUnread(0);
      } catch {
        /* ignore */
      }
    }
  }

  async function onTogglePush() {
    setPushBusy(true);
    setPushError(null);
    try {
      const next = pushState === 'enabled' ? await disablePush() : await enablePush();
      setPushState(next);
      if (next === 'denied') {
        setPushError(
          "Autorisation refusée. Active les notifications pour ce site dans les réglages du navigateur, puis réessaie.",
        );
      }
    } catch (e) {
      setPushError(e instanceof Error ? e.message : "Activation impossible.");
    } finally {
      setPushBusy(false);
    }
  }

  function pushFooter() {
    if (pushState === 'unsupported') {
      return (
        <p className="notif-push-hint">
          Installe l'app sur l'écran d'accueil pour recevoir les alertes push.
        </p>
      );
    }
    return (
      <button
        type="button"
        className={`notif-push-btn ${pushState === 'enabled' ? 'on' : ''}`}
        onClick={onTogglePush}
        disabled={pushBusy}
      >
        {pushState === 'enabled' ? 'Alertes push activées ✓' : '🔔 Activer les alertes push'}
      </button>
    );
  }

  return (
    <div className="notif" ref={ref}>
      <button
        type="button"
        className="notif-bell"
        onClick={toggle}
        title="Notifications"
        aria-label="Notifications"
      >
        <span aria-hidden="true">🔔</span>
        {unread > 0 && <span className="notif-badge">{unread > 9 ? '9+' : unread}</span>}
      </button>

      {open && (
        <div className="notif-panel">
          <div className="notif-head">Notifications</div>
          {items.length === 0 ? (
            <p className="notif-empty">Aucune notification.</p>
          ) : (
            <ul className="notif-list">
              {items.map((n) => (
                <li key={n.id} className={`notif-item ${n.read ? '' : 'unread'}`}>
                  <span className="notif-title">{n.title}</span>
                  {n.body && <span className="notif-body">{n.body}</span>}
                  <span className="notif-time">{new Date(n.at).toLocaleString('fr-FR')}</span>
                </li>
              ))}
            </ul>
          )}
          <div className="notif-foot">
            {pushFooter()}
            {pushError && <p className="notif-push-error">{pushError}</p>}
          </div>
        </div>
      )}
    </div>
  );
}
