import { useEffect, useState } from 'react';
import { api } from '../api';
import type { Beneficiary } from '../types';

export default function Contacts({ onClose }: { onClose: () => void }) {
  const [contacts, setContacts] = useState<Beneficiary[]>([]);
  const [name, setName] = useState('');
  const [iban, setIban] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function reload() {
    try {
      setContacts(await api.listBeneficiaries());
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Chargement impossible.');
    }
  }

  useEffect(() => {
    reload();
  }, []);

  async function add() {
    setError(null);
    if (!name.trim()) return setError('Donne un nom à ce contact.');
    if (!iban.trim()) return setError("Indique l'IBAN du contact.");
    setBusy(true);
    try {
      await api.addBeneficiary(name.trim(), iban.trim());
      setName('');
      setIban('');
      await reload();
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Ajout impossible.');
    } finally {
      setBusy(false);
    }
  }

  async function remove(id: string) {
    setError(null);
    setBusy(true);
    try {
      await api.deleteBeneficiary(id);
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
        Un contact est un IBAN que tu renommes. Tu le retrouveras lors d'un
        virement et pour tes demandes de remboursement. Tu peux aussi en
        enregistrer un directement en faisant un virement.
      </p>

      <div className="contacts-add">
        <input
          placeholder="Nom du contact (ex. Bob)"
          value={name}
          onChange={(e) => setName(e.target.value)}
        />
        <input
          placeholder="IBAN (ex. FR76…)"
          value={iban}
          onChange={(e) => setIban(e.target.value)}
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
          <div key={c.id} className="contact-item">
            <span className="contact-avatar">{c.label.charAt(0).toUpperCase()}</span>
            <span className="contact-info">
              <span className="contact-name">{c.label}</span>
              <span className="contact-iban">{c.accountNumber}</span>
            </span>
            <button
              className="ghost contact-remove"
              disabled={busy}
              onClick={() => remove(c.id)}
            >
              Retirer
            </button>
          </div>
        ))}
      </div>
    </div>
  );
}
