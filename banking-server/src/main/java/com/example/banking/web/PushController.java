package com.example.banking.web;

import com.example.banking.security.CurrentUser;
import com.example.banking.service.PushSubscriptionService;
import com.example.banking.service.WebPushService;
import com.example.banking.web.dto.PushSubscribeRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/push")
public class PushController {

    private final WebPushService webPush;
    private final PushSubscriptionService subscriptions;
    private final CurrentUser currentUser;

    public PushController(WebPushService webPush,
                          PushSubscriptionService subscriptions,
                          CurrentUser currentUser) {
        this.webPush = webPush;
        this.subscriptions = subscriptions;
        this.currentUser = currentUser;
    }

    /** Cle publique VAPID (et disponibilite du push) pour le frontend. */
    @GetMapping("/public-key")
    public PublicKey publicKey() {
        return new PublicKey(webPush.isEnabled() ? webPush.getPublicKey() : null, webPush.isEnabled());
    }

    @PostMapping("/subscribe")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void subscribe(@RequestBody PushSubscribeRequest req) {
        UUID userId = currentUser.require().getId();
        if (req == null || req.endpoint() == null || req.keys() == null) {
            return;
        }
        subscriptions.upsert(userId, req.endpoint(), req.keys().p256dh(), req.keys().auth());
    }

    @PostMapping("/unsubscribe")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unsubscribe(@RequestBody EndpointRequest req) {
        currentUser.require(); // exige une session valide
        if (req != null && req.endpoint() != null) {
            subscriptions.delete(req.endpoint());
        }
    }

    public record PublicKey(String publicKey, boolean enabled) {}
    public record EndpointRequest(String endpoint) {}
}
