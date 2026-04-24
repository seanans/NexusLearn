package com.nexuslearn.api.services;

import com.nexuslearn.api.dtos.JwtResponse;
import com.nexuslearn.api.dtos.LoginRequest;
import com.nexuslearn.api.dtos.RegisterRequest;
import com.nexuslearn.api.dtos.TokenRefreshResponse;
import com.nexuslearn.api.exceptions.AppException;
import com.nexuslearn.api.models.RefreshToken;
import com.nexuslearn.api.models.User;
import com.nexuslearn.api.repositories.UserRepository;
import com.nexuslearn.api.security.CustomUserDetails;
import com.nexuslearn.api.security.JwtTokenProvider;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public String registerUser(RegisterRequest signUpRequest) {
        if (userRepository.findByEmail(signUpRequest.getEmail()).isPresent()) {
            throw new AppException("Error: Email is already in use!", HttpStatus.CONFLICT);
        }

        User user = User.builder().email(signUpRequest.getEmail()).passwordHash(passwordEncoder.encode(signUpRequest.getPassword())).firstName(signUpRequest.getFirstName()).lastName(signUpRequest.getLastName()).build();

        userRepository.save(user);
        return "User registered successfully!";
    }

    @Transactional
    public JwtResponse authenticateUser(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        if (authentication.getPrincipal() instanceof CustomUserDetails userDetails) {

            String jwt = jwtTokenProvider.generateJwtToken(userDetails.getUsername());

            refreshTokenService.deleteByUserId(userDetails.user().getId());
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(userDetails.user());

            return JwtResponse.builder().accessToken(jwt).refreshToken(refreshToken.getToken()).id(userDetails.user().getId()).email(userDetails.getUsername()).firstName(userDetails.user().getFirstName()).lastName(userDetails.user().getLastName()).build();
        }

        throw new AppException("Authentication principal is invalid", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public TokenRefreshResponse refreshToken(String requestRefreshToken) {
        return refreshTokenService.findByToken(requestRefreshToken).map(refreshTokenService::verifyExpiration).map(RefreshToken::getUser).map(user -> {
            String newAccessToken = jwtTokenProvider.generateJwtToken(user.getEmail());
            return TokenRefreshResponse.builder().accessToken(newAccessToken).refreshToken(requestRefreshToken).build();
        }).orElseThrow(() -> new AppException("Refresh token is not in database!", HttpStatus.FORBIDDEN));
    }

    public void logoutUser(String refreshToken) {
        refreshTokenService.findByToken(refreshToken).ifPresent(token -> refreshTokenService.deleteByUserId(token.getUser().getId()));
    }
}
