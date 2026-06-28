package com.example.banking.web;

import com.example.banking.domain.User;
import com.example.banking.exception.BankingException;
import com.example.banking.security.AuthCookies;
import com.example.banking.security.CurrentUser;
import com.example.banking.service.AuthResult;
import com.example.banking.service.AuthService;
import com.example.banking.web.dto.LoginRequest;
import com.example.banking.web.dto.RegisterRequest;
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
    private final AuthCookies cookies;
    private final CurrentUser currentUser;

    public AuthController(AuthService authService, AuthCookies cookies, CurrentUser currentUser) {
        this.authService = authService;
        this.cookies = cookies;
        this.currentUser = currentUser;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public void register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request.username(), request.password());
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

    /** Identite de l'utilisateur connecte (le frontend ne lit plus le jeton). */
    @GetMapping("/me")
    public UserInfoResponse me() {
        User me = currentUser.require();
        return new UserInfoResponse(me.getUsername(), me.getRole().name());
    }
}
