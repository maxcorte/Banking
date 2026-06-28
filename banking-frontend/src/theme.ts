const KEY = 'banking.theme';

export function initTheme(): void {
  const saved = localStorage.getItem(KEY);
  document.documentElement.dataset.theme = saved === 'dark' ? 'dark' : 'light';
}

export function getTheme(): string {
  return document.documentElement.dataset.theme === 'dark' ? 'dark' : 'light';
}

export function toggleTheme(): string {
  const next = getTheme() === 'dark' ? 'light' : 'dark';
  document.documentElement.dataset.theme = next;
  try {
    localStorage.setItem(KEY, next);
  } catch {
    /* stockage indisponible : on garde le thème en mémoire seulement */
  }
  return next;
}
