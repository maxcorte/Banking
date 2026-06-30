import { useEffect, useRef, useState } from 'react';
import jsQR from 'jsqr';

export interface ScannedPay {
  iban: string;
  amount: string; // euros, ex "12.50" (vide si non précisé)
  desc: string;
}

/** Tente d'extraire une demande de paiement d'un texte de QR code. */
function parsePay(text: string): ScannedPay | null {
  try {
    const url = new URL(text);
    const pay = url.searchParams.get('pay');
    if (!pay) return null;
    const amt = url.searchParams.get('amt');
    return {
      iban: pay,
      amount: amt && Number(amt) > 0 ? (Number(amt) / 100).toFixed(2) : '',
      desc: url.searchParams.get('desc') ?? '',
    };
  } catch {
    // Pas une URL : on accepte un numéro de compte brut comme destinataire.
    const t = text.trim();
    if (t.length >= 6 && t.length <= 40 && !t.includes(' ')) {
      return { iban: t, amount: '', desc: '' };
    }
    return null;
  }
}

/**
 * Scanner de QR code via la caméra. Fonctionne sur tout navigateur supportant
 * getUserMedia (HTTPS requis), en décodant les images avec jsQR — pas besoin
 * de l'appareil photo système.
 */
export function QrScanner({
  onDetected,
  onClose,
}: {
  onDetected: (pay: ScannedPay) => void;
  onClose: () => void;
}) {
  const videoRef = useRef<HTMLVideoElement | null>(null);
  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [attempt, setAttempt] = useState(0);

  useEffect(() => {
    let stream: MediaStream | null = null;
    let raf = 0;
    let active = true;

    function messageFor(err: unknown): string {
      const name = (err as DOMException | undefined)?.name ?? '';
      switch (name) {
        case 'NotAllowedError':
        case 'SecurityError':
          return "Autorisation refusée. Autorise l'accès à la caméra pour ce site dans les réglages du navigateur, puis réessaie.";
        case 'NotFoundError':
        case 'OverconstrainedError':
          return "Aucune caméra utilisable n'a été trouvée sur cet appareil.";
        case 'NotReadableError':
          return 'La caméra est déjà utilisée par une autre application. Ferme-la et réessaie.';
        default:
          return "Impossible d'accéder à la caméra. Vérifie l'autorisation et que le site est bien en HTTPS.";
      }
    }

    async function start() {
      setError(null);
      if (!navigator.mediaDevices?.getUserMedia) {
        setError(
          "La caméra n'est pas disponible ici (un contexte sécurisé HTTPS est requis).",
        );
        return;
      }
      try {
        try {
          // Caméra arrière de préférence…
          stream = await navigator.mediaDevices.getUserMedia({
            video: { facingMode: 'environment' },
            audio: false,
          });
        } catch (inner) {
          // …mais certaines caméras refusent cette contrainte : on réessaie
          // avec n'importe quelle caméra (sauf si l'autorisation est refusée).
          const name = (inner as DOMException).name;
          if (name === 'NotAllowedError' || name === 'SecurityError') {
            throw inner;
          }
          stream = await navigator.mediaDevices.getUserMedia({ video: true, audio: false });
        }
        const video = videoRef.current;
        if (!video) return;
        video.srcObject = stream;
        video.setAttribute('playsinline', 'true');
        await video.play();
        tick();
      } catch (err) {
        if (active) {
          setError(messageFor(err));
        }
      }
    }

    function tick() {
      if (!active) return;
      const video = videoRef.current;
      const canvas = canvasRef.current;
      if (video && canvas && video.readyState === video.HAVE_ENOUGH_DATA) {
        const w = video.videoWidth;
        const h = video.videoHeight;
        canvas.width = w;
        canvas.height = h;
        const ctx = canvas.getContext('2d', { willReadFrequently: true });
        if (ctx && w && h) {
          ctx.drawImage(video, 0, 0, w, h);
          const data = ctx.getImageData(0, 0, w, h);
          const found = jsQR(data.data, w, h, { inversionAttempts: 'dontInvert' });
          if (found) {
            const pay = parsePay(found.data);
            if (pay) {
              cleanup();
              onDetected(pay);
              return;
            }
          }
        }
      }
      raf = requestAnimationFrame(tick);
    }

    function cleanup() {
      active = false;
      cancelAnimationFrame(raf);
      if (stream) stream.getTracks().forEach((t) => t.stop());
    }

    start();
    return cleanup;
  }, [onDetected, attempt]);

  return (
    <div className="scanner-overlay" onClick={onClose}>
      <div className="scanner-dialog" onClick={(e) => e.stopPropagation()}>
        <div className="scanner-head">
          <h3>Scanner un QR code</h3>
          <button type="button" className="legal-close" aria-label="Fermer" onClick={onClose}>
            ✕
          </button>
        </div>

        {error ? (
          <div className="scanner-error">
            <p className="error">{error}</p>
            <button type="button" className="ghost" onClick={() => setAttempt((a) => a + 1)}>
              Réessayer
            </button>
          </div>
        ) : (
          <>
            <div className="scanner-frame">
              <video ref={videoRef} muted playsInline />
              <div className="scanner-reticle" />
            </div>
            <p className="muted scanner-hint">
              Vise le QR code de paiement. Le virement se pré-remplira automatiquement.
            </p>
          </>
        )}
        <canvas ref={canvasRef} style={{ display: 'none' }} />
      </div>
    </div>
  );
}
