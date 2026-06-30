import { api } from './api';

/** Convertit une cle VAPID base64url en Uint8Array (pour applicationServerKey). */
function urlBase64ToUint8Array(base64String: string): Uint8Array {
  const padding = '='.repeat((4 - (base64String.length % 4)) % 4);
  const base64 = (base64String + padding).replace(/-/g, '+').replace(/_/g, '/');
  const raw = atob(base64);
  const out = new Uint8Array(raw.length);
  for (let i = 0; i < raw.length; i += 1) {
    out[i] = raw.charCodeAt(i);
  }
  return out;
}

export type PushState = 'unsupported' | 'denied' | 'enabled' | 'disabled';

export function pushSupported(): boolean {
  return (
    'serviceWorker' in navigator && 'PushManager' in window && 'Notification' in window
  );
}

export async function getPushState(): Promise<PushState> {
  if (!pushSupported()) return 'unsupported';
  if (Notification.permission === 'denied') return 'denied';
  try {
    const reg = await navigator.serviceWorker.ready;
    const sub = await reg.pushManager.getSubscription();
    return sub ? 'enabled' : 'disabled';
  } catch {
    return 'disabled';
  }
}

/** Demande l'autorisation, s'abonne, et enregistre l'abonnement cote serveur. */
export async function enablePush(): Promise<PushState> {
  if (!pushSupported()) return 'unsupported';

  const permission = await Notification.requestPermission();
  if (permission === 'denied') return 'denied';
  if (permission !== 'granted') return 'disabled'; // refus/fermeture du prompt

  const { publicKey, enabled } = await api.pushPublicKey();
  if (!enabled || !publicKey) {
    throw new Error("Le serveur n'a pas encore de clés push configurées.");
  }

  const reg = await navigator.serviceWorker.ready;
  let sub = await reg.pushManager.getSubscription();
  if (!sub) {
    try {
      sub = await reg.pushManager.subscribe({
        userVisibleOnly: true,
        applicationServerKey: urlBase64ToUint8Array(publicKey) as BufferSource,
      });
    } catch (e) {
      const msg = e instanceof Error ? e.message : String(e);
      throw new Error("Abonnement aux notifications impossible sur cet appareil. " + msg);
    }
  }
  await api.subscribePush(sub.toJSON());
  return 'enabled';
}

/** Desabonne le navigateur et supprime l'abonnement cote serveur. */
export async function disablePush(): Promise<PushState> {
  if (!pushSupported()) return 'unsupported';
  try {
    const reg = await navigator.serviceWorker.ready;
    const sub = await reg.pushManager.getSubscription();
    if (sub) {
      await api.unsubscribePush(sub.endpoint);
      await sub.unsubscribe();
    }
  } catch {
    /* ignore */
  }
  return 'disabled';
}
