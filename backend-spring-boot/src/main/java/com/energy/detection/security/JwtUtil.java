package com.energy.detection.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Componente responsável por gerar e validar tokens JWT.
 * Substitui o mecanismo anterior (SecurityContextHolder sem persistência),
 * que não sobrevivia entre pedidos HTTP distintos.
 */
@Component
public class JwtUtil {

    private final SecretKey secretKey;
    private final long expirationMs;

    public JwtUtil(
            @Value("${app.security.jwt.secret}") String secret,
            @Value("${app.security.jwt.expiration-ms}") long expirationMs
    ) {
        byte[] keyBytes = secret == null ? new byte[0] : secret.getBytes(StandardCharsets.UTF_8);
        if (secret == null || secret.isBlank() || keyBytes.length < 32) {
            throw new IllegalArgumentException("JWT_SECRET deve conter pelo menos 32 bytes UTF-8");
        }
        if (expirationMs <= 0 || expirationMs > 900000) {
            throw new IllegalArgumentException("JWT expiration deve estar entre 1 e 900000 ms");
        }
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMs = expirationMs;
    }

    public String gerarToken(UserDetails userDetails) {
        List<String> authorities = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        Date agora = new Date();
        Date expiracao = new Date(agora.getTime() + expirationMs);

        return Jwts.builder()
                .subject(userDetails.getUsername())
                .claim("authorities", authorities)
                .claim("authVersion", userDetails instanceof VersionedUserDetails versioned
                        ? versioned.getAuthVersion() : 0L)
                .issuedAt(agora)
                .expiration(expiracao)
                .signWith(secretKey)
                .compact();
    }

    public String extrairUsername(String token) {
        return parseClaims(token).getSubject();
    }

    @SuppressWarnings("unchecked")
    public List<String> extrairAuthorities(String token) {
        Claims claims = parseClaims(token);
        return (List<String>) claims.get("authorities");
    }

    public boolean tokenValido(String token, UserDetails userDetails) {
        try {
            Claims claims = parseClaims(token);
            Number versionClaim = claims.get("authVersion", Number.class);
            long tokenVersion = versionClaim == null ? 0 : versionClaim.longValue();
            long currentVersion = userDetails instanceof VersionedUserDetails versioned
                    ? versioned.getAuthVersion() : 0L;
            return claims.getSubject().equals(userDetails.getUsername())
                    && tokenVersion == currentVersion
                    && !claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
