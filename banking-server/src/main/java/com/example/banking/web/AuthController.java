package com.example.banking.web;

import com.example.banking.domain.User;
import com.example.banking.exception.BankingException;
import com.example.banking.security.AuthCookies;
import com.example.banking.security.CurrentUser;
import com.example.banking.service.AuthResult;
import com.example.banking.service.AuthService;
import com.example.banking.service.PasswordResetService;
import com.example.banking.web.dto.LoginRequest;
import com.example.banking.web.dto.ForgotPasswordRequest;
import com.example.banking.web.dto.RegisterRequest;
import com.example.banking.web.dto.ResetPasswordRequest;
import com.example.banking.web.dto.UserInfoResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;
    private final AuthCookies cookies;
    private final CurrentUser currentUser;

    public AuthController(AuthService authService,
                          PasswordResetService passwordResetService,
                          AuthCookies cookies,
                          CurrentUser currentUser) {
        this.authService = authService;
        this.passwordResetService = passwordResetService;
        this.cookies = cookies;
        this.currentUser = currentUser;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public void register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request.username(), request.email(), request.password());
    }

    @PostMapping("/login")
    public UserInfoResponse login(@Valid @RequestBody LoginRequest request,
                                  HttpServletResponse response) {
        AuthResult result = authService.login(request.username(), request.password());
        cookies.write(response, result.accessToken(), result.refreshToken());
        return new UserInfoResponse(result.username(), result.role());
    }

    /** Renouvelle les jetons a partir du cookie refresh_token (rotation). */
    @PostMapping("/refresh")
    public UserInfoResponse refresh(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = cookies.read(request, AuthCookies.REFRESH)
                .orElseThrow(() -> new BankingException(
                        "INVALID_REFRESH", "Session expirée, veuillez vous reconnecter."));
        AuthResult result = authService.refresh(refreshToken);
        cookies.write(response, result.accessToken(), result.refreshToken());
        return new UserInfoResponse(result.username(), result.role());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        cookies.read(request, AuthCookies.REFRESH).ifPresent(authService::logout);
        cookies.clear(response);
    }

    /** Demande de reinitialisation : envoie un lien par e-mail si le compte existe.
     *  Reponse identique dans tous les cas (on ne revele pas les comptes existants). */
    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.requestReset(request.email());
    }

    /** Applique un nouveau mot de passe a partir d'un jeton recu par e-mail. */
    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.reset(request.token(), request.password());
    }

    /** Identite de l'utilisateur connecte (le frontend ne lit plus le jeton). */
    @GetMapping("/me")
    public UserInfoResponse me() {
        User me = currentUser.require();
        return new UserInfoResponse(me.getUsername(), me.getRole().name());
    }
}
