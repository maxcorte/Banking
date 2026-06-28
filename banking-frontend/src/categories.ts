export interface CategoryOption {
  value: string;
  label: string;
  color: string;
}

// Doit correspondre a l'enum TransactionCategory cote serveur.
export const CATEGORIES: CategoryOption[] = [
  { value: 'AUTRES', label: 'Autre', color: '#94a3b8' },
  { value: 'COURSES', label: 'Courses', color: '#f59e0b' },
  { value: 'LOYER', label: 'Loyer', color: '#6366f1' },
  { value: 'SALAIRE', label: 'Salaire', color: '#10b981' },
  { value: 'FACTURES', label: 'Factures', color: '#ef4444' },
  { value: 'TRANSPORT', label: 'Transport', color: '#3b82f6' },
  { value: 'LOISIRS', label: 'Loisirs', color: '#ec4899' },
  { value: 'RESTAURANT', label: 'Restaurant', color: '#f97316' },
  { value: 'SANTE', label: 'Santé', color: '#14b8a6' },
  { value: 'EPARGNE', label: 'Épargne', color: '#22c55e' },
  { value: 'CADEAU', label: 'Cadeau', color: '#a855f7' },
];

export const CATEGORY_LABELS: Record<string, string> = Object.fromEntries(
  CATEGORIES.map((c) => [c.value, c.label]),
);

const CATEGORY_COLORS: Record<string, string> = Object.fromEntries(
  CATEGORIES.map((c) => [c.value, c.color]),
);

/** Libellé d'affichage : null pour AUTRES (pas de pastille dans l'historique). */
export function categoryLabel(value: string | null): string | null {
  if (!value || value === 'AUTRES') return null;
  return CATEGORY_LABELS[value] ?? value;
}

/** Libellé toujours présent (pour les statistiques : AUTRES -> "Autre"). */
export function categoryName(value: string | null): string {
  if (!value) return 'Autre';
  return CATEGORY_LABELS[value] ?? value;
}

export function categoryColor(value: string | null): string {
  if (!value) return CATEGORY_COLORS.AUTRES;
  return CATEGORY_COLORS[value] ?? CATEGORY_COLORS.AUTRES;
}
