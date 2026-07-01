import { useState, type FormEvent } from 'react';
import { api } from '../api';
import type { Account, Beneficiary } from '../types';
import { CATEGORIES } from '../categories';

interface Props {
  from: Account;
  beneficiaries: Beneficiary[];
  onDone: () => void;
  onBeneficiariesChanged: () => void;
  // Pré-remplissage (ex. depuis un QR code / lien de paiement).
  initialIban?: string;
  initialAmount?: string; // en euros, ex. "12.50"
  initialDescription?: string;
}

export function TransferForm({
  from,
  beneficiaries,
  onDone,
  onBeneficiariesChanged,
  initialIban = '',
  initialAmount = '',
  initialDescription = '',
}: Props) {
  const hasContacts = beneficiaries.length > 0;
  // Par défaut : virement à un contact. On bascule en « nouveau bénéficiaire »
  // s'il n'y a aucun contact, ou si un IBAN est pré-rempli (QR / lien).
  const [mode, setMode] = useState<'contact' | 'new'>(
    initialIban || !hasContacts ? 'new' : 'contact',
  );
  const [contactIban, setContactIban] = useState('');
  const [iban, setIban] = useState(initialIban);
  const [amount, setAmount] = useState(initialAmount);
  const [description, setDescription] = useState(initialDescription);
  const [category, setCategory] = useState('AUTRES');
  const [save, setSave] = useState(false);
  const [label, setLabel] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);

    const target = (mode === 'contact' ? contactIban : iban).trim();
    if (!target) {
      setError(mode === 'contact' ? 'Choisis un contact.' : 'Saisis un IBAN.');
      return;
    }

    setBusy(true);
    try {
      const minor = Math.round(parseFloat(amount) * 100);
      const motif = description.trim() || 'Virement';
      await api.transfer(from.id, target, minor, motif, category);
      if (mode === 'new' && save && label.trim()) {
        try {
          await api.addBeneficiary(label.trim(), target);
          onBeneficiariesChanged();
        } catch {
          // le virement a réussi ; un échec d'enregistrement n'est pas bloquant
        }
      }
      setContactIban('');
      setIban('');
      setAmount('');
      setDescription('');
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

  return (
    <form className="inline-form" onSubmit={handleSubmit}>
      <h3>Virement</h3>

      {hasContacts && (
        <div className="transfer-mode">
          <button
            type="button"
            className={mode === 'contact' ? 'active' : ''}
            onClick={() => setMode('contact')}
          >
            Un contact
          </button>
          <button
            type="button"
            className={mode === 'new' ? 'active' : ''}
            onClick={() => setMode('new')}
          >
            Nouveau bénéficiaire
          </button>
        </div>
      )}

      {mode === 'contact' ? (
        <label className="field-label">
          Contact
          <select value={contactIban} onChange={(e) => setContactIban(e.target.value)}>
            <option value="">— Choisir un contact —</option>
            {beneficiaries.map((b) => (
              <option key={b.id} value={b.accountNumber}>
                {b.label}
              </option>
            ))}
          </select>
        </label>
      ) : (
        <>
          <input
            placeholder="IBAN du destinataire (ex. FR76…)"
            value={iban}
            onChange={(e) => setIban(e.target.value)}
          />
          <label className="checkbox">
            <input type="checkbox" checked={save} onChange={(e) => setSave(e.target.checked)} />
            Enregistrer ce contact
          </label>
          {save && (
            <input
              placeholder="Nom du contact (ex. Bob)"
              value={label}
              onChange={(e) => setLabel(e.target.value)}
            />
          )}
        </>
      )}

      <input
        placeholder="Communication (optionnel)"
        value={description}
        onChange={(e) => setDescription(e.target.value)}
        style={{ marginTop: '0.6rem' }}
      />

      <select
        className="category-select"
        value={category}
        onChange={(e) => setCategory(e.target.value)}
        style={{ marginTop: '0.6rem' }}
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

      {error && <p className="error">{error}</p>}
    </form>
  );
}
