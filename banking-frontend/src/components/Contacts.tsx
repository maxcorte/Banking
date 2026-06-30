import { useEffect, useState } from 'react';
import { api } from '../api';
import type { Contact } from '../types';

export default function Contacts({ onClose }: { onClose: () => void }) {
  const [contacts, setContacts] = useState<Contact[]>([]);
  const [username, setUsername] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function reload() {
    try {
      setContacts(await api.listContacts());
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Chargement impossible.');
    }
  }

  useEffect(() => {
    reload();
  }, []);

  async function add() {
    setError(null);
    if (!username.trim()) return setError("Indique un nom d'utilisateur.");
    setBusy(true);
    try {
      await api.addContact(username.trim());
      setUsername('');
      await reload();
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Ajout impossible.');
    } finally {
      setBusy(false);
    }
  }

  async function remove(userId: string) {
    setError(null);
    setBusy(true);
    try {
      await api.removeContact(userId);
      await reload();
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Suppression impossible.');
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="card contacts-card">
      <div className="req-head">
        <h2>Mes contacts</h2>
        <button className="ghost" onClick={onClose}>
          Fermer
        </button>
      </div>

      <p className="muted">
        Ajoute les personnes à qui tu demandes souvent de l'argent : tu pourras
        les choisir dans une liste au lieu de taper leur nom.
      </p>

      <div className="contacts-add">
        <input
          placeholder="Nom d'utilisateur"
          value={username}
          onChange={(e) => setUsername(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && add()}
        />
        <button className="primary" disabled={busy} onClick={add}>
          Ajouter
        </button>
      </div>

      {error && <p className="error">{error}</p>}

      <div className="contacts-list">
        {contacts.length === 0 && <p className="muted">Aucun contact pour l'instant.</p>}
        {contacts.map((c) => (
          <div key={c.userId} className="contact-item">
            <span className="contact-avatar">{c.username.charAt(0).toUpperCase()}</span>
            <span className="contact-name">{c.username}</span>
            <button
              className="ghost contact-remove"
              disabled={busy}
              onClick={() => remove(c.userId)}
            >
              Retirer
            </button>
          </div>
        ))}
      </div>
    </div>
  );
}
