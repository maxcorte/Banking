import { useEffect, useState } from 'react';
import { api, formatEuros } from '../api';
import type { Account, Contact, PaymentRequest } from '../types';

const STATUS_LABEL: Record<PaymentRequest['status'], string> = {
  PENDING: 'En attente',
  ACCEPTED: 'Accepté',
  REFUSED: 'Refusé',
  CANCELLED: 'Annulé',
};

function StatusChip({ status }: { status: PaymentRequest['status'] }) {
  return <span className={`req-chip req-${status.toLowerCase()}`}>{STATUS_LABEL[status]}</span>;
}

export default function PaymentRequests({
  accounts,
  onClose,
  onDone,
}: {
  accounts: Account[];
  onClose: () => void;
  onDone: () => void;
}) {
  const [tab, setTab] = useState<'incoming' | 'outgoing'>('incoming');
  const [incoming, setIncoming] = useState<PaymentRequest[]>([]);
  const [outgoing, setOutgoing] = useState<PaymentRequest[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  // Formulaire « nouvelle demande »
  const [toAccountId, setToAccountId] = useState(accounts[0]?.id ?? '');
  const [payer, setPayer] = useState('');
  const [amount, setAmount] = useState('');
  const [desc, setDesc] = useState('');
  const [contacts, setContacts] = useState<Contact[]>([]);

  // Compte choisi pour payer chaque demande reçue (par id de demande)
  const [fromByReq, setFromByReq] = useState<Record<string, string>>({});

  async function reload() {
    try {
      const [inc, out] = await Promise.all([
        api.listIncomingRequests(),
        api.listOutgoingRequests(),
      ]);
      setIncoming(inc);
      setOutgoing(out);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Chargement impossible.');
    }
  }

  useEffect(() => {
    reload();
    api.listContacts().then(setContacts).catch(() => setContacts([]));
  }, []);

  async function submitNew() {
    setError(null);
    const cents = Math.round(parseFloat(amount.replace(',', '.')) * 100);
    if (!toAccountId) return setError('Choisis le compte à créditer.');
    if (!payer.trim()) return setError('Indique le nom du destinataire.');
    if (!Number.isFinite(cents) || cents <= 0) return setError('Montant invalide.');
    setBusy(true);
    try {
      await api.createPaymentRequest(toAccountId, payer.trim(), cents, desc.trim());
      setPayer('');
      setAmount('');
      setDesc('');
      setTab('outgoing');
      await reload();
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Envoi impossible.');
    } finally {
      setBusy(false);
    }
  }

  async function accept(req: PaymentRequest) {
    setError(null);
    const from = fromByReq[req.id] ?? accounts[0]?.id ?? '';
    if (!from) return setError('Aucun compte disponible pour payer.');
    setBusy(true);
    try {
      await api.acceptPaymentRequest(req.id, from);
      await reload();
      onDone(); // rafraîchit les soldes du dashboard
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Paiement impossible.');
    } finally {
      setBusy(false);
    }
  }

  async function act(fn: () => Promise<void>) {
    setError(null);
    setBusy(true);
    try {
      await fn();
      await reload();
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Action impossible.');
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="card req-card">
      <div className="req-head">
        <h2>Demandes de remboursement</h2>
        <button className="ghost" onClick={onClose}>
          Fermer
        </button>
      </div>

      <div className="req-tabs">
        <button
          className={tab === 'incoming' ? 'active' : ''}
          onClick={() => setTab('incoming')}
        >
          Reçues
        </button>
        <button
          className={tab === 'outgoing' ? 'active' : ''}
          onClick={() => setTab('outgoing')}
        >
          Envoyées
        </button>
      </div>

      {error && <p className="error">{error}</p>}

      {tab === 'incoming' && (
        <div className="req-list">
          {incoming.length === 0 && <p className="muted">Aucune demande reçue.</p>}
          {incoming.map((r) => (
            <div key={r.id} className="req-item">
              <div className="req-item-main">
                <span className="req-who">
                  <strong>{r.requesterName}</strong> vous demande {formatEuros(r.amountMinor)}
                </span>
                {r.description && <span className="req-desc">« {r.description} »</span>}
                <StatusChip status={r.status} />
              </div>
              {r.status === 'PENDING' && (
                <div className="req-actions">
                  <select
                    value={fromByReq[r.id] ?? accounts[0]?.id ?? ''}
                    onChange={(e) =>
                      setFromByReq((m) => ({ ...m, [r.id]: e.target.value }))
                    }
                  >
                    {accounts.map((a) => (
                      <option key={a.id} value={a.id}>
                        {a.accountNumber} — {formatEuros(a.balanceMinor)}
                      </option>
                    ))}
                  </select>
                  <button className="primary" disabled={busy} onClick={() => accept(r)}>
                    Accepter et payer
                  </button>
                  <button
                    className="ghost"
                    disabled={busy}
                    onClick={() => act(() => api.refusePaymentRequest(r.id))}
                  >
                    Refuser
                  </button>
                </div>
              )}
            </div>
          ))}
        </div>
      )}

      {tab === 'outgoing' && (
        <div className="req-list">
          <div className="req-new">
            <h3>Nouvelle demande</h3>
            <label>
              Compte à créditer
              <select value={toAccountId} onChange={(e) => setToAccountId(e.target.value)}>
                {accounts.map((a) => (
                  <option key={a.id} value={a.id}>
                    {a.accountNumber} — {formatEuros(a.balanceMinor)}
                  </option>
                ))}
              </select>
            </label>
            {contacts.length > 0 ? (
              <label>
                Destinataire
                <select value={payer} onChange={(e) => setPayer(e.target.value)}>
                  <option value="">— Choisir un contact —</option>
                  {contacts.map((c) => (
                    <option key={c.userId} value={c.username}>
                      {c.username}
                    </option>
                  ))}
                </select>
              </label>
            ) : (
              <>
                <input
                  placeholder="Destinataire (nom d'utilisateur)"
                  value={payer}
                  onChange={(e) => setPayer(e.target.value)}
                />
                <span className="muted">
                  Astuce : ajoute des contacts pour les choisir dans une liste.
                </span>
              </>
            )}
            <input
              placeholder="Montant en €"
              inputMode="decimal"
              value={amount}
              onChange={(e) => setAmount(e.target.value)}
            />
            <input
              placeholder="Motif (optionnel)"
              value={desc}
              onChange={(e) => setDesc(e.target.value)}
            />
            <button className="primary" disabled={busy} onClick={submitNew}>
              Envoyer la demande
            </button>
          </div>

          {outgoing.length === 0 && <p className="muted">Aucune demande envoyée.</p>}
          {outgoing.map((r) => (
            <div key={r.id} className="req-item">
              <div className="req-item-main">
                <span className="req-who">
                  Vous avez demandé {formatEuros(r.amountMinor)} à <strong>{r.payerName}</strong>
                </span>
                {r.description && <span className="req-desc">« {r.description} »</span>}
                <StatusChip status={r.status} />
              </div>
              {r.status === 'PENDING' && (
                <div className="req-actions">
                  <button
                    className="ghost"
                    disabled={busy}
                    onClick={() => act(() => api.cancelPaymentRequest(r.id))}
                  >
                    Annuler
                  </button>
                </div>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
