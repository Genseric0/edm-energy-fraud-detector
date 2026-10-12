package com.energy.detection;

import com.energy.detection.config.SecurityConfig;
import com.energy.detection.controller.*;
import com.energy.detection.security.JwtUtil;
import com.energy.detection.service.EnergyAnalysisService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {EnergyController.class, DemoEnergyController.class}, properties = {
    "app.security.jwt.secret=unit-test-only-key-of-at-least-32-bytes",
    "app.security.jwt.expiration-ms=900000",
    "EDM_CORS_ORIGIN=http://localhost:3000"
})
@Import({SecurityConfig.class, JwtUtil.class})
class ApiSecurityTest {
    @Autowired MockMvc mvc;
    @MockBean EnergyAnalysisService service;
    @MockBean UserDetailsService users;

    @Test void anonymousCannotRead() throws Exception {
        mvc.perform(get("/api/energy/summary")).andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @Test @WithMockUser(roles = "OPERATOR") void operatorCannotReadAlertsOrMutate() throws Exception {
        mvc.perform(get("/api/energy/alerts")).andExpect(status().isForbidden());
        mvc.perform(post("/api/energy/transformers/1/normalize")).andExpect(status().isForbidden());
        mvc.perform(post("/api/energy/transformers/1/simulate-theft")
            .contentType("application/json").content("{\"clandestineKwh\":1}"))
            .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test @WithMockUser(roles = "OPERATOR") void operatorCanReadWithSecurityHeaders() throws Exception {
        mvc.perform(get("/api/energy/summary")).andExpect(status().isOk())
            .andExpect(header().string("X-Frame-Options", "DENY"))
            .andExpect(header().string("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'; base-uri 'none'"));
        verify(service).getDashboardSummary();
    }

    @Test @WithMockUser(roles = "ADMIN") void demoRoutesDoNotExistWithoutDemoProfile() throws Exception {
        mvc.perform(post("/api/energy/transformers/1/normalize")).andExpect(status().isNotFound());
        mvc.perform(post("/api/energy/transformers/1/simulate-theft")
            .contentType("application/json").content("{\"clandestineKwh\":1}"))
            .andExpect(status().isNotFound());
        verifyNoInteractions(service);
    }

    @Test @WithMockUser(roles = "ADMIN") void adminCanReadAlerts() throws Exception {
        mvc.perform(get("/api/energy/alerts")).andExpect(status().isOk());
        verify(service).getCriticalAlerts();
    }

    @Test @WithMockUser(roles = "ADMIN") void missingTransformerIs404() throws Exception {
        when(service.getAnalysisById(999L)).thenThrow(new ResourceNotFoundException("not found"));
        mvc.perform(get("/api/energy/transformers/999")).andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Recurso não encontrado"));
    }

    @Test @WithMockUser(roles = "ADMIN") void badIdentifierIs400() throws Exception {
        mvc.perform(get("/api/energy/transformers/invalid")).andExpect(status().isBadRequest());
    }

    @Test void invalidBearerIsRejected() throws Exception {
        mvc.perform(get("/api/energy/summary").header("Authorization", "Bearer invalid"))
            .andExpect(status().isUnauthorized());
    }

    @Test void corsAllowsConfiguredOriginAndRejectsOthers() throws Exception {
        mvc.perform(options("/api/auth/login").header("Origin", "http://localhost:3000")
            .header("Access-Control-Request-Method", "POST"))
            .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"));
        mvc.perform(options("/api/auth/login").header("Origin", "https://untrusted.example")
            .header("Access-Control-Request-Method", "POST")).andExpect(status().isForbidden());
    }
}
