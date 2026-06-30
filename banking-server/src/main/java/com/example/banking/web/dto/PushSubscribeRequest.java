package com.example.banking.web.dto;

/** Charge utile envoyee par le navigateur lors de l'abonnement push. */
public record PushSubscribeRequest(String endpoint, Keys keys) {
    public record Keys(String p256dh, String auth) {}
}
