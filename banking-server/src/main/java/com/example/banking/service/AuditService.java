package com.example.banking.service;

import com.example.banking.domain.AuditEntry;
import com.example.banking.repository.AuditEntryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Ecrit les entrees du journal d'audit.
 *
 * Chaque ecriture se fait dans une transaction SEPAREE (REQUIRES_NEW) : ainsi
 * une trace persiste meme si l'action metier echoue ensuite, et un probleme
 * d'audit ne fait jamais echouer l'action metier.
 */
@Service
public class AuditService {

    private final AuditEntryRepository repository;

    public AuditService(AuditEntryRepository repository) {
        this.repository = repository;
    }

    /** Enregistre une action de l'utilisateur courant (acteur deduit du contexte). */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String action, String detail) {
        save(currentActor(), action, detail);
    }

    /** Enregistre une action avec un acteur explicite (ex. tentative de connexion). */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordAs(String actor, String action, String detail) {
        save(actor, action, detail);
    }

    @Transactional(readOnly = true)
    public List<AuditEntry> recent(int limit) {
        int capped = Math.max(1, Math.min(limit, 500));
        return repository.findAllByOrderByAtDesc(PageRequest.of(0, capped));
    }

    /** Page du journal avec recherche optionnelle (acteur / action / detail). */
    @Transactional(readOnly = true)
    public Page<AuditEntry> page(String q, int page, int size) {
        int p = Math.max(0, page);
        int s = Math.min(Math.max(1, size), 100);
        Pageable pageable = PageRequest.of(p, s, Sort.by(Sort.Direction.DESC, "at"));
        String query = (q == null || q.isBlank()) ? null : q.trim();
        // Pas de recherche : on liste tout (trie). Sinon, requete LIKE.
        return query == null ? repository.findAll(pageable) : repository.search(query, pageable);
    }

    private void save(String actor, String action, String detail) {
        repository.save(new AuditEntry(
                UUID.randomUUID(), OffsetDateTime.now(), actor, action, detail));
    }

    private String currentActor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return "anonyme";
        }
        return auth.getName();
    }
}
