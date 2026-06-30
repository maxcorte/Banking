package com.example.banking.repository;

import com.example.banking.domain.AuditEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface AuditEntryRepository extends JpaRepository<AuditEntry, UUID> {

    List<AuditEntry> findAllByOrderByAtDesc(Pageable pageable);

    /** Recherche paginee : si q est null, renvoie tout (trie par date desc). */
    @Query("""
        select a from AuditEntry a
        where :q is null
           or lower(a.actor)  like lower(concat('%', :q, '%'))
           or lower(a.action) like lower(concat('%', :q, '%'))
           or lower(a.detail) like lower(concat('%', :q, '%'))
        order by a.at desc
        """)
    Page<AuditEntry> search(@Param("q") String q, Pageable pageable);
}
