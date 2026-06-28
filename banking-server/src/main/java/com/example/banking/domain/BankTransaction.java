package com.example.banking.domain;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Une opération financière complète (ex. un virement). Regroupe les
 * écritures (postings) qui, ensemble, s'équilibrent à zéro.
 * Nommée "BankTransaction" pour ne pas heurter @jakarta.transaction.Transactional.
 */
@Entity
@Table(name = "transactions")
public class BankTransaction {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, updatable = false)
    private String reference;

    @Column(name = "idempotency_key", unique = true, updatable = false)
    private String idempotencyKey;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(length = 40)
    private TransactionCategory category = TransactionCategory.AUTRES;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Posting> postings = new ArrayList<>();

    protected BankTransaction() {
        // requis par JPA
    }

    public BankTransaction(UUID id, String reference, String idempotencyKey, String description) {
        this.id = id;
        this.reference = reference;
        this.idempotencyKey = idempotencyKey;
        this.description = description;
    }

    public void addPosting(Posting posting) {
        posting.setTransaction(this);
        this.postings.add(posting);
    }

    // --- getters ---
    public UUID getId() { return id; }
    public String getReference() { return reference; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getDescription() { return description; }

    public TransactionCategory getCategory() { return category; }

    public void setCategory(TransactionCategory category) { this.category = category; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public List<Posting> getPostings() { return postings; }
}
