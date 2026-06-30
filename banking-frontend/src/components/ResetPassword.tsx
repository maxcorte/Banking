import { useState, type FormEvent } from 'react';
import { api } from '../api';

/**
 * Écran de réinitialisation, affiché lorsque l'URL contient ?reset=JETON
 * (lien reçu par e-mail). On envoie le jeton + le nouveau mot de passe, puis
 * on renvoie l'utilisateur vers la connexion (en nettoyant l'URL).
 */
export function ResetPassword({ token }: { token: string }) {
  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [done, setDone] = useState(false);
  const [busy, setBusy] = useState(false);

  function goToLogin() {
    // Retire ?reset=… de l'URL et recharge l'app proprement sur l'accueil.
    window.history.replaceState(null, '', '/');
    window.location.reload();
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    if (password.length < 8) {
      setError('Le mot de passe doit contenir au moins 8 caractères.');
      return;
    }
    if (password !== confirm) {
      setError('Les deux mots de passe ne correspondent pas.');
      return;
    }
    setBusy(true);
    try {
      await api.resetPassword(token, password);
      setDone(true);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Erreur inconnue');
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="auth-screen">
      <div className="card auth-card">
        <h1 className="brand">Ma Banque</h1>

        {done ? (
          <>
            <p className="subtitle">Mot de passe réinitialisé</p>
            <p className="info-message">
              Ton mot de passe a été mis à jour. Tu peux maintenant te connecter.
            </p>
            <button type="button" onClick={goToLogin}>
              Aller à la connexion
            </button>
          </>
        ) : (
          <form onSubmit={handleSubmit}>
            <p className="subtitle">Choisis un nouveau mot de passe</p>

            <label>
              Nouveau mot de passe
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                autoComplete="new-password"
                required
              />
            </label>

            <label>
              Confirmer le mot de passe
              <input
                type="password"
                value={confirm}
                onChange={(e) => setConfirm(e.target.value)}
                autoComplete="new-password"
                required
              />
            </label>

            {error && <p className="error">{error}</p>}

            <button type="submit" disabled={busy}>
              {busy ? '…' : 'Réinitialiser'}
            </button>

            <p className="switch">
              <button type="button" className="link" onClick={goToLogin}>
                ← Retour à la connexion
              </button>
            </p>
          </form>
        )}
      </div>
    </div>
  );
}
