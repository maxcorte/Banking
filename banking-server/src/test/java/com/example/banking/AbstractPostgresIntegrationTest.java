package com.example.banking;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base des tests d'intégration : démarre un PostgreSQL réel et jetable dans un
 * conteneur Docker, identique à la production. Flyway applique les migrations
 * (dont la création du compte "monde extérieur") au démarrage du contexte.
 *
 * @ServiceConnection branche automatiquement la datasource Spring sur ce
 * conteneur : aucune URL ni mot de passe à configurer.
 *
 * Pré-requis pour lancer ces tests : Docker doit être démarré sur la machine.
 */
@SpringBootTest
@Testcontainers
abstract class AbstractPostgresIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:17");
}
