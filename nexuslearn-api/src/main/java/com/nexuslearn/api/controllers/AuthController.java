package com.nexuslearn.api.controllers;

import com.nexuslearn.api.dtos.JwtResponse;
import com.nexuslearn.api.dtos.LoginRequest;
import com.nexuslearn.api.dtos.RegisterRequest;
import com.nexuslearn.api.dtos.TokenRefreshRequest;
import com.nexuslearn.api.models.RefreshToken;
import com.nexuslearn.api.security.JwtTokenProvider;
import com.nexuslearn.api.services.AuthService;
import com.nexuslearn.api.services.RefreshTokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest signUpRequest) {
        try {
            String result = authService.registerUser(signUpRequest);
            return ResponseEntity.ok(Map.of("message", result));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<JwtResponse> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        JwtResponse response = authService.authenticateUser(loginRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@Valid @RequestBody TokenRefreshRequest request) {
        String requestRefreshToken = request.getRefreshToken();

        return refreshTokenService.findByToken(requestRefreshToken)
                .map(refreshTokenService::verifyExpiration)
                .map(RefreshToken::getUser)
                .map(user -> {
                    String token = jwtTokenProvider.generateJwtToken(user.getEmail());
                    return ResponseEntity.ok(Map.of(
                            "accessToken", token,
                            "refreshToken", requestRefreshToken,
                            "tokenType", "Bearer"
                    ));
                })
                .orElseThrow(() -> new RuntimeException("Refresh token is not in database!"));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser(@Valid @RequestBody TokenRefreshRequest request) {
        authService.logoutUser(request.getRefreshToken());
        return ResponseEntity.ok(Map.of("message", "Log out successful!"));
    }
}
