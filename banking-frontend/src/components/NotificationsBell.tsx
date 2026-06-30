import { useEffect, useRef, useState } from 'react';
import { api } from '../api';
import type { NotificationItem } from '../types';

/**
 * Cloche de notifications : badge avec le nombre de non-lues, panneau déroulant
 * listant les dernières notifications. Le compteur est rafraîchi par sondage
 * (toutes les 25 s) et au retour sur l'onglet. Ouvrir le panneau marque les
 * notifications comme lues.
 */
export function NotificationsBell() {
  const [open, setOpen] = useState(false);
  const [items, setItems] = useState<NotificationItem[]>([]);
  const [unread, setUnread] = useState(0);
  const ref = useRef<HTMLDivElement | null>(null);

  async function refreshCount() {
    try {
      const { count } = await api.unreadNotifications();
      setUnread(count);
    } catch {
      /* hors-ligne / non connecté : on ignore */
    }
  }

  // Sondage périodique + au retour sur l'onglet.
  useEffect(() => {
    refreshCount();
    const id = window.setInterval(refreshCount, 25000);
    const onFocus = () => refreshCount();
    window.addEventListener('focus', onFocus);
    return () => {
      window.clearInterval(id);
      window.removeEventListener('focus', onFocus);
    };
  }, []);

  // Fermer le panneau au clic à l'extérieur.
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
        setUnread(0); // optimiste : tout est lu dès l'ouverture
      } catch {
        /* ignore */
      }
    }
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
        </div>
      )}
    </div>
  );
}
