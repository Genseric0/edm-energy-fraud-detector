package com.energy.detection;

import com.energy.detection.model.*;
import com.energy.detection.repository.AppUserRepository;
import com.energy.detection.security.JwtUtil;
import com.energy.detection.service.AccountAdministrationService;
import com.energy.detection.controller.AccountConflictException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@ActiveProfiles("postgres")
@AutoConfigureMockMvc
@SpringBootTest(properties = {
    "JWT_SECRET=test-only-admin-api-key-at-least-32-bytes",
    "app.demo.seed-enabled=false",
    "EDM_GENSERICO_PASSWORD=test-genserico-password", "EDM_OXIDIA_PASSWORD=test-oxidia-password",
    "EDM_LEYSA_PASSWORD=test-leysa-password", "EDM_HELEN_PASSWORD=test-helen-password"
})
class AccountAdministrationTest {
    @Container static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
    @DynamicPropertySource static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired AppUserRepository users;
    @Autowired PasswordEncoder encoder;
    @Autowired JwtUtil jwt;
    @Autowired ObjectMapper json;
    @Autowired AccountAdministrationService administration;

    @BeforeEach void resetFixtures() {
        for (String name : List.of("genserico", "oxidia", "leysa", "helen"))
            jdbc.update("UPDATE app_user SET active=true, failed_attempts=0, locked_until=NULL, role=?, password_hash=?, auth_version=auth_version+1 WHERE username=?",
                name.equals("genserico") || name.equals("oxidia") ? "ADMIN" : "OPERATOR", encoder.encode("test-" + name + "-password"), name);
    }
    @AfterEach void clearContext() { SecurityContextHolder.clearContext(); }
    private AppUser user(String name) { return users.findByUsername(name).orElseThrow(); }
    private String token(String name) { return jwt.gerarToken(user(name)); }
    private String path(String name) { return "/api/admin/accounts/" + user(name).getId(); }
    private long events() { return jdbc.queryForObject("SELECT count(*) FROM account_security_event", Long.class); }
    private Map<String,Object> action(String name) {
        return new HashMap<>(Map.of("expectedVersion", user(name).getAuthVersion(), "reason", "Revisão administrativa de teste", "currentPassword", "test-genserico-password"));
    }
    private Map<String,Object> update(String name, String role, boolean active) {
        var body = action(name); body.put("role", role); body.put("active", active); return body;
    }
    private MockHttpServletRequestBuilder request(MockHttpServletRequestBuilder builder, String bearer, Object body) throws Exception {
        return builder.header("Authorization", "Bearer " + bearer).contentType("application/json")
            .with(req -> { req.setRemoteAddr(UUID.randomUUID().toString()); return req; })
            .content(json.writeValueAsString(body));
    }
    private void context(AppUser actor) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(actor, null, actor.getAuthorities()));
    }

    @Test void anonymousAndOperatorCannotListOrMutateAccounts() throws Exception {
        long count = events();
        mvc.perform(get("/api/admin/accounts")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/accounts").header("Authorization", "Bearer " + token("helen"))).andExpect(status().isForbidden());
        for (String suffix : List.of("/revoke-sessions", "/unlock"))
            mvc.perform(request(post(path("leysa") + suffix), token("helen"), action("leysa"))).andExpect(status().isForbidden());
        mvc.perform(request(put(path("leysa")), token("helen"), update("leysa", "ADMIN", false))).andExpect(status().isForbidden());
        assertEquals(count, events());
    }

    @Test void listIsPaginatedAndNeverSerializesHashesOrCredentials() throws Exception {
        String body = mvc.perform(get("/api/admin/accounts?size=2&page=0").header("Authorization", "Bearer " + token("genserico")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(4)).andExpect(jsonPath("$.accounts.length()").value(2))
            .andReturn().getResponse().getContentAsString();
        var node = json.readTree(body).get("accounts").get(0);
        Set<String> fields = new HashSet<>(); node.fieldNames().forEachRemaining(fields::add);
        assertEquals(Set.of("id","username","displayName","role","active","failedAttempts","lockedUntil","version"), fields);
        assertFalse(body.contains("$2a$")); assertFalse(body.contains("test-genserico-password"));
        mvc.perform(get("/api/admin/accounts?size=101").header("Authorization", "Bearer " + token("genserico"))).andExpect(status().isBadRequest());
        mvc.perform(get("/api/admin/accounts?page=-1").header("Authorization", "Bearer " + token("genserico"))).andExpect(status().isBadRequest());
    }

    @Test void disableAndReactivateAreAuditedAndNeverRestoreOldTokens() throws Exception {
        AppUser previous = user("helen"); String oldToken = token("helen"); String admin = token("genserico");
        long count = events();
        mvc.perform(request(put(path("helen")), admin, update("helen", "OPERATOR", false))).andExpect(status().isOk());
        assertFalse(user("helen").isActive()); assertEquals(count + 1, events());
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + oldToken)).andExpect(status().isUnauthorized());
        var event = jdbc.queryForMap("SELECT actor_id,action,old_active,new_active,reason FROM account_security_event WHERE account_id=? AND auth_version=?", previous.getId(), previous.getAuthVersion()+1);
        assertEquals(user("genserico").getId(), ((Number) event.get("actor_id")).longValue());
        assertEquals("ACCOUNT_UPDATED", event.get("action")); assertEquals(true,event.get("old_active")); assertEquals(false,event.get("new_active"));
        mvc.perform(request(put(path("helen")), admin, update("helen", "OPERATOR", true))).andExpect(status().isOk());
        assertTrue(user("helen").isActive()); assertEquals(previous.getPassword(), user("helen").getPassword());
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + oldToken)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token("helen"))).andExpect(status().isOk());
    }

    @Test void roleReductionRevokesTokensAndFreshOperatorTokenCannotAdminister() throws Exception {
        String oldToken = token("oxidia");
        mvc.perform(request(put(path("oxidia")), token("genserico"), update("oxidia", "OPERATOR", true))).andExpect(status().isOk());
        assertEquals(AccountRole.OPERATOR, user("oxidia").getRole());
        mvc.perform(get("/api/admin/accounts").header("Authorization", "Bearer " + oldToken)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/accounts").header("Authorization", "Bearer " + token("oxidia"))).andExpect(status().isForbidden());
        var event = jdbc.queryForMap("SELECT old_role,new_role FROM account_security_event WHERE account_id=? AND auth_version=?", user("oxidia").getId(), user("oxidia").getAuthVersion());
        assertEquals("ADMIN", event.get("old_role")); assertEquals("OPERATOR", event.get("new_role"));
    }

    @Test void revocationPreservesPasswordAndInvalidatesOnlyTargetAccount() throws Exception {
        AppUser previous = user("helen"); String old = token("helen"); String other = token("leysa");
        mvc.perform(request(post(path("helen") + "/revoke-sessions"), token("genserico"), action("helen"))).andExpect(status().isOk());
        assertEquals(previous.getPassword(), user("helen").getPassword());
        assertEquals(previous.getAuthVersion()+1, user("helen").getAuthVersion());
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + old)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + other)).andExpect(status().isOk());
    }

    @Test void unlockClearsFailuresAndCannotResurrectPreviouslyIssuedToken() throws Exception {
        String old = token("helen");
        jdbc.update("UPDATE app_user SET failed_attempts=5, locked_until=CURRENT_TIMESTAMP+INTERVAL '15 minutes' WHERE username='helen'");
        mvc.perform(request(post(path("helen") + "/unlock"), token("genserico"), action("helen"))).andExpect(status().isOk());
        assertEquals(0,user("helen").getFailedAttempts()); assertNull(user("helen").getLockedUntil());
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + old)).andExpect(status().isUnauthorized());
        assertEquals("ACCOUNT_UNLOCKED", jdbc.queryForObject("SELECT action FROM account_security_event WHERE account_id=? AND auth_version=?", String.class,user("helen").getId(),user("helen").getAuthVersion()));
    }

    @Test void staleVersionMissingAccountAndNoOpAreNotSilentlyAccepted() throws Exception {
        var stale = update("helen", "OPERATOR", false); stale.put("expectedVersion", user("helen").getAuthVersion()-1);
        long count = events();
        mvc.perform(request(put(path("helen")), token("genserico"), stale)).andExpect(status().isConflict());
        mvc.perform(request(put(path("helen")), token("genserico"), update("helen","OPERATOR",true))).andExpect(status().isConflict());
        mvc.perform(request(post("/api/admin/accounts/999999/revoke-sessions"), token("genserico"), action("helen"))).andExpect(status().isNotFound());
        assertEquals(count,events()); assertTrue(user("helen").isActive());
    }

    @Test void reasonAndRoleAreValidatedBeforeChanges() throws Exception {
        long count = events();
        for (String reason : List.of("short", "                  ")) {
            var body = update("helen","ADMIN",true); body.put("reason",reason);
            mvc.perform(request(put(path("helen")),token("genserico"),body)).andExpect(status().isBadRequest());
        }
        var invalid = update("helen","SUPERUSER",true);
        mvc.perform(request(put(path("helen")),token("genserico"),invalid)).andExpect(status().isBadRequest());
        assertEquals(count,events()); assertEquals(AccountRole.OPERATOR,user("helen").getRole());
    }

    @Test void incorrectReauthenticationCountsFailuresAndLocksActorWithoutMutatingTarget() throws Exception {
        String admin = token("genserico"); long count = events();
        for(int i=0;i<5;i++) {
            var body = update("helen","ADMIN",true); body.put("currentPassword","wrong-password");
            mvc.perform(request(put(path("helen")),admin,body)).andExpect(status().isBadRequest());
        }
        assertFalse(user("genserico").isAccountNonLocked()); assertEquals(5,user("genserico").getFailedAttempts());
        mvc.perform(get("/api/admin/accounts").header("Authorization","Bearer " + admin)).andExpect(status().isUnauthorized());
        assertEquals(count,events()); assertEquals(AccountRole.OPERATOR,user("helen").getRole());
    }

    @Test void lastActiveAdminCannotBeDisabledOrDemoted() throws Exception {
        jdbc.update("UPDATE app_user SET active=false WHERE username='oxidia'");
        long count = events();
        mvc.perform(request(put(path("genserico")),token("genserico"),update("genserico","ADMIN",false))).andExpect(status().isConflict());
        mvc.perform(request(put(path("genserico")),token("genserico"),update("genserico","OPERATOR",true))).andExpect(status().isConflict());
        assertTrue(user("genserico").isActive()); assertEquals(AccountRole.ADMIN,user("genserico").getRole()); assertEquals(count,events());
    }

    @Test void selfRevocationEndsTheAdministratorsCurrentSession() throws Exception {
        String admin = token("genserico");
        mvc.perform(request(post(path("genserico")+"/revoke-sessions"),admin,action("genserico"))).andExpect(status().isOk());
        mvc.perform(get("/api/admin/accounts").header("Authorization","Bearer " + admin)).andExpect(status().isUnauthorized());
    }

    @Test void concurrentCrossDeactivationCannotRemoveBothAdministrators() throws Exception {
        AppUser first = user("genserico"), second = user("oxidia"); long count = events();
        var start = new CountDownLatch(1); var executor = Executors.newFixedThreadPool(2);
        try {
            var results = new ArrayList<Future<Boolean>>();
            for (AppUser actor : List.of(first,second)) results.add(executor.submit(() -> {
                AppUser target = actor.getId().equals(first.getId()) ? second : first;
                context(actor);
                try {
                    assertTrue(start.await(10,TimeUnit.SECONDS));
                    administration.update(actor,target.getId(),target.getAuthVersion(),AccountRole.ADMIN,false,"Desactivação concorrente de teste","test-"+actor.getUsername()+"-password");
                    return true;
                } catch (CredentialsExpiredException | AccountConflictException expected) { return false; }
                finally { SecurityContextHolder.clearContext(); }
            }));
            start.countDown(); int successes=0;
            for(var result:results) if(result.get(20,TimeUnit.SECONDS)) successes++;
            assertEquals(1,successes); assertEquals(1,users.countByRoleAndActiveTrue(AccountRole.ADMIN)); assertEquals(count+1,events());
        } finally { executor.shutdownNow(); }
    }

    @Test void failedAuditInsertRollsBackStateAndVersion() {
        AppUser actor=user("genserico"), previous=user("helen"); context(actor);
        jdbc.update("INSERT INTO account_security_event(account_id,occurred_at,action,auth_version) VALUES (?,CURRENT_TIMESTAMP,'PASSWORD_CHANGED',?)",previous.getId(),previous.getAuthVersion()+1);
        assertThrows(DataIntegrityViolationException.class,() -> administration.update(actor,previous.getId(),previous.getAuthVersion(),AccountRole.ADMIN,false,"Falha de auditoria simulada","test-genserico-password"));
        assertEquals(previous.getAuthVersion(),user("helen").getAuthVersion()); assertEquals(AccountRole.OPERATOR,user("helen").getRole()); assertTrue(user("helen").isActive());
    }
}
