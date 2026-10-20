package com.energy.detection;

import com.energy.detection.model.AppUser;
import com.energy.detection.repository.*;
import com.energy.detection.security.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** H2 é uma verificação adicional da API, sem substituir os testes PostgreSQL/CI. */
@AutoConfigureMockMvc
@SpringBootTest(properties = {
    "JWT_SECRET=test-only-local-api-key-at-least-32-bytes", "app.demo.seed-enabled=true",
    "spring.datasource.url=jdbc:h2:mem:local-api-flows;DB_CLOSE_DELAY=-1",
    "EDM_GENSERICO_PASSWORD=test-genserico-password", "EDM_OXIDIA_PASSWORD=test-oxidia-password",
    "EDM_LEYSA_PASSWORD=test-leysa-password", "EDM_HELEN_PASSWORD=test-helen-password"
})
class LocalApiFlowsTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired MeterRepository meters;
    @Autowired MeterReadingRepository readings;
    @Autowired AppUserRepository users;
    @Autowired JdbcTemplate jdbc;
    @Autowired JwtUtil jwt;
    private long meterId;
    private String path;
    @BeforeEach void reset() {
        readings.deleteAll();
        meterId=meters.findAll().get(0).getId(); path="/api/energy/meters/"+meterId+"/readings";
        jdbc.update("UPDATE app_user SET active=true,role='ADMIN',failed_attempts=0,locked_until=NULL,auth_version=auth_version+1 WHERE username IN ('genserico','oxidia')");
        jdbc.update("UPDATE app_user SET active=true,role='OPERATOR',failed_attempts=0,locked_until=NULL,auth_version=auth_version+1 WHERE username='helen'");
    }
    private AppUser user(String name) { return users.findByUsername(name).orElseThrow(); }
    private String token(String name) { return jwt.gerarToken(user(name)); }
    private MockHttpServletRequestBuilder reading(String body,String actor) {
        return post(path).header("Authorization","Bearer "+token(actor)).contentType("application/json").content(body);
    }
    private String input(String amount,String time) { return "{\"recordedAt\":\""+time+"\",\"energyKwh\":"+amount+"}"; }

    @Test void exactDecimalAndAuthenticatedAuthorArePersistedAndReturned() throws Exception {
        mvc.perform(reading(input("123.456","2026-01-01T10:00:00.123456Z"),"genserico"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.ingestedBy").value("genserico")).andExpect(jsonPath("$.source").value("MANUAL"));
        var saved=readings.findAll().get(0);
        assertEquals(0,new BigDecimal("123.456").compareTo(saved.getEnergyKwh()));
        assertEquals(Instant.parse("2026-01-01T10:00:00.123456Z"),saved.getRecordedAt());
        mvc.perform(get(path).header("Authorization","Bearer "+token("helen"))).andExpect(status().isOk()).andExpect(jsonPath("$[0].energyKwh").value(123.456));
    }
    @Test void duplicatesReturnConflictAndNeverCreateTwoReadings() throws Exception {
        String input=input("0.001","2026-01-01T10:00:00Z");
        mvc.perform(reading(input,"genserico")).andExpect(status().isCreated());
        mvc.perform(reading(input,"genserico")).andExpect(status().isConflict());
        assertEquals(1,readings.count());
    }
    @Test void rejectsNonFiniteNegativeOversizedAndOverPreciseValues() throws Exception {
        for(String value:List.of("\"Infinity\"","\"NaN\"","-0.001","100000000000","0.0001","null"))
            mvc.perform(reading(input(value,"2026-01-01T10:00:00Z"),"genserico")).andExpect(status().isBadRequest());
        assertEquals(0,readings.count());
    }
    @Test void refusesFutureAndSubMicrosecondTimestampsInsteadOfRounding() throws Exception {
        mvc.perform(reading(input("1",Instant.now().plusSeconds(3600).toString()),"genserico")).andExpect(status().isBadRequest());
        mvc.perform(reading(input("1","2026-01-01T10:00:00.123456789Z"),"genserico")).andExpect(status().isBadRequest());
        assertEquals(0,readings.count());
    }
    @Test void permissionsAndMissingMeterAreEnforced() throws Exception {
        mvc.perform(post(path).contentType("application/json").content(input("1","2026-01-01T10:00:00Z"))).andExpect(status().isUnauthorized());
        mvc.perform(reading(input("1","2026-01-01T10:00:00Z"),"helen")).andExpect(status().isForbidden());
        mvc.perform(post("/api/energy/meters/999999/readings").header("Authorization","Bearer "+token("genserico")).contentType("application/json").content(input("1","2026-01-01T10:00:00Z"))).andExpect(status().isNotFound());
    }
    @Test void legacyNullAuthorIsReadableAndClientCannotForgeSourceOrAuthor() throws Exception {
        var meter=meters.findById(meterId).orElseThrow();
        readings.saveAndFlush(com.energy.detection.model.MeterReading.builder().meter(meter).recordedAt(Instant.parse("2025-01-01T00:00:00Z")).energyKwh(new BigDecimal("2.500")).source("MANUAL").build());
        mvc.perform(get(path).header("Authorization","Bearer "+token("genserico"))).andExpect(status().isOk());
        mvc.perform(reading("{\"recordedAt\":\"2026-01-01T00:00:00Z\",\"energyKwh\":3,\"source\":\"AMI\",\"ingestedBy\":\"other\"}","genserico"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.ingestedBy").value("genserico")).andExpect(jsonPath("$.source").value("MANUAL"));
    }
    @Test void concurrentDuplicateInsertionHasOneWinner() throws Exception {
        String bearer=token("genserico"); var start=new CountDownLatch(1); var executor=Executors.newFixedThreadPool(2);
        try {
            var jobs=new ArrayList<Future<Integer>>();
            for(int i=0;i<2;i++) jobs.add(executor.submit(()->{
                assertTrue(start.await(10,TimeUnit.SECONDS));
                return mvc.perform(post(path).header("Authorization","Bearer "+bearer).contentType("application/json").content(input("12.345","2026-01-01T00:00:00Z"))).andReturn().getResponse().getStatus();
            }));
            start.countDown(); var statuses=new ArrayList<Integer>(); for(var job:jobs)statuses.add(job.get(20,TimeUnit.SECONDS));
            Collections.sort(statuses); assertEquals(List.of(201,409),statuses); assertEquals(1,readings.count());
        } finally { executor.shutdownNow(); }
    }
    private Map<String,Object> adminBody(String target,boolean active) {
        return Map.of("role","OPERATOR","active",active,"expectedVersion",user(target).getAuthVersion(),"reason","Verificação local com conta sintética","currentPassword","test-genserico-password");
    }
    private MockHttpServletRequestBuilder admin(String target,Map<String,Object> body,String actor) throws Exception {
        return put("/api/admin/accounts/"+user(target).getId()).header("Authorization","Bearer "+token(actor)).contentType("application/json").content(json.writeValueAsString(body))
            .with(r->{r.setRemoteAddr(UUID.randomUUID().toString());return r;});
    }
    @Test void administrativeUiApiCanDisableAndRestoreWithoutRestoringOldTokens() throws Exception {
        String old=token("helen");
        mvc.perform(admin("helen",adminBody("helen",false),"genserico")).andExpect(status().isOk());
        mvc.perform(admin("helen",adminBody("helen",true),"genserico")).andExpect(status().isOk());
        mvc.perform(get("/api/auth/me").header("Authorization","Bearer "+old)).andExpect(status().isUnauthorized());
        assertEquals(2,jdbc.queryForObject("SELECT count(*) FROM account_security_event WHERE account_id=? AND action='ACCOUNT_UPDATED' AND actor_id=?",Integer.class,user("helen").getId(),user("genserico").getId()));
    }
    @Test void operatorCannotAdministerAndAdminListOmitsCredentials() throws Exception {
        mvc.perform(admin("helen",adminBody("helen",false),"helen")).andExpect(status().isForbidden());
        String body=mvc.perform(get("/api/admin/accounts").header("Authorization","Bearer "+token("genserico"))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertFalse(body.contains("password"));assertFalse(body.contains("$2a$"));
    }
    @Test void inspectionEstimateIsExactAndNeverBecomesAFine() throws Exception {
        mvc.perform(post("/api/energy/meters/"+meterId+"/inspections").header("Authorization","Bearer "+token("genserico")).contentType("application/json")
            .content("{\"estimatedUnaccountedKwh\":12.345,\"inspectionNotes\":\"Registo sintético de teste\"}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.estimatedUnaccountedKwh").value(12.345))
            .andExpect(jsonPath("$.status").value("PENDING_REVIEW")).andExpect(jsonPath("$.fineAmount").isEmpty());
        assertEquals(0,new BigDecimal("12.345").compareTo(jdbc.queryForObject("SELECT estimated_unaccounted_kwh FROM inspection_records WHERE meter_id=? ORDER BY id DESC LIMIT 1",BigDecimal.class,meterId)));
    }
    @Test void inspectionRejectsNonFiniteAndOverPreciseEstimates() throws Exception {
        for(String value:List.of("\"Infinity\"","\"NaN\"","-1","0.0001","100000000000"))
            mvc.perform(post("/api/energy/meters/"+meterId+"/inspections").header("Authorization","Bearer "+token("genserico")).contentType("application/json")
                .content("{\"estimatedUnaccountedKwh\":"+value+"}")).andExpect(status().isBadRequest());
    }
    @Test void localAdministrationProtectsLastActiveAdmin() throws Exception {
        jdbc.update("UPDATE app_user SET active=false WHERE username='oxidia'");
        var input=new HashMap<String,Object>(adminBody("genserico",false));input.put("role","ADMIN");
        mvc.perform(admin("genserico",input,"genserico")).andExpect(status().isConflict());
        assertTrue(user("genserico").isActive());
    }
    @Test void localAdministrativeMutualDeactivationHasOneWinner() throws Exception {
        String a=token("genserico"),b=token("oxidia");
        var first=new HashMap<String,Object>(adminBody("oxidia",false));first.put("role","ADMIN");
        var second=new HashMap<String,Object>(adminBody("genserico",false));second.put("role","ADMIN");second.put("currentPassword","test-oxidia-password");
        long firstId=user("oxidia").getId(),secondId=user("genserico").getId();
        var start=new CountDownLatch(1);var executor=Executors.newFixedThreadPool(2);
        try {
            var jobs=new ArrayList<Future<Integer>>();
            jobs.add(executor.submit(()->{start.await(10,TimeUnit.SECONDS);return mvc.perform(put("/api/admin/accounts/"+firstId).header("Authorization","Bearer "+a).contentType("application/json").content(json.writeValueAsString(first))).andReturn().getResponse().getStatus();}));
            jobs.add(executor.submit(()->{start.await(10,TimeUnit.SECONDS);return mvc.perform(put("/api/admin/accounts/"+secondId).header("Authorization","Bearer "+b).contentType("application/json").content(json.writeValueAsString(second))).andReturn().getResponse().getStatus();}));
            start.countDown();var statuses=new ArrayList<Integer>();for(var job:jobs)statuses.add(job.get(20,TimeUnit.SECONDS));
            assertEquals(1,statuses.stream().filter(status->status==200).count());
            assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM app_user WHERE role='ADMIN' AND active=true",Integer.class));
        } finally {executor.shutdownNow();}
    }
}
