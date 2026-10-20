package com.energy.detection;

import com.energy.detection.security.LoginRateLimitFilter;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import static org.junit.jupiter.api.Assertions.*;

class LoginRateLimitTest {
    private static class MutableClock extends Clock {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        public ZoneId getZone() { return ZoneOffset.UTC; }
        public Clock withZone(ZoneId zone) { return this; }
        public Instant instant() { return now; }
    }
    private MockHttpServletResponse login(LoginRateLimitFilter filter, String ip) throws Exception {
        var request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setServletPath("/api/auth/login");
        request.setRemoteAddr(ip);
        // Não deve ser possível contornar o limite com cabeçalho forjado.
        request.addHeader("X-Forwarded-For", java.util.UUID.randomUUID().toString());
        var response = new MockHttpServletResponse();
        filter.doFilter(request, response, (req, res) -> {});
        return response;
    }
    @Test void limitsAttemptsIsolatesIpsAndExpiresWindow() throws Exception {
        var clock = new MutableClock();
        var filter = new LoginRateLimitFilter(clock);
        for (int i = 0; i < 10; i++) assertEquals(200, login(filter, "192.0.2.1").getStatus());
        var rejected = login(filter, "192.0.2.1");
        assertEquals(429, rejected.getStatus());
        assertEquals("60", rejected.getHeader("Retry-After"));
        assertEquals(200, login(filter, "192.0.2.2").getStatus());
        clock.now = clock.now.plusSeconds(60);
        assertEquals(200, login(filter, "192.0.2.1").getStatus());
    }

    @Test void passwordRotationSharesTheLoginIpBudget() throws Exception {
        var filter = new LoginRateLimitFilter(new MutableClock());
        for (int i = 0; i < 10; i++) login(filter, "192.0.2.3");
        var request = new MockHttpServletRequest("POST", "/api/auth/password");
        request.setServletPath("/api/auth/password");
        request.setRemoteAddr("192.0.2.3");
        var response = new MockHttpServletResponse();
        filter.doFilter(request, response, (req, res) -> fail("Rate limit contornado"));
        assertEquals(429, response.getStatus());
    }
    @Test void adminMutationsShareCredentialAttemptBudget() throws Exception {
        var filter = new LoginRateLimitFilter(new MutableClock());
        for(int i=0;i<10;i++) login(filter,"192.0.2.4");
        for(String method:java.util.List.of("PUT","POST")) {
            var request=new MockHttpServletRequest(method,"/api/admin/accounts/1");
            request.setServletPath("/api/admin/accounts/1"); request.setRemoteAddr("192.0.2.4");
            var response=new MockHttpServletResponse();
            filter.doFilter(request,response,(req,res)->fail("Admin mutation bypassed rate limit"));
            assertEquals(429,response.getStatus());
        }
    }
}
