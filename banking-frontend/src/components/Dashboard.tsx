import { useCallback, useEffect, useState } from 'react';
import { api, formatEuros } from '../api';
import type { Account, Beneficiary, TransactionLine } from '../types';
import { useAuth } from '../auth';
import { CreateAccountForm } from './CreateAccountForm';
import { DepositForm } from './DepositForm';
import { TransferForm } from './TransferForm';
import { CopyIban } from './CopyIban';
import { AuditLog } from './AuditLog';
import { BalanceChart } from './BalanceChart';
import { ThemeToggle } from './ThemeToggle';
import { SpendingStats } from './SpendingStats';
import { downloadStatementCsv, printStatement } from '../statement';
import { categoryLabel, categoryColor } from '../categories';
import { ReceiveQR } from './ReceiveQR';
import { QrScanner, type ScannedPay } from './QrScanner';

export function Dashboard() {
  const { logout, isAdmin } = useAuth();
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [selected, setSelected] = useState<Account | null>(null);
  const [history, setHistory] = useState<TransactionLine[]>([]);
  const [beneficiaries, setBeneficiaries] = useState<Beneficiary[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [showAudit, setShowAudit] = useState(false);
  const [showStats, setShowStats] = useState(false);
  const [showReceive, setShowReceive] = useState(false);
  const [showScanner, setShowScanner] = useState(false);
  // Lien de paiement reçu par QR : /?pay=<compte>&amt=<centimes>&desc=<motif>
  const [payRequest, setPayRequest] = useState<{ iban: string; amount: string; desc: string } | null>(
    () => {
      const p = new URLSearchParams(window.location.search);
      const pay = p.get('pay');
      if (!pay) return null;
      const amt = p.get('amt');
      return {
        iban: pay,
        amount: amt && Number(amt) > 0 ? (Number(amt) / 100).toFixed(2) : '',
        desc: p.get('desc') ?? '',
      };
    },
  );

  const loadBeneficiaries = useCallback(async () => {
    try {
      setBeneficiaries(await api.listBeneficiaries());
    } catch {
      setBeneficiaries([]);
    }
  }, []);

  const refresh = useCallback(async () => {
    setError(null);
    try {
      const list = await api.listAccounts();
      setAccounts(list);
      // Met a jour le compte selectionne s'il existe encore.
      setSelected((prev) => (prev ? list.find((a) => a.id === prev.id) ?? null : null));
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Erreur de chargement');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    refresh();
    loadBeneficiaries();
  }, [refresh, loadBeneficiaries]);

  function clearPayRequest() {
    setPayRequest(null);
    window.history.replaceState(null, '', '/');
  }

  function handleScan(pay: ScannedPay) {
    setShowScanner(false);
    setShowReceive(false);
    setShowStats(false);
    setShowAudit(false);
    setPayRequest({ iban: pay.iban, amount: pay.amount, desc: pay.desc });
  }

  // Demande de paiement reçue mais aucun compte choisi : on en sélectionne un
  // automatiquement pour pré-remplir le virement.
  useEffect(() => {
    if (payRequest && !selected && accounts.length > 0) {
      void selectAccount(accounts[0]);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [payRequest, selected, accounts]);

  async function selectAccount(account: Account) {
    setSelected(account);
    try {
      setHistory(await api.history(account.id));
    } catch {
      setHistory([]);
    }
  }

  async function afterMutation() {
    await refresh();
    if (selected) {
      try {
        setHistory(await api.history(selected.id));
      } catch {
        /* ignore */
      }
    }
  }

  async function closeAccount(account: Account) {
    if (!window.confirm(`Clôturer le compte de ${account.ownerName} ? Cette action est définitive.`)) {
      return;
    }
    setError(null);
    try {
      await api.deleteAccount(account.id);
      setSelected(null);
      setHistory([]);
      await refresh();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Clôture impossible');
    }
  }

  return (
    <div className="app">
      <header className="topbar">
        <span className="brand">Ma Banque</span>
        <div className="row">
          <ThemeToggle />
          <button
            className="link"
            onClick={() => {
              setShowReceive((v) => !v);
              setShowStats(false);
              setShowAudit(false);
            }}
          >
            {showReceive ? 'Mes comptes' : 'Recevoir'}
          </button>
          <button className="link" onClick={() => setShowScanner(true)}>
            Scanner
          </button>
          <button
            className="link"
            onClick={() => {
              setShowStats((v) => !v);
              setShowAudit(false);
              setShowReceive(false);
            }}
          >
            {showStats ? 'Mes comptes' : 'Statistiques'}
          </button>
          {isAdmin && (
            <button
              className="link"
              onClick={() => {
                setShowAudit((v) => !v);
                setShowStats(false);
                setShowReceive(false);
              }}
            >
              {showAudit ? 'Mes comptes' : "Journal d'audit"}
            </button>
          )}
          <button className="link" onClick={logout}>
            Déconnexion
          </button>
        </div>
      </header>

      {payRequest && (
        <div className="pay-banner">
          <span>
            Demande de paiement
            {payRequest.amount ? ` de ${payRequest.amount} €` : ''} vers{' '}
            <strong>{payRequest.iban}</strong>.
            {selected
              ? ' Vérifie et confirme le virement ci-dessous.'
              : ' Choisis le compte à débiter.'}
          </span>
          <button className="ghost" onClick={clearPayRequest}>
            Annuler
          </button>
        </div>
      )}

      {showReceive ? (
        <main className="layout-single">
          <ReceiveQR accounts={accounts} onClose={() => setShowReceive(false)} />
        </main>
      ) : showStats ? (
        <main className="layout-single">
          <SpendingStats onClose={() => setShowStats(false)} />
        </main>
      ) : showAudit && isAdmin ? (
        <main className="layout-single">
          <AuditLog onClose={() => setShowAudit(false)} />
        </main>
      ) : (
      <main className="layout">
        <section>
          <div className="section-head">
            <h2>Mes comptes</h2>
            <button className="ghost" onClick={refresh}>
              Rafraîchir
            </button>
          </div>

          {loading && <p className="muted">Chargement…</p>}
          {error && <p className="error">{error}</p>}
          {!loading && accounts.length === 0 && (
            <p className="muted">Aucun compte pour l'instant. Créez-en un ci-dessous.</p>
          )}

          <div className="accounts">
            {accounts.map((account) => (
              <div
                key={account.id}
                className={`account-card ${selected?.id === account.id ? 'active' : ''}`}
                role="button"
                tabIndex={0}
                onClick={() => selectAccount(account)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter' || e.key === ' ') selectAccount(account);
                }}
              >
                <span className="owner">{account.ownerName}</span>
                <span className="iban-row">
                  <span className="iban">{account.accountNumber}</span>
                  <CopyIban value={account.accountNumber} />
                </span>
                <span className="balance">{formatEuros(account.balanceMinor)}</span>
              </div>
            ))}
          </div>

          <CreateAccountForm onCreated={afterMutation} />
        </section>

        <section>
          {selected ? (
            <>
              <div className="section-head">
                <h2>{selected.ownerName}</h2>
                <span className="big-balance">{formatEuros(selected.balanceMinor)}</span>
              </div>

              {isAdmin ? (
                <DepositForm account={selected} onDone={afterMutation} />
              ) : (
                <p className="muted">Les dépôts sont réservés à l'administrateur.</p>
              )}
              <TransferForm
                key={payRequest ? 'pay' : 'normal'}
                from={selected}
                beneficiaries={beneficiaries}
                onDone={() => {
                  void afterMutation();
                  if (payRequest) clearPayRequest();
                }}
                onBeneficiariesChanged={loadBeneficiaries}
                initialIban={payRequest?.iban}
                initialAmount={payRequest?.amount}
                initialDescription={payRequest?.desc}
              />

              <div className="section-head">
                <h3>Historique</h3>
                {history.length > 0 && (
                  <div className="row">
                    <button className="ghost" onClick={() => downloadStatementCsv(selected, history)}>
                      Exporter CSV
                    </button>
                    <button className="ghost" onClick={() => printStatement(selected, history)}>
                      Relevé PDF
                    </button>
                  </div>
                )}
              </div>
              {history.length === 0 ? (
                <p className="muted">Aucun mouvement.</p>
              ) : (
                <>
                  <BalanceChart lines={history} />
                  <ul className="history">
                    {history.map((line) => {
                      const incoming = line.amountMinor >= 0;
                      const title =
                        line.kind === 'DEPOSIT'
                          ? 'Dépôt'
                          : incoming
                            ? `Reçu de ${line.counterpartyName ?? '—'}`
                            : `Envoyé à ${line.counterpartyName ?? '—'}`;
                      return (
                        <li key={line.id} className="history-row">
                          <span className={`history-dir ${incoming ? 'credit' : 'debit'}`}>
                            {incoming ? '↓' : '↑'}
                          </span>
                          <span className="history-text">
                            <span className="history-party">
                              {title}
                              {categoryLabel(line.category) && (
                                <span
                                  className="cat-chip"
                                  style={{
                                    color: categoryColor(line.category),
                                    borderColor: categoryColor(line.category),
                                  }}
                                >
                                  {categoryLabel(line.category)}
                                </span>
                              )}
                            </span>
                            {line.description && line.description !== 'Virement' && (
                              <span className="history-note">« {line.description} »</span>
                            )}
                            <span className="history-meta">
                              {new Date(line.at).toLocaleString('fr-FR')}
                              {line.counterpartyNumber ? ` · ${line.counterpartyNumber}` : ''}
                            </span>
                          </span>
                          <span className="history-amounts">
                            <span className={incoming ? 'credit' : 'debit'}>
                              {incoming ? '+' : '−'}
                              {formatEuros(Math.abs(line.amountMinor))}
                            </span>
                            <span className="history-balance">
                              {formatEuros(line.balanceAfterMinor)}
                            </span>
                          </span>
                        </li>
                      );
                    })}
                  </ul>
                </>
              )}

              <div className="danger-zone">
                <button className="danger" onClick={() => closeAccount(selected)}>
                  Clôturer ce compte
                </button>
                <span className="muted">Le solde doit être à zéro.</span>
              </div>
            </>
          ) : (
            <p className="muted">Sélectionnez un compte pour voir le détail et agir dessus.</p>
          )}
        </section>
      </main>
      )}

      {showScanner && (
        <QrScanner onDetected={handleScan} onClose={() => setShowScanner(false)} />
      )}
    </div>
  );
}
