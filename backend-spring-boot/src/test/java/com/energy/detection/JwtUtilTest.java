package com.energy.detection;

import com.energy.detection.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {
    private static final String KEY = "unit-test-only-key-of-at-least-32-bytes";
    @Test void rejectsShortBlankAndNullSecrets() {
        assertThrows(IllegalArgumentException.class, () -> new JwtUtil("short", 900000));
        assertThrows(IllegalArgumentException.class, () -> new JwtUtil(" ".repeat(32), 900000));
        assertThrows(IllegalArgumentException.class, () -> new JwtUtil(null, 900000));
    }
    @Test void rejectsInvalidLifetime() {
        assertThrows(IllegalArgumentException.class, () -> new JwtUtil(KEY, 0));
        assertThrows(IllegalArgumentException.class, () -> new JwtUtil(KEY, 900001));
    }
    @Test void roundTripAndDifferentKey() {
        var jwt = new JwtUtil(KEY, 900000);
        var user = User.withUsername("inspector").password("unused").roles("OPERATOR").build();
        String token = jwt.gerarToken(user);
        assertTrue(jwt.tokenValido(token, user));
        assertEquals("inspector", jwt.extrairUsername(token));
        assertFalse(new JwtUtil(KEY + "other", 900000).tokenValido(token, user));
    }
}
