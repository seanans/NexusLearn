package com.nexuslearn.api.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class JwtResponse {
    private String accessToken;
    private String refreshToken;
    private UUID id;
    private String email;
    private String firstName;
    private String lastName;
    @Builder.Default
    private String tokenType = "Bearer";
}