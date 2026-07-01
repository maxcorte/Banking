import { useEffect, useState } from 'react';
import QRCode from 'qrcode';
import { api } from '../api';

type Phase = 'loading' | 'disabled' | 'setup' | 'enabled';

export default function Security({ onClose, onChanged }: { onClose: () => void; onChanged: () => void }) {
  const [phase, setPhase] = useState<Phase>('loading');
  const [secret, setSecret] = useState('');
  const [qr, setQr] = useState('');
  const [code, setCode] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function loadStatus() {
    try {
      const { enabled } = await api.twoFactorStatus();
      setPhase(enabled ? 'enabled' : 'disabled');
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Erreur');
      setPhase('disabled');
    }
  }

  useEffect(() => {
    loadStatus();
  }, []);

  async function startSetup() {
    setError(null);
    setBusy(true);
    try {
      const { secret: s, otpauthUri } = await api.twoFactorSetup();
      setSecret(s);
      setQr(await QRCode.toDataURL(otpauthUri, { margin: 1, width: 220 }));
      setCode('');
      setPhase('setup');
    } catch (e) {
      setError(e instanceof Error ? e.message : "Échec de l'initialisation.");
    } finally {
      setBusy(false);
    }
  }

  async function confirmEnable() {
    setError(null);
    setBusy(true);
    try {
      await api.twoFactorEnable(code.trim());
      setCode('');
      setPhase('enabled');
      onChanged();
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Code invalide.');
    } finally {
      setBusy(false);
    }
  }

  async function disable() {
    setError(null);
    setBusy(true);
    try {
      await api.twoFactorDisable(code.trim());
      setCode('');
      setPhase('disabled');
      onChanged();
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Code invalide.');
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="card security-card">
      <div className="req-head">
        <h2>Sécurité — Authentification à deux facteurs</h2>
        <button className="ghost" onClick={onClose}>
          Fermer
        </button>
      </div>

      {phase === 'loading' && <p className="muted">Chargement…</p>}

      {phase === 'disabled' && (
        <>
          <p className="muted">
            La 2FA ajoute un code à usage unique (via une appli comme Google
            Authenticator ou Authy) pour valider tes virements. Fortement
            recommandé.
          </p>
          <button className="primary" disabled={busy} onClick={startSetup}>
            Activer la 2FA
          </button>
        </>
      )}

      {phase === 'setup' && (
        <div className="twofa-setup">
          <ol className="twofa-steps">
            <li>Scanne ce QR code avec ton appli d'authentification.</li>
            <li>Saisis le code à 6 chiffres affiché pour confirmer.</li>
          </ol>
          {qr && <img className="twofa-qr" src={qr} alt="QR code 2FA" />}
          <p className="twofa-secret">
            Clé manuelle : <code>{secret}</code>
          </p>
          <input
            className="twofa-code"
            inputMode="numeric"
            maxLength={6}
            placeholder="Code à 6 chiffres"
            value={code}
            onChange={(e) => setCode(e.target.value.replace(/\D/g, ''))}
          />
          <div className="row">
            <button className="primary" disabled={busy} onClick={confirmEnable}>
              Confirmer
            </button>
            <button className="ghost" disabled={busy} onClick={() => setPhase('disabled')}>
              Annuler
            </button>
          </div>
        </div>
      )}

      {phase === 'enabled' && (
        <>
          <p className="twofa-on">✅ La 2FA est active. Tes virements demandent un code.</p>
          <p className="muted">Pour la désactiver, saisis un code de ton appli :</p>
          <input
            className="twofa-code"
            inputMode="numeric"
            maxLength={6}
            placeholder="Code à 6 chiffres"
            value={code}
            onChange={(e) => setCode(e.target.value.replace(/\D/g, ''))}
          />
          <button className="danger" disabled={busy} onClick={disable}>
            Désactiver la 2FA
          </button>
        </>
      )}

      {error && <p className="error">{error}</p>}
    </div>
  );
}
