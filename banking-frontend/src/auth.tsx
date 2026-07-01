import { createContext, useContext, useEffect, useState, type ReactNode } from 'react';
import { get as webauthnGet } from '@github/webauthn-json';
import { api } from './api';
import type { UserInfo } from './types';

interface AuthContextValue {
  user: UserInfo | null;
  loading: boolean;
  isAdmin: boolean;
  login: (username: string, password: string) => Promise<void>;
  loginWithPasskey: () => Promise<void>;
  register: (username: string, email: string, password: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserInfo | null>(null);
  const [loading, setLoading] = useState(true);

  // Au démarrage, on demande au serveur « qui suis-je ? ». Si un cookie de
  // session valide existe (ou peut être rafraîchi), on récupère l'identité ;
  // sinon on reste déconnecté. Le jeton n'est plus lisible en JS.
  useEffect(() => {
    let active = true;
    api
      .meOrNull()
      .then((me) => {
        if (active) setUser(me);
      })
      .catch(() => {
        if (active) setUser(null);
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, []);

  // Si une session expire en cours d'usage (refresh impossible), on repasse
  // à l'écran de connexion.
  useEffect(() => {
    function onExpired() {
      setUser(null);
    }
    window.addEventListener('auth:expired', onExpired);
    return () => window.removeEventListener('auth:expired', onExpired);
  }, []);

  async function login(username: string, password: string) {
    const me = await api.login(username, password);
    setUser(me);
  }

  async function loginWithPasskey() {
    const { flowId, optionsJson } = await api.webauthnLoginStart();
    const options = JSON.parse(optionsJson);
    const credential = await webauthnGet(options);
    const me = await api.webauthnLoginFinish(flowId, JSON.stringify(credential));
    setUser(me);
  }

  async function register(username: string, email: string, password: string) {
    await api.register(username, email, password);
    await login(username, password);
  }

  function logout() {
    api.logout().catch(() => {});
    setUser(null);
  }

  return (
    <AuthContext.Provider
      value={{
        user,
        loading,
        isAdmin: user?.role === 'ADMIN',
        login,
        loginWithPasskey,
        register,
        logout,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error('useAuth doit etre utilise dans un AuthProvider');
  }
  return ctx;
}
