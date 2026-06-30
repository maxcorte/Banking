import { useState } from 'react';
import { useAuth } from './auth';
import { LoginForm } from './components/LoginForm';
import { Dashboard } from './components/Dashboard';
import { SiteFooter } from './components/SiteFooter';
import { LegalPages } from './components/LegalPages';

export function App() {
  const { user, loading } = useAuth();
  const [legalTab, setLegalTab] = useState<'mentions' | 'confidentialite' | null>(null);

  if (loading) {
    return <div className="app-loading">Chargement…</div>;
  }

  return (
    <div className="app-shell">
      <div className="app-main">{user ? <Dashboard /> : <LoginForm />}</div>
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
