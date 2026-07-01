package com.example.banking.web;

import com.example.banking.domain.User;
import com.example.banking.security.AuthCookies;
import com.example.banking.security.CurrentUser;
import com.example.banking.service.AuthResult;
import com.example.banking.service.AuthService;
import com.example.banking.service.WebAuthnService;
import com.example.banking.repository.WebAuthnCredentialRepository;
import com.example.banking.web.dto.PasskeyResponse;
import com.example.banking.web.dto.UserInfoResponse;
import com.example.banking.web.dto.WebAuthnLoginFinishRequest;
import com.example.banking.web.dto.WebAuthnRegisterFinishRequest;
import com.example.banking.web.dto.WebAuthnStartResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/webauthn")
public class WebAuthnController {

    private final WebAuthnService service;
    private final AuthService authService;
    private final AuthCookies cookies;
    private final CurrentUser currentUser;
    private final WebAuthnCredentialRepository creds;

    public WebAuthnController(WebAuthnService service,
                             AuthService authService,
                             AuthCookies cookies,
                             CurrentUser currentUser,
                             WebAuthnCredentialRepository creds) {
        this.service = service;
        this.authService = authService;
        this.cookies = cookies;
        this.currentUser = currentUser;
        this.creds = creds;
    }

    // ---- Enregistrement d'une passkey (utilisateur connecte) ----

    @PostMapping("/register/start")
    public WebAuthnStartResponse registerStart() {
        return service.startRegistration(currentUser.require());
    }

    @PostMapping("/register/finish")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void registerFinish(@RequestBody WebAuthnRegisterFinishRequest req) {
        service.finishRegistration(currentUser.require(), req.flowId(), req.credential(), req.label());
    }

    // ---- Connexion par passkey (public) ----

    @PostMapping("/login/start")
    public WebAuthnStartResponse loginStart() {
        return service.startLogin();
    }

    @PostMapping("/login/finish")
    public UserInfoResponse loginFinish(@RequestBody WebAuthnLoginFinishRequest req,
                                        HttpServletResponse response) {
        String username = service.finishLogin(req.flowId(), req.credential());
        AuthResult result = authService.issueSessionByUsername(username);
        cookies.write(response, result.accessToken(), result.refreshToken());
        return new UserInfoResponse(result.username(), result.role());
    }

    // ---- Gestion des passkeys (utilisateur connecte) ----

    @GetMapping("/credentials")
    public List<PasskeyResponse> list() {
        User me = currentUser.require();
        return creds.findByUserId(me.getId()).stream()
                .map(c -> new PasskeyResponse(c.getId().toString(), c.getLabel(), c.getCreatedAt().toString()))
                .toList();
    }

    @DeleteMapping("/credentials/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        User me = currentUser.require();
        service.deleteCredential(id, me.getId());
    }
}
