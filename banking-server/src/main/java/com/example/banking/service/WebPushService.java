package com.example.banking.service;

import com.example.banking.domain.PushSubscription;
import nl.martijndwars.webpush.Encoding;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Security;
import java.util.List;
import java.util.UUID;

/**
 * Envoi des notifications push web (protocole Web Push + VAPID). Si les cles
 * VAPID ne sont pas configurees, le service est simplement desactive : l'app
 * fonctionne normalement, sans push.
 *
 * Les appels reseau (HTTP vers le service de push du navigateur) se font HORS
 * de toute transaction base de donnees : on ne tient jamais une connexion JDBC
 * pendant un appel reseau lent.
 */
@Service
public class WebPushService {

    private static final Logger log = LoggerFactory.getLogger(WebPushService.class);

    private final PushSubscriptionService subscriptions;
    private final String publicKey;
    private PushService pushService;
    private final boolean enabled;

    public WebPushService(PushSubscriptionService subscriptions,
                          @Value("${app.vapid.public-key:}") String publicKey,
                          @Value("${app.vapid.private-key:}") String privateKey,
                          @Value("${app.vapid.subject:mailto:noreply@maximedelcorte.cloud}") String subject) {
        this.subscriptions = subscriptions;
        this.publicKey = publicKey;
        boolean ok = false;
        if (publicKey != null && !publicKey.isBlank()
                && privateKey != null && !privateKey.isBlank()) {
            try {
                if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
                    Security.addProvider(new BouncyCastleProvider());
                }
                this.pushService = new PushService(publicKey, privateKey, subject);
                ok = true;
                log.info("Web Push active (VAPID configure).");
            } catch (Exception e) {
                log.warn("Web Push desactive : cles VAPID invalides ({}).", e.getMessage());
            }
        } else {
            log.info("Web Push desactive : cles VAPID absentes.");
        }
        this.enabled = ok;
    }

    public boolean isEnabled() {
        return enabled;
    }

    /** Cle publique VAPID exposee au frontend pour l'abonnement. */
    public String getPublicKey() {
        return publicKey;
    }

    /** Envoie une notification a tous les navigateurs abonnes de l'utilisateur. */
    public void sendToUser(UUID userId, String title, String body) {
        if (!enabled) {
            return;
        }
        List<PushSubscription> subs = subscriptions.listByUser(userId);
        String payload = "{\"title\":\"" + escape(title) + "\",\"body\":\"" + escape(body) + "\"}";

        for (PushSubscription s : subs) {
            try {
                Notification notification = new Notification(
                        s.getEndpoint(),
                        s.getP256dh(),
                        s.getAuth(),
                        payload.getBytes(StandardCharsets.UTF_8));
                var response = pushService.send(notification, Encoding.AES128GCM);
                int status = response.getStatusLine().getStatusCode();
                // 404 / 410 : l'abonnement n'existe plus cote navigateur -> on le purge.
                if (status == 404 || status == 410) {
                    subscriptions.delete(s.getEndpoint());
                }
            } catch (Exception e) {
                log.debug("Echec d'envoi push (endpoint purge ou indisponible) : {}", e.getMessage());
            }
        }
    }

    private static String escape(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
