import { useEffect, useState } from 'react';
import { api } from '../api';
import type { AuditEntry } from '../types';

const LABELS: Record<string, string> = {
  LOGIN_SUCCESS: 'Connexion',
  LOGIN_FAILURE: 'Échec connexion',
  LOGOUT: 'Déconnexion',
  REGISTER: 'Inscription',
  ACCOUNT_CREATED: 'Compte créé',
  ACCOUNT_CLOSED: 'Compte clôturé',
  DEPOSIT: 'Dépôt',
  TRANSFER: 'Virement',
  BENEFICIARY_ADDED: 'Bénéficiaire ajouté',
  BENEFICIARY_REMOVED: 'Bénéficiaire supprimé',
};

function actionLabel(action: string): string {
  return LABELS[action] ?? action;
}

function actionClass(action: string): string {
  if (action === 'LOGIN_FAILURE') return 'audit-tag danger';
  if (action === 'TRANSFER' || action === 'DEPOSIT') return 'audit-tag money';
  return 'audit-tag';
}

const PAGE_SIZE = 25;

export function AuditLog({ onClose }: { onClose: () => void }) {
  const [items, setItems] = useState<AuditEntry[]>([]);
  const [page, setPage] = useState(0);
  const [total, setTotal] = useState(0);
  const [hasMore, setHasMore] = useState(false);
  const [query, setQuery] = useState('');
  const [activeQuery, setActiveQuery] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  async function load() {
    setLoading(true);
    setError(null);
    try {
      const res = await api.listAudit({ q: activeQuery, page, size: PAGE_SIZE });
      setItems(res.items);
      setTotal(res.total);
      setHasMore(res.hasMore);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Erreur');
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page, activeQuery]);

  function runSearch() {
    setPage(0);
    setActiveQuery(query.trim());
  }

  const from = total === 0 ? 0 : page * PAGE_SIZE + 1;
  const to = page * PAGE_SIZE + items.length;

  return (
    <section className="audit">
      <div className="section-head">
        <h2>Journal d'audit</h2>
        <button className="ghost" onClick={onClose}>
          Retour
        </button>
      </div>

      <div className="audit-toolbar">
        <input
          className="audit-search"
          placeholder="Rechercher (acteur, action, détail)…"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && runSearch()}
        />
        <button className="primary" onClick={runSearch}>
          Rechercher
        </button>
        {activeQuery && (
          <button
            className="ghost"
            onClick={() => {
              setQuery('');
              setPage(0);
              setActiveQuery('');
            }}
          >
            Effacer
          </button>
        )}
      </div>

      {loading && <p className="muted">Chargement…</p>}
      {error && <p className="error">{error}</p>}
      {!loading && items.length === 0 && <p className="muted">Aucune entrée.</p>}

      {items.length > 0 && (
        <>
          <div className="audit-rows">
            <div className="audit-row audit-row-head">
              <span>Date</span>
              <span>Acteur</span>
              <span>Action</span>
              <span>Détail</span>
            </div>
            {items.map((e) => (
              <div key={e.id} className="audit-row">
                <span className="audit-date" data-label="Date">
                  {new Date(e.at).toLocaleString('fr-FR')}
                </span>
                <span className="audit-actor" data-label="Acteur">
                  {e.actor}
                </span>
                <span data-label="Action">
                  <span className={actionClass(e.action)}>{actionLabel(e.action)}</span>
                </span>
                <span className="audit-detail" data-label="Détail">
                  {e.detail}
                </span>
              </div>
            ))}
          </div>

          <div className="audit-pager">
            <button className="ghost" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
              ← Précédent
            </button>
            <span className="muted">
              {from}–{to} sur {total}
            </span>
            <button className="ghost" disabled={!hasMore} onClick={() => setPage((p) => p + 1)}>
              Suivant →
            </button>
          </div>
        </>
      )}
    </section>
  );
}
