import { useState, type FormEvent } from 'react';
import { api } from '../api';
import type { Account, Beneficiary } from '../types';
import { CATEGORIES } from '../categories';

interface Props {
  from: Account;
  beneficiaries: Beneficiary[];
  onDone: () => void;
  onBeneficiariesChanged: () => void;
}

export function TransferForm({ from, beneficiaries, onDone, onBeneficiariesChanged }: Props) {
  const [iban, setIban] = useState('');
  const [amount, setAmount] = useState('');
  const [category, setCategory] = useState('AUTRES');
  const [save, setSave] = useState(false);
  const [label, setLabel] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setBusy(true);
    try {
      const minor = Math.round(parseFloat(amount) * 100);
      await api.transfer(from.id, iban.trim(), minor, 'Virement', category);
      if (save && label.trim()) {
        try {
          await api.addBeneficiary(label.trim(), iban.trim());
          onBeneficiariesChanged();
        } catch {
          // le virement a reussi ; un echec d'enregistrement n'est pas bloquant
        }
      }
      setIban('');
      setAmount('');
      setLabel('');
      setCategory('AUTRES');
      setSave(false);
      onDone();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Erreur');
    } finally {
      setBusy(false);
    }
  }

  async function removeBeneficiary(id: string) {
    try {
      await api.deleteBeneficiary(id);
      onBeneficiariesChanged();
    } catch {
      /* ignore */
    }
  }

  return (
    <form className="inline-form" onSubmit={handleSubmit}>
      <h3>Virement</h3>

      {beneficiaries.length > 0 && (
        <div className="beneficiaries">
          {beneficiaries.map((b) => (
            <span key={b.id} className="chip">
              <button type="button" className="chip-fill" onClick={() => setIban(b.accountNumber)}>
                {b.label}
              </button>
              <button
                type="button"
                className="chip-remove"
                title="Supprimer ce bénéficiaire"
                onClick={() => removeBeneficiary(b.id)}
              >
                ×
              </button>
            </span>
          ))}
        </div>
      )}

      <input
        placeholder="IBAN du destinataire (ex. FR76…)"
        value={iban}
        onChange={(e) => setIban(e.target.value)}
        required
      />

      <select
        className="category-select"
        value={category}
        onChange={(e) => setCategory(e.target.value)}
      >
        {CATEGORIES.map((c) => (
          <option key={c.value} value={c.value}>
            {c.label}
          </option>
        ))}
      </select>

      <div className="row" style={{ marginTop: '0.6rem' }}>
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
          Virer
        </button>
      </div>

      <label className="checkbox">
        <input type="checkbox" checked={save} onChange={(e) => setSave(e.target.checked)} />
        Enregistrer ce bénéficiaire
      </label>
      {save && (
        <input
          placeholder="Nom du bénéficiaire (ex. Bob)"
          value={label}
          onChange={(e) => setLabel(e.target.value)}
        />
      )}

      {error && <p className="error">{error}</p>}
    </form>
  );
}
