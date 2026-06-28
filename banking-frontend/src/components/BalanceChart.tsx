import { formatEuros } from '../api';
import type { TransactionLine } from '../types';

/**
 * Petit graphique d'évolution du solde, dessiné en SVG pur (aucune dépendance).
 * Reçoit l'historique (du plus récent au plus ancien) et trace le solde courant.
 */
export function BalanceChart({ lines }: { lines: TransactionLine[] }) {
  if (lines.length < 2) {
    return null;
  }

  // Ordre chronologique (l'historique arrive du plus récent au plus ancien).
  const points = [...lines]
    .reverse()
    .map((l) => ({ t: new Date(l.at).getTime(), v: l.balanceAfterMinor }));

  const w = 600;
  const h = 160;
  const pad = 8;

  const xs = points.map((p) => p.t);
  const ys = points.map((p) => p.v);
  const minX = Math.min(...xs);
  const maxX = Math.max(...xs);
  const minY = Math.min(...ys, 0);
  const maxY = Math.max(...ys, 0);
  const spanX = maxX - minX || 1;
  const spanY = maxY - minY || 1;

  const sx = (t: number) => pad + ((t - minX) / spanX) * (w - 2 * pad);
  const sy = (v: number) => h - pad - ((v - minY) / spanY) * (h - 2 * pad);

  const line = points
    .map((p, i) => `${i === 0 ? 'M' : 'L'} ${sx(p.t).toFixed(1)} ${sy(p.v).toFixed(1)}`)
    .join(' ');
  const area =
    `${line} L ${sx(points[points.length - 1].t).toFixed(1)} ${h - pad} ` +
    `L ${sx(points[0].t).toFixed(1)} ${h - pad} Z`;

  const current = ys[ys.length - 1];

  return (
    <div className="chart">
      <div className="chart-head">
        <span className="muted">Évolution du solde</span>
        <span className="chart-current">{formatEuros(current)}</span>
      </div>
      <svg viewBox={`0 0 ${w} ${h}`} className="chart-svg" preserveAspectRatio="none">
        <path d={area} className="chart-area" />
        {minY < 0 && (
          <line x1={pad} x2={w - pad} y1={sy(0)} y2={sy(0)} className="chart-zero" />
        )}
        <path d={line} className="chart-line" />
      </svg>
    </div>
  );
}
