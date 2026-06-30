import type { Account, AuditPage, Beneficiary, NotificationItem, PaymentRequest, Transaction, TransactionLine, UserInfo } from './types';

const BASE = '/api';

// Routes d'auth qui ne doivent jamais declencher un refresh automatique.
const NO_REFRESH = ['/auth/login', '/auth/register', '/auth/refresh', '/auth/logout', '/auth/forgot-password', '/auth/reset-password'];

// Un seul refresh a la fois, partage entre les requetes concurrentes.
let refreshing: Promise<boolean> | null = null;

async function doRefresh(): Promise<boolean> {
  try {
    // Le refresh token voyage dans un cookie httpOnly : rien a envoyer ici.
    const res = await fetch(`${BASE}/auth/refresh`, {
      method: 'POST',
      credentials: 'include',
    });
    if (!res.ok) {
      throw new Error('refresh failed');
    }
    return true; // le serveur a repose des cookies a jour
  } catch {
    // Session terminee : on previent l'application.
    window.dispatchEvent(new Event('auth:expired'));
    return false;
  }
}

function refreshSession(): Promise<boolean> {
  if (!refreshing) {
    refreshing = doRefresh().finally(() => {
      refreshing = null;
    });
  }
  return refreshing;
}

async function request<T>(path: string, options: RequestInit = {}, retry = true): Promise<T> {
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(options.headers as Record<string, string> | undefined),
  };

  // credentials: 'include' => le navigateur joint les cookies httpOnly d'auth.
  const res = await fetch(BASE + path, { ...options, headers, credentials: 'include' });

  // Jeton d'acces expire (401) : on rafraichit en silence via le cookie refresh,
  // puis on rejoue la requete une seule fois. Jamais sur les routes d'auth.
  if (res.status === 401 && retry && !NO_REFRESH.includes(path)) {
    const ok = await refreshSession();
    if (ok) {
      return request<T>(path, options, false);
    }
  }

  if (!res.ok) {
    let message = `Erreur ${res.status}`;
    try {
      const body = await res.json();
      message = body.detail || body.title || message;
    } catch {
      // corps non-JSON : on garde le message par defaut
    }
    throw new Error(message);
  }

  if (res.status === 204) {
    return null as T;
  }
  const text = await res.text();
  return (text ? JSON.parse(text) : null) as T;
}

export const api = {
  register: (username: string, email: string, password: string) =>
    request<void>('/auth/register', {
      method: 'POST',
      body: JSON.stringify({ username, email, password }),
    }),

  forgotPassword: (email: string) =>
    request<void>('/auth/forgot-password', {
      method: 'POST',
      body: JSON.stringify({ email }),
    }),

  resetPassword: (token: string, password: string) =>
    request<void>('/auth/reset-password', {
      method: 'POST',
      body: JSON.stringify({ token, password }),
    }),

  login: (username: string, password: string) =>
    request<UserInfo>('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ username, password }),
    }),

  logout: () => request<void>('/auth/logout', { method: 'POST' }),

  me: () => request<UserInfo>('/auth/me'),

  listNotifications: () => request<NotificationItem[]>('/notifications'),
  unreadNotifications: () => request<{ count: number }>('/notifications/unread-count'),
  markNotificationsRead: () => request<void>('/notifications/read', { method: 'POST' }),

  listIncomingRequests: () => request<PaymentRequest[]>('/payment-requests/incoming'),
  listOutgoingRequests: () => request<PaymentRequest[]>('/payment-requests/outgoing'),
  createPaymentRequest: (
    toAccountId: string,
    payerUsername: string,
    amountMinor: number,
    description: string,
  ) =>
    request<void>('/payment-requests', {
      method: 'POST',
      body: JSON.stringify({ toAccountId, payerUsername, amountMinor, description }),
    }),
  acceptPaymentRequest: (id: string, fromAccountId: string) =>
    request<void>(`/payment-requests/${id}/accept`, {
      method: 'POST',
      body: JSON.stringify({ fromAccountId }),
    }),
  refusePaymentRequest: (id: string) =>
    request<void>(`/payment-requests/${id}/refuse`, { method: 'POST' }),
  cancelPaymentRequest: (id: string) =>
    request<void>(`/payment-requests/${id}/cancel`, { method: 'POST' }),

  pushPublicKey: () =>
    request<{ publicKey: string | null; enabled: boolean }>('/push/public-key'),
  subscribePush: (sub: PushSubscriptionJSON) =>
    request<void>('/push/subscribe', { method: 'POST', body: JSON.stringify(sub) }),
  unsubscribePush: (endpoint: string) =>
    request<void>('/push/unsubscribe', { method: 'POST', body: JSON.stringify({ endpoint }) }),

  // Restaure la session au chargement SANS declencher l'evenement global
  // 'auth:expired' : on sonde /me, et si l'acces est expire on tente un seul
  // refresh via le cookie, puis on reessaie. Evite toute collision avec une
  // connexion concurrente.
  meOrNull: async (): Promise<UserInfo | null> => {
    const meRes = await fetch(`${BASE}/auth/me`, { credentials: 'include' });
    if (meRes.ok) {
      return (await meRes.json()) as UserInfo;
    }
    if (meRes.status !== 401) {
      return null;
    }
    const refreshRes = await fetch(`${BASE}/auth/refresh`, {
      method: 'POST',
      credentials: 'include',
    });
    if (!refreshRes.ok) {
      return null;
    }
    const retry = await fetch(`${BASE}/auth/me`, { credentials: 'include' });
    return retry.ok ? ((await retry.json()) as UserInfo) : null;
  },

  listAccounts: () => request<Account[]>('/accounts'),

  getAccount: (id: string) => request<Account>(`/accounts/${id}`),

  createAccount: (ownerName: string, currency: string) =>
    request<Account>('/accounts', {
      method: 'POST',
      body: JSON.stringify({ ownerName, currency }),
    }),

  deleteAccount: (id: string) =>
    request<void>(`/accounts/${id}`, { method: 'DELETE' }),

  history: (id: string) => request<TransactionLine[]>(`/accounts/${id}/history`),

  deposit: (id: string, amountMinor: number, description: string) =>
    request<Transaction>(`/accounts/${id}/deposit`, {
      method: 'POST',
      body: JSON.stringify({ amountMinor, description }),
    }),

  transfer: (
    fromAccountId: string,
    toAccountNumber: string,
    amountMinor: number,
    description: string,
    category: string,
  ) =>
    request<Transaction>('/transfers', {
      method: 'POST',
      body: JSON.stringify({ fromAccountId, toAccountNumber, amountMinor, description, category }),
    }),

  listBeneficiaries: () => request<Beneficiary[]>('/beneficiaries'),

  addBeneficiary: (label: string, accountNumber: string) =>
    request<Beneficiary>('/beneficiaries', {
      method: 'POST',
      body: JSON.stringify({ label, accountNumber }),
    }),

  deleteBeneficiary: (id: string) =>
    request<void>(`/beneficiaries/${id}`, { method: 'DELETE' }),

  listAudit: (params: { q?: string; page?: number; size?: number } = {}) => {
    const sp = new URLSearchParams();
    if (params.q) sp.set('q', params.q);
    sp.set('page', String(params.page ?? 0));
    sp.set('size', String(params.size ?? 25));
    return request<AuditPage>(`/audit?${sp.toString()}`);
  },
};

export function formatEuros(minor: number): string {
  return new Intl.NumberFormat('fr-FR', {
    style: 'currency',
    currency: 'EUR',
  }).format(minor / 100);
}
