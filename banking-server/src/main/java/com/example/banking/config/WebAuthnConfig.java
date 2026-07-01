package com.example.banking.config;

import com.example.banking.security.webauthn.JpaCredentialRepository;
import com.yubico.webauthn.RelyingParty;
import com.yubico.webauthn.data.RelyingPartyIdentity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Configuration
public class WebAuthnConfig {

    /**
     * rp-id = domaine (doit couvrir l'origine du site). origins = origine(s) exacte(s)
     * autorisee(s). Valeurs par defaut = production ; surchargeables par propriete/env
     * (WEBAUTHN_RP_ID, WEBAUTHN_ORIGINS) pour le developpement local (localhost).
     */
    @Bean
    public RelyingParty relyingParty(
            JpaCredentialRepository credentialRepository,
            @Value("${webauthn.rp-id:maximedelcorte.cloud}") String rpId,
            @Value("${webauthn.rp-name:Ma Banque}") String rpName,
            @Value("${webauthn.origins:https://maximedelcorte.cloud}") String origins) {

        RelyingPartyIdentity identity = RelyingPartyIdentity.builder()
                .id(rpId)
                .name(rpName)
                .build();

        Set<String> originSet = Arrays.stream(origins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());

        return RelyingParty.builder()
                .identity(identity)
                .credentialRepository(credentialRepository)
                .origins(originSet)
                .build();
    }
}
