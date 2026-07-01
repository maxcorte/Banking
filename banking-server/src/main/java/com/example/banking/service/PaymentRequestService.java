package com.example.banking.service;

import com.example.banking.domain.Account;
import com.example.banking.domain.PaymentRequest;
import com.example.banking.domain.PaymentRequestStatus;
import com.example.banking.domain.TransactionCategory;
import com.example.banking.domain.User;
import com.example.banking.exception.AccountNotFoundException;
import com.example.banking.exception.ForbiddenException;
import com.example.banking.exception.InvalidTransferException;
import com.example.banking.repository.AccountRepository;
import com.example.banking.repository.PaymentRequestRepository;
import com.example.banking.repository.UserRepository;
import com.example.banking.web.dto.PaymentRequestResponse;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Demandes de remboursement entre utilisateurs. Une demande PENDING peut etre
 * acceptee (declenche un virement), refusee, ou annulee par son auteur.
 */
@Service
public class PaymentRequestService {

    private final PaymentRequestRepository requests;
    private final AccountRepository accounts;
    private final UserRepository users;
    private final TransferService transferService;
    private final ApplicationEventPublisher events;

    public PaymentRequestService(PaymentRequestRepository requests,
                                 AccountRepository accounts,
                                 UserRepository users,
                                 TransferService transferService,
                                 ApplicationEventPublisher events) {
        this.requests = requests;
        this.accounts = accounts;
        this.users = users;
        this.transferService = transferService;
        this.events = events;
    }

    /** L'utilisateur courant demande un remboursement au titulaire d'un IBAN. */
    @Transactional
    public PaymentRequest create(User requester, UUID toAccountId, String payerAccountNumber,
                                 long amountMinor, String description) {
        if (amountMinor <= 0) {
            throw new InvalidTransferException("Le montant doit être strictement positif.");
        }
        Account to = accounts.findById(toAccountId)
                .orElseThrow(() -> new AccountNotFoundException(toAccountId.toString()));
        if (to.getOwnerId() == null || !to.getOwnerId().equals(requester.getId())) {
            throw new ForbiddenException("Ce compte ne vous appartient pas.");
        }
        if (payerAccountNumber == null || payerAccountNumber.isBlank()) {
            throw new InvalidTransferException("Destinataire requis.");
        }
        Account payerAccount = accounts.findByAccountNumber(payerAccountNumber.trim())
                .orElseThrow(() -> new InvalidTransferException("Ce contact / IBAN est introuvable."));
        if (payerAccount.getOwnerId() == null) {
            throw new InvalidTransferException("Ce compte ne peut pas recevoir de demande.");
        }
        User payer = users.findById(payerAccount.getOwnerId())
                .orElseThrow(() -> new InvalidTransferException("Titulaire introuvable."));
        if (payer.getId().equals(requester.getId())) {
            throw new InvalidTransferException("Vous ne pouvez pas vous adresser une demande à vous-même.");
        }

        String desc = (description != null && !description.isBlank()) ? description.trim() : null;
        PaymentRequest req = new PaymentRequest(
                UUID.randomUUID(), requester.getId(), toAccountId, payer.getId(),
                amountMinor, to.getCurrency(), desc);
        requests.save(req);

        String body = requester.getUsername() + " vous demande " + formatMinor(amountMinor)
                + (desc != null ? " (" + desc + ")" : "") + ".";
        events.publishEvent(new UserNotificationEvent(
                payer.getId(), "REQUEST_INCOMING", "Demande de paiement", body));

        return req;
    }

    /** Le destinataire accepte : execute le virement depuis l'un de ses comptes. */
    @Transactional
    public void accept(UUID requestId, User payer, UUID fromAccountId) {
        PaymentRequest req = requests.findById(requestId)
                .orElseThrow(() -> new InvalidTransferException("Demande introuvable."));
        if (!req.getPayerUserId().equals(payer.getId())) {
            throw new ForbiddenException("Cette demande ne vous est pas adressée.");
        }
        if (req.getStatus() != PaymentRequestStatus.PENDING) {
            throw new InvalidTransferException("Cette demande a déjà été traitée.");
        }
        Account from = accounts.findById(fromAccountId)
                .orElseThrow(() -> new AccountNotFoundException(fromAccountId.toString()));
        if (from.getOwnerId() == null || !from.getOwnerId().equals(payer.getId())) {
            throw new ForbiddenException("Ce compte ne vous appartient pas.");
        }

        // Le virement applique tous les controles (solde, devise, verrous) et
        // publie l'evenement qui notifiera le beneficiaire ("Paiement reçu").
        transferService.transfer(
                fromAccountId, req.getToAccountId(), req.getAmountMinor(),
                UUID.randomUUID().toString(),
                req.getDescription() != null ? req.getDescription() : "Remboursement",
                TransactionCategory.AUTRES);

        req.markAccepted(fromAccountId);
    }

    /** Le destinataire refuse la demande. */
    @Transactional
    public void refuse(UUID requestId, User payer) {
        PaymentRequest req = requests.findById(requestId)
                .orElseThrow(() -> new InvalidTransferException("Demande introuvable."));
        if (!req.getPayerUserId().equals(payer.getId())) {
            throw new ForbiddenException("Cette demande ne vous est pas adressée.");
        }
        if (req.getStatus() != PaymentRequestStatus.PENDING) {
            throw new InvalidTransferException("Cette demande a déjà été traitée.");
        }
        req.markRefused();

        User payerUser = users.findById(payer.getId()).orElse(null);
        String payerName = payerUser != null ? payerUser.getUsername() : "Le destinataire";
        String body = payerName + " a refusé votre demande de " + formatMinor(req.getAmountMinor()) + ".";
        events.publishEvent(new UserNotificationEvent(
                req.getRequesterUserId(), "REQUEST_REFUSED", "Demande refusée", body));
    }

    /** L'auteur annule sa propre demande encore en attente. */
    @Transactional
    public void cancel(UUID requestId, User requester) {
        PaymentRequest req = requests.findById(requestId)
                .orElseThrow(() -> new InvalidTransferException("Demande introuvable."));
        if (!req.getRequesterUserId().equals(requester.getId())) {
            throw new ForbiddenException("Cette demande ne vous appartient pas.");
        }
        if (req.getStatus() != PaymentRequestStatus.PENDING) {
            throw new InvalidTransferException("Cette demande a déjà été traitée.");
        }
        req.markCancelled();
    }

    @Transactional(readOnly = true)
    public List<PaymentRequestResponse> listIncoming(UUID payerUserId) {
        return requests.findByPayerUserIdOrderByCreatedAtDesc(payerUserId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<PaymentRequestResponse> listOutgoing(UUID requesterUserId) {
        return requests.findByRequesterUserIdOrderByCreatedAtDesc(requesterUserId)
                .stream().map(this::toResponse).toList();
    }

    private PaymentRequestResponse toResponse(PaymentRequest r) {
        String requesterName = users.findById(r.getRequesterUserId())
                .map(User::getUsername).orElse("—");
        String payerName = users.findById(r.getPayerUserId())
                .map(User::getUsername).orElse("—");
        String toAccountNumber = accounts.findById(r.getToAccountId())
                .map(Account::getAccountNumber).orElse(null);
        return new PaymentRequestResponse(
                r.getId(), requesterName, payerName, toAccountNumber,
                r.getAmountMinor(), r.getCurrency(), r.getDescription(),
                r.getStatus().name(), r.getCreatedAt(), r.getResolvedAt());
    }

    private static String formatMinor(long minor) {
        return String.format("%.2f €", minor / 100.0).replace('.', ',');
    }
}
