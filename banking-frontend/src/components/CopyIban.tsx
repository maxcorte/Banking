import { useState, type MouseEvent } from 'react';
import { copyText } from '../clipboard';

export function CopyIban({ value }: { value: string }) {
  const [copied, setCopied] = useState(false);

  async function handleCopy(e: MouseEvent) {
    e.stopPropagation(); // ne pas declencher la selection du compte
    const ok = await copyText(value);
    if (ok) {
      setCopied(true);
      setTimeout(() => setCopied(false), 1500);
    }
  }

  return (
    <button type="button" className="copy-btn" onClick={handleCopy} title="Copier l'IBAN">
      {copied ? 'Copié ✓' : 'Copier'}
    </button>
  );
}
