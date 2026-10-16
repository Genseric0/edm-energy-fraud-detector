package com.energy.detection.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.time.Clock;
import java.util.HashMap;
import java.util.Map;

/** Limite por IP remoto, sem confiar em X-Forwarded-For fornecido pelo cliente.
 * Para múltiplas instâncias, substituir por armazenamento partilhado no gateway.
 */
public class LoginRateLimitFilter extends OncePerRequestFilter {
    private static final int MAX_KEYS = 4096;
    private static final int MAX_ATTEMPTS = 10;
    private static final long WINDOW_MS = 60000;
    private final Map<String, Window> windows = new HashMap<>();
    private final Clock clock;
    private record Window(long start, int attempts) {}

    public LoginRateLimitFilter() { this(Clock.systemUTC()); }
    public LoginRateLimitFilter(Clock clock) { this.clock = clock; }

    private synchronized boolean allow(String key) {
        long now = clock.millis();
        windows.entrySet().removeIf(e -> now - e.getValue().start() >= WINDOW_MS);
        Window window = windows.get(key);
        if (window == null) {
            if (windows.size() >= MAX_KEYS) return false;
            windows.put(key, new Window(now, 1));
            return true;
        }
        if (window.attempts() >= MAX_ATTEMPTS) return false;
        windows.put(key, new Window(window.start(), window.attempts() + 1));
        return true;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if ("POST".equals(request.getMethod())
                && ("/api/auth/login".equals(request.getServletPath()) || "/api/auth/password".equals(request.getServletPath()))
                && !allow(request.getRemoteAddr())) {
            response.setStatus(429);
            response.setHeader("Retry-After", "60");
            response.setContentType("application/json");
            response.getWriter().write("{\"message\":\"Demasiadas tentativas. Aguarde um minuto.\"}");
            return;
        }
        chain.doFilter(request, response);
    }
}
