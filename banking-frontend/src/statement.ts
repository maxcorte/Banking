import type { Account, TransactionLine } from './types';
import { categoryLabel } from './categories';

function eurosPlain(minor: number): string {
  return (minor / 100).toFixed(2).replace('.', ',');
}

function eurosLabel(minor: number): string {
  return new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'EUR' }).format(minor / 100);
}

function typeLabel(l: TransactionLine): string {
  if (l.kind === 'DEPOSIT') return 'Dépôt';
  return l.amountMinor >= 0 ? 'Virement reçu' : 'Virement émis';
}

function csvCell(value: string): string {
  return /[";\n]/.test(value) ? '"' + value.replace(/"/g, '""') + '"' : value;
}

function escapeHtml(s: string): string {
  return s.replace(
    /[&<>"]/g,
    (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c] as string),
  );
}

function triggerDownload(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);
}

/** Télécharge l'historique du compte au format CSV (séparateur ';', décimales ','). */
export function downloadStatementCsv(account: Account, lines: TransactionLine[]) {
  const header = ['Date', 'Type', 'Catégorie', 'Contrepartie', 'IBAN', 'Montant (EUR)', 'Solde (EUR)'];
  const rows = lines.map((l) => [
    new Date(l.at).toLocaleString('fr-FR'),
    typeLabel(l),
    categoryLabel(l.category) ?? '',
    l.counterpartyName ?? '',
    l.counterpartyNumber ?? '',
    eurosPlain(l.amountMinor),
    eurosPlain(l.balanceAfterMinor),
  ]);
  const csv = [header, ...rows]
    .map((r) => r.map((c) => csvCell(String(c))).join(';'))
    .join('\r\n');
  // BOM pour qu'Excel reconnaisse l'UTF-8 (accents).
  const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8;' });
  triggerDownload(blob, `releve-${account.accountNumber}.csv`);
}

/**
 * Génère un vrai fichier PDF téléchargeable (relevé de compte).
 * Sur mobile, le navigateur propose alors de l'enregistrer/partager —
 * plus d'onglet imprimable bloqué sans retour possible.
 */
export async function downloadStatementPdf(account: Account, lines: TransactionLine[]) {
  // Imports dynamiques : la lib n'est chargée que si l'utilisateur exporte.
  const { jsPDF } = await import('jspdf');
  const autoTable = (await import('jspdf-autotable')).default;

  // IMPORTANT : la police PDF standard ne gère pas les espaces fines
  // insécables (U+202F) ni le moins typographique (U+2212) que produit
  // Intl.NumberFormat. On formate donc en ASCII pur.
  const euroAbs = (minorAbs: number): string => {
    const [int, dec] = (Math.abs(minorAbs) / 100).toFixed(2).split('.');
    const grouped = int.replace(/\B(?=(\d{3})+(?!\d))/g, ' ');
    return `${grouped},${dec} EUR`;
  };
  const signedEuro = (minor: number): string => (minor < 0 ? '-' : '') + euroAbs(minor);

  const doc = new jsPDF({ unit: 'pt', format: 'a4' });
  const marginX = 40;

  doc.setFontSize(16);
  doc.setTextColor(26, 34, 54);
  doc.text('Relevé de compte', marginX, 48);
  doc.setFontSize(10);
  doc.setTextColor(107, 116, 136);
  doc.text(`${account.ownerName} - ${account.accountNumber}`, marginX, 66);
  doc.text(`Solde actuel : ${signedEuro(account.balanceMinor)}`, marginX, 80);
  doc.text(`Edite le ${new Date().toLocaleString('fr-FR')}`, marginX, 94);

  const body = lines.map((l) => {
    const incoming = l.amountMinor >= 0;
    const cat = categoryLabel(l.category);
    const op = l.counterpartyName ? `${typeLabel(l)} - ${l.counterpartyName}` : typeLabel(l);
    return [
      new Date(l.at).toLocaleString('fr-FR'),
      cat ? `${op} (${cat})` : op,
      `${incoming ? '+' : '-'}${euroAbs(l.amountMinor)}`,
      signedEuro(l.balanceAfterMinor),
    ];
  });

  autoTable(doc, {
    startY: 112,
    head: [['Date', 'Opération', 'Montant', 'Solde']],
    body,
    styles: { fontSize: 8, cellPadding: 4, overflow: 'linebreak' },
    headStyles: { fillColor: [31, 111, 235], textColor: 255 },
    columnStyles: {
      0: { cellWidth: 105 },
      1: { cellWidth: 'auto' },
      2: { cellWidth: 78, halign: 'right' },
      3: { cellWidth: 78, halign: 'right' },
    },
    margin: { left: marginX, right: marginX },
    tableWidth: 'auto',
  });

  doc.save(`releve-${account.accountNumber}.pdf`);
}

/** Ouvre un relevé imprimable : l'utilisateur peut « Imprimer → Enregistrer en PDF ». */
export function printStatement(account: Account, lines: TransactionLine[]) {
  const rowsHtml = lines
    .map((l) => {
      const incoming = l.amountMinor >= 0;
      const cat = categoryLabel(l.category);
      const base = l.counterpartyName
        ? `${typeLabel(l)} — ${escapeHtml(l.counterpartyName)}`
        : typeLabel(l);
      const label = cat ? `${base} <em>(${escapeHtml(cat)})</em>` : base;
      return `<tr>
        <td>${new Date(l.at).toLocaleString('fr-FR')}</td>
        <td>${label}</td>
        <td class="num ${incoming ? 'pos' : 'neg'}">${incoming ? '+' : '−'}${eurosLabel(Math.abs(l.amountMinor))}</td>
        <td class="num">${eurosLabel(l.balanceAfterMinor)}</td>
      </tr>`;
    })
    .join('');

  const html = `<!doctype html><html lang="fr"><head><meta charset="utf-8">
  <title>Relevé ${account.accountNumber}</title>
  <style>
    body{font-family:system-ui,Arial,sans-serif;color:#1a2236;margin:2rem;}
    h1{font-size:1.3rem;margin:0 0 .2rem;}
    .meta{color:#6b7488;font-size:.9rem;margin-bottom:1.2rem;line-height:1.5;}
    table{width:100%;border-collapse:collapse;font-size:.85rem;}
    th,td{text-align:left;padding:.4rem .5rem;border-bottom:1px solid #e2e6ef;}
    th{color:#6b7488;}
    .num{text-align:right;white-space:nowrap;}
    .pos{color:#11875a;} .neg{color:#c0392b;}
    @media print{body{margin:1rem;}}
  </style></head><body>
  <h1>Relevé de compte</h1>
  <div class="meta">${escapeHtml(account.ownerName)} · ${account.accountNumber}<br>
    Solde actuel : <strong>${eurosLabel(account.balanceMinor)}</strong><br>
    Édité le ${new Date().toLocaleString('fr-FR')}</div>
  <table>
    <thead><tr><th>Date</th><th>Opération</th><th class="num">Montant</th><th class="num">Solde</th></tr></thead>
    <tbody>${rowsHtml}</tbody>
  </table>
  <script>window.onload=function(){window.print();};</script>
  </body></html>`;

  const win = window.open('', '_blank');
  if (!win) {
    return;
  }
  win.document.open();
  win.document.write(html);
  win.document.close();
}
