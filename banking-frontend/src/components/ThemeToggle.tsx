import { useState } from 'react';
import { getTheme, toggleTheme } from '../theme';

export function ThemeToggle() {
  const [theme, setTheme] = useState(getTheme());
  return (
    <button className="link" onClick={() => setTheme(toggleTheme())} title="Changer de thème">
      {theme === 'dark' ? '☀ Clair' : '☾ Sombre'}
    </button>
  );
}
