import { useState, type FormEvent } from 'react';
import { useAuth } from '../auth';
import { api } from '../api';

type Mode = 'login' | 'register' | 'forgot';

export function LoginForm() {
  const { login, loginWithPasskey, register } = useAuth();
  const passkeySupported =
    typeof window !== 'undefined' && typeof window.PublicKeyCredential !== 'undefined';
  const [mode, setMode] = useState<Mode>('login');
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [info, setInfo] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  function switchMode(next: Mode) {
    setMode(next);
    setError(null);
    setInfo(null);
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setInfo(null);
    setBusy(true);
    try {
      if (mode === 'login') {
        await login(username, password);
      } else if (mode === 'register') {
        await register(username, email, password);
      } else {
        await api.forgotPassword(email);
        setInfo(
          "Si un compte est associé à cette adresse, un e-mail contenant un lien de réinitialisation vient d'être envoyé.",
        );
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Erreur inconnue');
    } finally {
      setBusy(false);
    }
  }

  async function handlePasskey() {
    setError(null);
    setInfo(null);
    setBusy(true);
    try {
      await loginWithPasskey();
    } catch (err) {
      // L'utilisateur qui annule Face ID / Touch ID ne doit pas voir d'erreur alarmante.
      const msg = err instanceof Error ? err.message : 'Connexion par passkey impossible.';
      if (!/abort|notallowed|cancell?ed/i.test(msg)) {
        setError(msg);
      }
    } finally {
      setBusy(false);
    }
  }

  const title =
    mode === 'login'
      ? 'Connectez-vous à votre espace'
      : mode === 'register'
        ? 'Créez votre compte'
        : 'Réinitialiser votre mot de passe';

  const submitLabel =
    mode === 'login' ? 'Se connecter' : mode === 'register' ? "S'inscrire" : 'Envoyer le lien';

  return (
    <div className="auth-screen">
      <form className="card auth-card" onSubmit={handleSubmit}>
        <h1 className="brand">Ma Banque</h1>
        <p className="subtitle">{title}</p>

        {mode !== 'forgot' && (
          <label>
            Nom d'utilisateur
            <input
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              autoComplete="username"
              required
            />
          </label>
        )}

        {(mode === 'register' || mode === 'forgot') && (
          <label>
            Adresse e-mail
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              autoComplete="email"
              required
            />
          </label>
        )}

        {mode !== 'forgot' && (
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
        )}

        {error && <p className="error">{error}</p>}
        {info && <p className="info-message">{info}</p>}

        <button type="submit" disabled={busy}>
          {busy ? '…' : submitLabel}
        </button>

        {mode === 'login' && passkeySupported && (
          <>
            <div className="auth-sep">ou</div>
            <button
              type="button"
              className="passkey-btn"
              disabled={busy}
              onClick={handlePasskey}
            >
              🔑 Se connecter avec une passkey
            </button>
          </>
        )}

        {mode === 'login' && (
          <p className="switch">
            <button type="button" className="link" onClick={() => switchMode('forgot')}>
              Mot de passe oublié ?
            </button>
          </p>
        )}

        {mode !== 'forgot' ? (
          <p className="switch">
            {mode === 'login' ? 'Pas encore de compte ?' : 'Déjà inscrit ?'}{' '}
            <button
              type="button"
              className="link"
              onClick={() => switchMode(mode === 'login' ? 'register' : 'login')}
            >
              {mode === 'login' ? "S'inscrire" : 'Se connecter'}
            </button>
          </p>
        ) : (
          <p className="switch">
            <button type="button" className="link" onClick={() => switchMode('login')}>
              ← Retour à la connexion
            </button>
          </p>
        )}
      </form>
    </div>
  );
}
