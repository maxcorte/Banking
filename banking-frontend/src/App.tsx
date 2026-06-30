import { useState } from 'react';
import { useAuth } from './auth';
import { LoginForm } from './components/LoginForm';
import { Dashboard } from './components/Dashboard';
import { SiteFooter } from './components/SiteFooter';
import { LegalPages } from './components/LegalPages';
import { ResetPassword } from './components/ResetPassword';

export function App() {
  const { user, loading } = useAuth();
  const [legalTab, setLegalTab] = useState<'mentions' | 'confidentialite' | null>(null);

  // Lien reçu par e-mail : https://…/?reset=JETON → écran de réinitialisation,
  // prioritaire sur tout le reste (pas besoin d'être connecté).
  const resetToken = new URLSearchParams(window.location.search).get('reset');

  function renderMain() {
    if (resetToken) return <ResetPassword token={resetToken} />;
    if (loading) return <div className="app-loading">Chargement…</div>;
    return user ? <Dashboard /> : <LoginForm />;
  }

  return (
    <div className="app-shell">
      <div className="app-main">{renderMain()}</div>
      <SiteFooter onOpenLegal={(tab) => setLegalTab(tab)} />
      <LegalPages
        key={legalTab ?? 'closed'}
        open={legalTab !== null}
        initialTab={legalTab ?? 'mentions'}
        onClose={() => setLegalTab(null)}
      />
    </div>
  );
}
