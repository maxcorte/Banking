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

export function AuditLog({ onClose }: { onClose: () => void }) {
  const [entries, setEntries] = useState<AuditEntry[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  async function load() {
    setLoading(true);
    setError(null);
    try {
      setEntries(await api.listAudit(200));
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Erreur');
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load();
  }, []);

  return (
    <section className="audit">
      <div className="section-head">
        <h2>Journal d'audit</h2>
        <div className="row">
          <button className="ghost" onClick={load}>
            Rafraîchir
          </button>
          <button className="ghost" onClick={onClose}>
            Retour
          </button>
        </div>
      </div>

      {loading && <p className="muted">Chargement…</p>}
      {error && <p className="error">{error}</p>}
      {!loading && entries.length === 0 && <p className="muted">Aucune entrée.</p>}

      {entries.length > 0 && (
        <table className="audit-table">
          <thead>
            <tr>
              <th>Date</th>
              <th>Acteur</th>
              <th>Action</th>
              <th>Détail</th>
            </tr>
          </thead>
          <tbody>
            {entries.map((e) => (
              <tr key={e.id}>
                <td className="nowrap">{new Date(e.at).toLocaleString('fr-FR')}</td>
                <td>{e.actor ?? '—'}</td>
                <td>
                  <span className={actionClass(e.action)}>{actionLabel(e.action)}</span>
                </td>
                <td>{e.detail ?? ''}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  );
}
