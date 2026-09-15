package com.energy.detection.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponseDTO {

    private boolean authenticated;
    private String token; // JWT a ser enviado em "Authorization: Bearer {token}" nos pedidos seguintes
    private String username;
    private String name;
    private String role; // "ROLE_ADMIN" ou "ROLE_OPERATOR"
    private List<String> authorities;
    private String message;
}
