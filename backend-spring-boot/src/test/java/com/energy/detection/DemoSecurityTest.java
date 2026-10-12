package com.energy.detection;

import com.energy.detection.config.SecurityConfig;
import com.energy.detection.config.AccountsConfiguration;
import com.energy.detection.controller.DemoEnergyController;
import com.energy.detection.security.JwtUtil;
import com.energy.detection.service.EnergyAnalysisService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("demo")
@WebMvcTest(controllers = DemoEnergyController.class, properties = {
    "app.security.jwt.secret=unit-test-only-key-of-at-least-32-bytes",
    "app.security.jwt.expiration-ms=900000"
})
@Import({SecurityConfig.class, AccountsConfiguration.class, JwtUtil.class})
class DemoSecurityTest {
    @Autowired MockMvc mvc;
    @MockBean EnergyAnalysisService service;
    @MockBean UserDetailsService users;

    @ParameterizedTest @ValueSource(strings = {"{}", "{\"clandestineKwh\":null}",
        "{\"clandestineKwh\":-1}", "{\"clandestineKwh\":0}", "{\"clandestineKwh\":1000001}", "invalid"})
    @WithMockUser(roles = "ADMIN") void invalidSimulationCannotMutate(String body) throws Exception {
        mvc.perform(post("/api/energy/transformers/1/simulate-theft").contentType("application/json").content(body))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test @WithMockUser(roles = "ADMIN") void adminCanSimulateInDemo() throws Exception {
        mvc.perform(post("/api/energy/transformers/1/simulate-theft").contentType("application/json")
            .content("{\"clandestineKwh\":2400}")).andExpect(status().isOk());
        verify(service).simulateTheft(1L, 2400.0);
    }

    @Test @WithMockUser(roles = "OPERATOR") void operatorCannotSimulateEvenInDemo() throws Exception {
        mvc.perform(post("/api/energy/transformers/1/normalize")).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }
}
