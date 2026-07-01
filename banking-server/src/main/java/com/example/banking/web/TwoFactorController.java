package com.example.banking.web;

import com.example.banking.domain.User;
import com.example.banking.security.CurrentUser;
import com.example.banking.service.TwoFactorService;
import com.example.banking.web.dto.TotpCodeRequest;
import com.example.banking.web.dto.TotpSetupResponse;
import com.example.banking.web.dto.TwoFactorStatusResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/2fa")
public class TwoFactorController {

    private final TwoFactorService service;
    private final CurrentUser currentUser;

    public TwoFactorController(TwoFactorService service, CurrentUser currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @GetMapping("/status")
    public TwoFactorStatusResponse status() {
        return new TwoFactorStatusResponse(service.isEnabled(currentUser.require().getId()));
    }

    @PostMapping("/setup")
    public TotpSetupResponse setup() {
        return service.setup(currentUser.require());
    }

    @PostMapping("/enable")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void enable(@RequestBody TotpCodeRequest req) {
        service.enable(currentUser.require(), req.code());
    }

    @PostMapping("/disable")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void disable(@RequestBody TotpCodeRequest req) {
        service.disable(currentUser.require(), req.code());
    }
}
