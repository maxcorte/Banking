import { useEffect, useMemo, useState } from 'react';
import { api, formatEuros } from '../api';
import type { Account, TransactionLine } from '../types';
import { categoryColor, categoryName } from '../categories';
import { Donut, type DonutSlice } from './Donut';

type Period = 'month' | 'all';

export function SpendingStats({ onClose }: { onClose: () => void }) {
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [lines, setLines] = useState<TransactionLine[]>([]);
  const [period, setPeriod] = useState<Period>('month');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    (async () => {
      try {
        const accs = await api.listAccounts();
        const histories = await Promise.all(
          accs.map((a) => api.history(a.id).catch(() => [] as TransactionLine[])),
        );
        if (!active) return;
        setAccounts(accs);
        setLines(histories.flat());
      } catch (e) {
        if (active) setError(e instanceof Error ? e.message : 'Erreur');
      } finally {
        if (active) setLoading(false);
      }
    })();
    return () => {
      active = false;
    };
  }, []);

  const stats = useMemo(() => {
    const myNumbers = new Set(accounts.map((a) => a.accountNumber));
    const now = new Date();
    const inPeriod = (l: TransactionLine) => {
      if (period === 'all') return true;
      const d = new Date(l.at);
      return d.getMonth() === now.getMonth() && d.getFullYear() === now.getFullYear();
    };

    let totalIn = 0;
    let totalOut = 0;
    const byCat: Record<string, number> = {};

    for (const l of lines) {
      if (!inPeriod(l)) continue;
      // On ignore les virements entre mes propres comptes (pas une vraie dépense).
      if (l.counterpartyNumber && myNumbers.has(l.counterpartyNumber)) continue;

      if (l.amountMinor >= 0) {
        totalIn += l.amountMinor;
      } else {
        const amount = -l.amountMinor;
        totalOut += amount;
        const cat = l.category || 'AUTRES';
        byCat[cat] = (byCat[cat] || 0) + amount;
      }
    }

    const slices: (DonutSlice & { value: string })[] = Object.entries(byCat)
      .map(([value, amount]) => ({
        value,
        amount,
        label: categoryName(value),
        color: categoryColor(value),
      }))
      .sort((a, b) => b.amount - a.amount);

    return { totalIn, totalOut, net: totalIn - totalOut, slices };
  }, [lines, accounts, period]);

  return (
    <section className="stats">
      <div className="section-head">
        <h2>Statistiques</h2>
        <div className="row">
          <div className="seg">
            <button
              className={period === 'month' ? 'seg-on' : ''}
              onClick={() => setPeriod('month')}
            >
              Ce mois-ci
            </button>
            <button className={period === 'all' ? 'seg-on' : ''} onClick={() => setPeriod('all')}>
              Tout
            </button>
          </div>
          <button className="ghost" onClick={onClose}>
            Retour
          </button>
        </div>
      </div>

      {loading && <p className="muted">Chargement…</p>}
      {error && <p className="error">{error}</p>}

      {!loading && !error && (
        <>
          <div className="stat-cards">
            <div className="stat-card">
              <span className="muted">Entrées</span>
              <span className="credit stat-value">+{formatEuros(stats.totalIn)}</span>
            </div>
            <div className="stat-card">
              <span className="muted">Sorties</span>
              <span className="debit stat-value">−{formatEuros(stats.totalOut)}</span>
            </div>
            <div className="stat-card">
              <span className="muted">Solde net</span>
              <span className={`stat-value ${stats.net >= 0 ? 'credit' : 'debit'}`}>
                {stats.net >= 0 ? '+' : '−'}
                {formatEuros(Math.abs(stats.net))}
              </span>
            </div>
          </div>

          <h3>Dépenses par catégorie</h3>
          {stats.slices.length === 0 ? (
            <p className="muted">Aucune dépense sur la période.</p>
          ) : (
            <div className="stats-breakdown">
              <Donut slices={stats.slices} />
              <ul className="cat-legend">
                {stats.slices.map((s) => {
                  const pct = stats.totalOut > 0 ? (s.amount / stats.totalOut) * 100 : 0;
                  return (
                    <li key={s.value}>
                      <span className="cat-dot" style={{ background: s.color }} />
                      <span className="cat-name">{s.label}</span>
                      <span className="cat-bar">
                        <span
                          className="cat-bar-fill"
                          style={{ width: `${pct}%`, background: s.color }}
                        />
                      </span>
                      <span className="cat-amount">{formatEuros(s.amount)}</span>
                      <span className="cat-pct muted">{pct.toFixed(0)}%</span>
                    </li>
                  );
                })}
              </ul>
            </div>
          )}
        </>
      )}
    </section>
  );
}
