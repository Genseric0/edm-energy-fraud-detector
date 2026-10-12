package com.energy.detection.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro executado uma vez por pedido: lê o cabeçalho Authorization,
 * valida o token JWT e, se válido, autentica o utilizador no SecurityContext
 * para essa requisição. É este mecanismo que substitui o antigo
 * SecurityContextHolder.setAuthentication() feito no login, que não persistia.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService users;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            String username = jwtUtil.extrairUsername(token);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // A assinatura identifica a sessão; o servidor decide as permissões actuais.
                UserDetails userDetails = users.loadUserByUsername(username);
                if (userDetails.isEnabled() && userDetails.isAccountNonLocked()
                        && userDetails.isAccountNonExpired() && userDetails.isCredentialsNonExpired()
                        && jwtUtil.tokenValido(token, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception ex) {
            // Token inválido/expirado: segue sem autenticar; os filtros seguintes do Spring Security
            // vão rejeitar o pedido caso a rota exija autenticação.
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
