import { useEffect, useState } from 'react';
import QRCode from 'qrcode';
import { create as webauthnCreate } from '@github/webauthn-json';
import { api } from '../api';
import type { Passkey } from '../types';

type Phase = 'loading' | 'disabled' | 'setup' | 'enabled';

export default function Security({ onClose, onChanged }: { onClose: () => void; onChanged: () => void }) {
  // ----- 2FA (TOTP) -----
  const [phase, setPhase] = useState<Phase>('loading');
  const [secret, setSecret] = useState('');
  const [qr, setQr] = useState('');
  const [code, setCode] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  // ----- Passkeys (WebAuthn) -----
  const passkeySupported =
    typeof window !== 'undefined' && typeof window.PublicKeyCredential !== 'undefined';
  const [passkeys, setPasskeys] = useState<Passkey[]>([]);
  const [pkName, setPkName] = useState('');
  const [pkError, setPkError] = useState<string | null>(null);
  const [pkBusy, setPkBusy] = useState(false);

  async function loadStatus() {
    try {
      const { enabled } = await api.twoFactorStatus();
      setPhase(enabled ? 'enabled' : 'disabled');
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Erreur');
      setPhase('disabled');
    }
  }

  async function loadPasskeys() {
    try {
      setPasskeys(await api.webauthnListCredentials());
    } catch {
      setPasskeys([]);
    }
  }

  useEffect(() => {
    loadStatus();
    if (passkeySupported) loadPasskeys();
    // eslint-disable-next-line react-hooks/exhaustive-deps
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

  async function addPasskey() {
    setPkError(null);
    setPkBusy(true);
    try {
      const { flowId, optionsJson } = await api.webauthnRegisterStart();
      const options = JSON.parse(optionsJson);
      const credential = await webauthnCreate(options);
      await api.webauthnRegisterFinish(
        flowId,
        JSON.stringify(credential),
        pkName.trim() || 'Ma passkey',
      );
      setPkName('');
      await loadPasskeys();
    } catch (e) {
      const msg = e instanceof Error ? e.message : 'Ajout impossible.';
      if (!/abort|notallowed|cancell?ed/i.test(msg)) setPkError(msg);
    } finally {
      setPkBusy(false);
    }
  }

  async function removePasskey(id: string) {
    setPkError(null);
    setPkBusy(true);
    try {
      await api.webauthnDeleteCredential(id);
      await loadPasskeys();
    } catch (e) {
      setPkError(e instanceof Error ? e.message : 'Suppression impossible.');
    } finally {
      setPkBusy(false);
    }
  }

  return (
    <>
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

      <div className="card security-card">
        <h2>Passkeys — Face ID / Touch ID / Windows Hello</h2>
        <p className="muted">
          Une passkey te permet de te connecter d'un simple regard ou d'une
          empreinte, sans mot de passe. Elle reste sur ton appareil ; le serveur
          ne stocke qu'une clé publique.
        </p>

        {!passkeySupported ? (
          <p className="muted">Cet appareil / navigateur ne gère pas les passkeys.</p>
        ) : (
          <>
            <div className="contacts-add">
              <input
                placeholder="Nom (ex. iPhone de Maxime)"
                value={pkName}
                onChange={(e) => setPkName(e.target.value)}
              />
              <button className="primary" disabled={pkBusy} onClick={addPasskey}>
                Ajouter une passkey
              </button>
            </div>

            <div className="contacts-list">
              {passkeys.length === 0 && (
                <p className="muted">Aucune passkey enregistrée pour l'instant.</p>
              )}
              {passkeys.map((p) => (
                <div key={p.id} className="contact-item">
                  <span className="contact-avatar">🔑</span>
                  <span className="contact-info">
                    <span className="contact-name">{p.label}</span>
                    <span className="contact-iban">
                      Ajoutée le {new Date(p.createdAt).toLocaleDateString('fr-FR')}
                    </span>
                  </span>
                  <button
                    className="ghost contact-remove"
                    disabled={pkBusy}
                    onClick={() => removePasskey(p.id)}
                  >
                    Retirer
                  </button>
                </div>
              ))}
            </div>

            {pkError && <p className="error">{pkError}</p>}
          </>
        )}
      </div>
    </>
  );
}
