package com.example.banking.service;

import com.example.banking.domain.PushSubscription;
import com.example.banking.repository.PushSubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Operations base de donnees sur les abonnements push (transactions courtes). */
@Service
public class PushSubscriptionService {

    private final PushSubscriptionRepository subscriptions;

    public PushSubscriptionService(PushSubscriptionRepository subscriptions) {
        this.subscriptions = subscriptions;
    }

    /** Cree ou met a jour l'abonnement pour un endpoint donne. */
    @Transactional
    public void upsert(UUID userId, String endpoint, String p256dh, String auth) {
        subscriptions.findByEndpoint(endpoint).ifPresentOrElse(
                existing -> {
                    existing.setUserId(userId);
                    existing.setP256dh(p256dh);
                    existing.setAuth(auth);
                },
                () -> subscriptions.save(
                        new PushSubscription(UUID.randomUUID(), userId, endpoint, p256dh, auth)));
    }

    @Transactional
    public void delete(String endpoint) {
        subscriptions.deleteByEndpoint(endpoint);
    }

    @Transactional(readOnly = true)
    public List<PushSubscription> listByUser(UUID userId) {
        return subscriptions.findByUserId(userId);
    }
}
