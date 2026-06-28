package com.example.banking;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base des tests d'intégration : un PostgreSQL réel et jetable (identique à la
 * prod), démarré UNE SEULE FOIS pour toute la suite de tests (pattern singleton).
 *
 * On ne ferme jamais le conteneur : Ryuk (Testcontainers) le nettoie à la sortie
 * de la JVM. Cela évite que chaque classe redémarre la base sur un nouveau port
 * et casse le contexte Spring mis en cache.
 *
 * Pré-requis : un démon Docker disponible (le cas sur les runners GitHub Actions).
 */
@SpringBootTest
abstract class AbstractPostgresIntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:17");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
