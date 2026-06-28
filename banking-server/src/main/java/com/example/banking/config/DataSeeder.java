package com.example.banking.config;

import com.example.banking.domain.Account;
import com.example.banking.domain.Role;
import com.example.banking.domain.User;
import com.example.banking.repository.AccountRepository;
import com.example.banking.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * S'execute au demarrage, APRES les migrations Flyway. Cree le compte
 * administrateur s'il n'existe pas, puis rattache a cet admin les comptes
 * clients qui n'ont pas encore de proprietaire (ex. Alice/Bob crees avant
 * l'autorisation). Idempotent : ne fait rien si tout est deja en place.
 */
@Component
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository users;
    private final AccountRepository accounts;
    private final PasswordEncoder passwordEncoder;
    private final String adminUsername;
    private final String adminPassword;

    public DataSeeder(UserRepository users,
                      AccountRepository accounts,
                      PasswordEncoder passwordEncoder,
                      @Value("${app.admin.username:admin}") String adminUsername,
                      @Value("${app.admin.password:admin1234}") String adminPassword) {
        this.users = users;
        this.accounts = accounts;
        this.passwordEncoder = passwordEncoder;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        User admin = users.findByUsername(adminUsername).orElseGet(() -> {
            User created = new User(
                    UUID.randomUUID(),
                    adminUsername,
                    passwordEncoder.encode(adminPassword),
                    Role.ADMIN);
            log.info("Compte administrateur '{}' cree.", adminUsername);
            return users.save(created);
        });

        // Le mot de passe admin configure (ADMIN_PASSWORD) fait foi : si le compte
        // existait deja avec un autre mot de passe, on le met a jour. Cela permet
        // d'appliquer un mot de passe fort au deploiement sans reinitialiser la base.
        if (!passwordEncoder.matches(adminPassword, admin.getPasswordHash())) {
            admin.setPasswordHash(passwordEncoder.encode(adminPassword));
            users.save(admin);
            log.info("Mot de passe administrateur synchronise avec la configuration.");
        }

        // Rattache les comptes clients orphelins a l'admin.
        int adopted = 0;
        for (Account account : accounts.findByAllowNegativeBalanceFalseOrderByCreatedAtAsc()) {
            if (account.getOwnerId() == null) {
                account.setOwnerId(admin.getId());
                accounts.save(account);
                adopted++;
            }
        }
        if (adopted > 0) {
            log.info("{} compte(s) sans proprietaire rattache(s) a l'admin.", adopted);
        }
    }
}
