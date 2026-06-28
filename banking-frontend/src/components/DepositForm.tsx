import { useState, type FormEvent } from 'react';
import { api } from '../api';
import type { Account } from '../types';

export function DepositForm({ account, onDone }: { account: Account; onDone: () => void }) {
  const [amount, setAmount] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setBusy(true);
    try {
      const minor = Math.round(parseFloat(amount) * 100);
      await api.deposit(account.id, minor, 'Dépôt');
      setAmount('');
      onDone();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Erreur');
    } finally {
      setBusy(false);
    }
  }

  return (
    <form className="inline-form" onSubmit={handleSubmit}>
      <h3>Déposer de l'argent</h3>
      <div className="row">
        <input
          type="number"
          step="0.01"
          min="0.01"
          placeholder="Montant en €"
          value={amount}
          onChange={(e) => setAmount(e.target.value)}
          required
        />
        <button type="submit" disabled={busy}>
          Déposer
        </button>
      </div>
      {error && <p className="error">{error}</p>}
    </form>
  );
}
