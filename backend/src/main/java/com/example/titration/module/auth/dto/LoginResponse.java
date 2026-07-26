package com.example.titration.module.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    private String token;
    private Long userId;
    private String username;
    private String realName;
    private String role;
    private String tokenType;
    private Long expiresIn;

    public static LoginResponse of(String token, Long userId, String username,
                                    String realName, String role, long expiresIn) {
        return LoginResponse.builder()
                .token(token)
                .userId(userId)
                .username(username)
                .realName(realName)
                .role(role)
                .tokenType("Bearer")
                .expiresIn(expiresIn)
                .build();
    }
}
