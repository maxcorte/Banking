/**
 * Pied de page discret, présent sur toutes les vues (connecté ou non).
 * Il ouvre les pages légales et rappelle la nature pédagogique du projet.
 */
export function SiteFooter({
  onOpenLegal,
}: {
  onOpenLegal: (tab: 'mentions' | 'confidentialite') => void;
}) {
  return (
    <footer className="site-footer">
      <span className="site-footer-note">
        Projet de démonstration — données fictives
      </span>
      <nav className="site-footer-links">
        <button type="button" className="link" onClick={() => onOpenLegal('mentions')}>
          Mentions légales
        </button>
        <span aria-hidden="true">·</span>
        <button
          type="button"
          className="link"
          onClick={() => onOpenLegal('confidentialite')}
        >
          Confidentialité
        </button>
      </nav>
    </footer>
  );
}
