import { useState, type FormEvent } from 'react';
import { api } from '../api';

export function CreateAccountForm({ onCreated }: { onCreated: () => void }) {
  const [ownerName, setOwnerName] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setBusy(true);
    try {
      await api.createAccount(ownerName, 'EUR');
      setOwnerName('');
      onCreated();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Erreur');
    } finally {
      setBusy(false);
    }
  }

  return (
    <form className="inline-form" onSubmit={handleSubmit}>
      <h3>Nouveau compte</h3>
      <div className="row">
        <input
          placeholder="Nom du titulaire"
          value={ownerName}
          onChange={(e) => setOwnerName(e.target.value)}
          required
        />
        <button type="submit" disabled={busy}>
          Créer
        </button>
      </div>
      {error && <p className="error">{error}</p>}
    </form>
  );
}
