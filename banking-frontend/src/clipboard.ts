/**
 * Copie un texte dans le presse-papier.
 * Utilise l'API moderne quand elle est disponible (HTTPS / localhost),
 * sinon bascule sur une methode de repli qui fonctionne aussi en http
 * (ex. acces via l'adresse IP locale sur le telephone).
 */
export async function copyText(text: string): Promise<boolean> {
  try {
    if (navigator.clipboard && window.isSecureContext) {
      await navigator.clipboard.writeText(text);
      return true;
    }
  } catch {
    // on tente le repli ci-dessous
  }

  try {
    const textarea = document.createElement('textarea');
    textarea.value = text;
    textarea.style.position = 'fixed';
    textarea.style.opacity = '0';
    document.body.appendChild(textarea);
    textarea.focus();
    textarea.select();
    const ok = document.execCommand('copy');
    document.body.removeChild(textarea);
    return ok;
  } catch {
    return false;
  }
}
