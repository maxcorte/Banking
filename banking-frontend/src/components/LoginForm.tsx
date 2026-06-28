import { useState, type FormEvent } from 'react';
import { useAuth } from '../auth';

export function LoginForm() {
  const { login, register } = useAuth();
  const [mode, setMode] = useState<'login' | 'register'>('login');
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setBusy(true);
    try {
      if (mode === 'login') {
        await login(username, password);
      } else {
        await register(username, password);
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Erreur inconnue');
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="auth-screen">
      <form className="card auth-card" onSubmit={handleSubmit}>
        <h1 className="brand">Ma Banque coucou</h1>
        <p className="subtitle">
          {mode === 'login' ? 'Connectez-vous à votre espace' : 'Créez votre compte'}
        </p>

        <label>
          Nom d'utilisateur
          <input
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            autoComplete="username"
            required
          />
        </label>

        <label>
          Mot de passe
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            autoComplete={mode === 'login' ? 'current-password' : 'new-password'}
            required
          />
        </label>

        {error && <p className="error">{error}</p>}

        <button type="submit" disabled={busy}>
          {busy ? '…' : mode === 'login' ? 'Se connecter' : "S'inscrire"}
        </button>

        <p className="switch">
          {mode === 'login' ? 'Pas encore de compte ?' : 'Déjà inscrit ?'}{' '}
          <button
            type="button"
            className="link"
            onClick={() => {
              setMode(mode === 'login' ? 'register' : 'login');
              setError(null);
            }}
          >
            {mode === 'login' ? "S'inscrire" : 'Se connecter'}
          </button>
        </p>
      </form>
    </div>
  );
}
