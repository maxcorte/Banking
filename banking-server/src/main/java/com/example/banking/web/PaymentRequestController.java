package com.example.banking.web;

import com.example.banking.domain.User;
import com.example.banking.security.CurrentUser;
import com.example.banking.service.PaymentRequestService;
import com.example.banking.web.dto.AcceptPaymentRequest;
import com.example.banking.web.dto.CreatePaymentRequestRequest;
import com.example.banking.web.dto.PaymentRequestResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/payment-requests")
public class PaymentRequestController {

    private final PaymentRequestService service;
    private final CurrentUser currentUser;

    public PaymentRequestController(PaymentRequestService service, CurrentUser currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void create(@RequestBody CreatePaymentRequestRequest req) {
        User me = currentUser.require();
        service.create(me, req.toAccountId(), req.payerAccountNumber(), req.amountMinor(), req.description());
    }

    @GetMapping("/incoming")
    public List<PaymentRequestResponse> incoming() {
        return service.listIncoming(currentUser.require().getId());
    }

    @GetMapping("/outgoing")
    public List<PaymentRequestResponse> outgoing() {
        return service.listOutgoing(currentUser.require().getId());
    }

    @PostMapping("/{id}/accept")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void accept(@PathVariable UUID id, @RequestBody AcceptPaymentRequest req) {
        service.accept(id, currentUser.require(), req.fromAccountId());
    }

    @PostMapping("/{id}/refuse")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void refuse(@PathVariable UUID id) {
        service.refuse(id, currentUser.require());
    }

    @PostMapping("/{id}/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable UUID id) {
        service.cancel(id, currentUser.require());
    }
}
