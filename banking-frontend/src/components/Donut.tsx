export interface DonutSlice {
  label: string;
  amount: number;
  color: string;
}

/** Graphique en anneau (donut) dessiné en SVG pur, sans dépendance. */
export function Donut({ slices, size = 190 }: { slices: DonutSlice[]; size?: number }) {
  const total = slices.reduce((s, x) => s + x.amount, 0);
  if (total <= 0) {
    return null;
  }

  const r = size / 2;
  const inner = r * 0.62;
  const cx = r;
  const cy = r;

  // Cas particulier : une seule catégorie -> un anneau plein (l'arc se fermerait sinon).
  if (slices.length === 1) {
    const mid = (r + inner) / 2;
    return (
      <svg viewBox={`0 0 ${size} ${size}`} width={size} height={size} className="donut">
        <circle cx={cx} cy={cy} r={mid} fill="none" stroke={slices[0].color} strokeWidth={r - inner} />
      </svg>
    );
  }

  let angle = -Math.PI / 2;
  const arcs = slices.map((s) => {
    const frac = s.amount / total;
    const a0 = angle;
    const a1 = angle + frac * 2 * Math.PI;
    angle = a1;
    const large = frac > 0.5 ? 1 : 0;
    const x0 = cx + r * Math.cos(a0);
    const y0 = cy + r * Math.sin(a0);
    const x1 = cx + r * Math.cos(a1);
    const y1 = cy + r * Math.sin(a1);
    const xi1 = cx + inner * Math.cos(a1);
    const yi1 = cy + inner * Math.sin(a1);
    const xi0 = cx + inner * Math.cos(a0);
    const yi0 = cy + inner * Math.sin(a0);
    const d =
      `M ${x0.toFixed(2)} ${y0.toFixed(2)} ` +
      `A ${r} ${r} 0 ${large} 1 ${x1.toFixed(2)} ${y1.toFixed(2)} ` +
      `L ${xi1.toFixed(2)} ${yi1.toFixed(2)} ` +
      `A ${inner} ${inner} 0 ${large} 0 ${xi0.toFixed(2)} ${yi0.toFixed(2)} Z`;
    return { d, color: s.color };
  });

  return (
    <svg viewBox={`0 0 ${size} ${size}`} width={size} height={size} className="donut">
      {arcs.map((a, i) => (
        <path key={i} d={a.d} fill={a.color} />
      ))}
    </svg>
  );
}
