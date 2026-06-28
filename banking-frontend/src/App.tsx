import { useAuth } from './auth';
import { LoginForm } from './components/LoginForm';
import { Dashboard } from './components/Dashboard';

export function App() {
  const { user, loading } = useAuth();
  if (loading) {
    return <div className="app-loading">Chargement…</div>;
  }
  return user ? <Dashboard /> : <LoginForm />;
}
