import { useEffect, useMemo, useState } from 'react';
import QRCode from 'qrcode';
import type { Account } from '../types';
import { copyText } from '../clipboard';

/**
 * « Recevoir » : génère un QR code (et un lien partageable) qui encode une
 * demande de paiement vers l'un de tes comptes. Quiconque scanne le QR avec
 * l'appareil photo de son téléphone ouvre l'app, qui pré-remplit le virement.
 *
 * Le lien a la forme :  https://…/?pay=<numéro>&amt=<centimes>&desc=<motif>
 * (montant et motif optionnels).
 */
export function ReceiveQR({
  accounts,
  onClose,
}: {
  accounts: Account[];
  onClose: () => void;
}) {
  const [accountId, setAccountId] = useState(accounts[0]?.id ?? '');
  const [amount, setAmount] = useState('');
  const [description, setDescription] = useState('');
  const [dataUrl, setDataUrl] = useState<string | null>(null);
  const [copied, setCopied] = useState(false);

  const account = accounts.find((a) => a.id === accountId) ?? null;

  // Construit le lien de paiement à partir des champs.
  const payLink = useMemo(() => {
    if (!account) return '';
    const params = new URLSearchParams();
    params.set('pay', account.accountNumber);
    const minor = Math.round(parseFloat(amount) * 100);
    if (Number.isFinite(minor) && minor > 0) params.set('amt', String(minor));
    if (description.trim()) params.set('desc', description.trim());
    return `${window.location.origin}/?${params.toString()}`;
  }, [account, amount, description]);

  // (Re)génère le QR code à chaque changement de lien.
  useEffect(() => {
    let active = true;
    if (!payLink) {
      setDataUrl(null);
      return;
    }
    QRCode.toDataURL(payLink, { margin: 1, width: 240, errorCorrectionLevel: 'M' })
      .then((url) => {
        if (active) setDataUrl(url);
      })
      .catch(() => {
        if (active) setDataUrl(null);
      });
    return () => {
      active = false;
    };
  }, [payLink]);

  async function handleCopy() {
    if (await copyText(payLink)) {
      setCopied(true);
      setTimeout(() => setCopied(false), 1500);
    }
  }

  async function handleShare() {
    // L'API de partage natif n'existe que sur mobile / navigateurs compatibles.
    if (navigator.share) {
      try {
        await navigator.share({ title: 'Demande de paiement', url: payLink });
      } catch {
        /* partage annulé : on ignore */
      }
    } else {
      handleCopy();
    }
  }

  return (
    <div className="card receive-card">
      <div className="section-head">
        <h2>Recevoir un paiement</h2>
        <button className="ghost" onClick={onClose}>
          Fermer
        </button>
      </div>

      {accounts.length === 0 ? (
        <p className="muted">Crée d'abord un compte pour pouvoir recevoir un paiement.</p>
      ) : (
        <>
          <label>
            Compte à créditer
            <select value={accountId} onChange={(e) => setAccountId(e.target.value)}>
              {accounts.map((a) => (
                <option key={a.id} value={a.id}>
                  {a.ownerName} — {a.accountNumber}
                </option>
              ))}
            </select>
          </label>

          <div className="row" style={{ marginTop: '0.6rem' }}>
            <input
              type="number"
              step="0.01"
              min="0.01"
              placeholder="Montant en € (optionnel)"
              value={amount}
              onChange={(e) => setAmount(e.target.value)}
            />
          </div>

          <input
            placeholder="Motif (optionnel)"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            style={{ marginTop: '0.6rem' }}
          />

          {dataUrl && (
            <div className="qr-wrap">
              <img src={dataUrl} alt="QR code de demande de paiement" width={240} height={240} />
              <p className="muted qr-hint">
                Fais scanner ce QR code (ou partage le lien). La personne ouvre l'app avec le
                virement déjà pré-rempli.
              </p>
            </div>
          )}

          <div className="row qr-actions">
            <button type="button" className="ghost" onClick={handleCopy}>
              {copied ? 'Lien copié ✓' : 'Copier le lien'}
            </button>
            <button type="button" onClick={handleShare}>
              Partager
            </button>
          </div>
        </>
      )}
    </div>
  );
}
