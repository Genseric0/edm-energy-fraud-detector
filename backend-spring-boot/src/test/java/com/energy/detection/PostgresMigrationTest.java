package com.energy.detection;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

/** PostgreSQL real: falha se Docker não estiver disponível, sem fallback H2. */
@Testcontainers
class PostgresMigrationTest {
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    private String schema() { return "migration_" + UUID.randomUUID().toString().replace("-", ""); }
    private FluentConfiguration config(String schema) {
        return Flyway.configure().dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
            .schemas(schema).defaultSchema(schema).locations("classpath:db/migration")
            .baselineOnMigrate(false).cleanDisabled(true).validateOnMigrate(true);
    }
    private Connection connection(String schema) throws Exception {
        var connection = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        connection.setSchema(schema);
        return connection;
    }

    @Test void freshSchemaMigratesAndRepeatedStartupDoesNotReapply() throws Exception {
        String schema = schema();
        Flyway flyway = config(schema).load();
        assertEquals(5, flyway.migrate().migrationsExecuted);
        flyway.validate();
        assertEquals(0, flyway.migrate().migrationsExecuted);
        try (var c = connection(schema); var s = c.createStatement()) {
            var result = s.executeQuery("SELECT count(*) FROM flyway_schema_history WHERE success AND version IS NOT NULL");
            assertTrue(result.next());
            assertEquals(5, result.getInt(1));
            result = s.executeQuery("SELECT count(*) FROM information_schema.tables WHERE table_schema = current_schema() AND table_name IN ('transformers','residential_meters','meter_readings','inspection_records','app_user')");
            assertTrue(result.next());
            assertEquals(5, result.getInt(1));
        }
        assertThrows(FlywayException.class, flyway::clean);
    }

    @Test void upgradeFromV1PreservesReadingsAndInspections() throws Exception {
        String schema = schema();
        assertEquals(1, config(schema).target("1").load().migrate().migrationsExecuted);
        try (var c = connection(schema); var s = c.createStatement()) {
            s.executeUpdate("INSERT INTO transformers (code,neighborhood,total_supplied_kwh) VALUES ('TEST-1','Synthetic',200)");
            s.executeUpdate("INSERT INTO residential_meters (meter_number,resident_name,address,billed_kwh,installation_location,remote_reading_enabled,transformer_id) VALUES ('TEST-M1','Synthetic','Test',100,'UNKNOWN',false,1)");
            s.executeUpdate("INSERT INTO meter_readings (meter_id,recorded_at,energy_kwh,source) VALUES (1,'2026-01-01T00:00:00Z',123.45,'MANUAL')");
            s.executeUpdate("INSERT INTO inspection_records (meter_id,created_at,reported_by,estimated_unaccounted_kwh,status) VALUES (1,'2026-01-01T00:00:00Z','test',12.5,'PENDING_REVIEW')");
        }
        assertEquals(4, config(schema).load().migrate().migrationsExecuted);
        try (var c = connection(schema); var s = c.createStatement()) {
            var result = s.executeQuery("SELECT energy_kwh FROM meter_readings WHERE id=1");
            assertTrue(result.next());
            assertEquals(123.45, result.getDouble(1), 0.00001);
            assertFalse(result.next());
            result = s.executeQuery("SELECT estimated_unaccounted_kwh, status FROM inspection_records WHERE id=1");
            assertTrue(result.next());
            assertEquals(12.5, result.getDouble(1), 0.00001);
            assertEquals("PENDING_REVIEW", result.getString(2));
            result = s.executeQuery("SELECT count(*) FROM pg_indexes WHERE schemaname=current_schema() AND indexname IN ('idx_meter_transformer','idx_reading_meter_time','idx_inspection_meter_time')");
            assertTrue(result.next());
            assertEquals(3, result.getInt(1));
            result = s.executeQuery("SELECT count(*) FROM information_schema.columns WHERE table_schema=current_schema() AND table_name='app_user' AND column_name IN ('password_hash','failed_attempts','locked_until','auth_version')");
            assertTrue(result.next());
            assertEquals(4, result.getInt(1));
            // Sequência continua a gerar IDs válidos depois da migração incremental.
            s.executeUpdate("INSERT INTO meter_readings (meter_id,recorded_at,energy_kwh,source) VALUES (1,'2026-01-02T00:00:00Z',150,'MANUAL')");
            result = s.executeQuery("SELECT count(DISTINCT id) FROM meter_readings");
            assertTrue(result.next());
            assertEquals(2, result.getInt(1));
        }
    }

    @Test void legacySchemaIsRefusedWithoutImplicitBaselineOrDataLoss() throws Exception {
        String schema = schema();
        try (var c = connection("public"); var s = c.createStatement()) {
            s.execute("CREATE SCHEMA " + schema);
            s.execute("CREATE TABLE " + schema + ".legacy_marker (id INTEGER PRIMARY KEY)");
            s.execute("INSERT INTO " + schema + ".legacy_marker VALUES (42)");
        }
        assertThrows(FlywayException.class, () -> config(schema).load().migrate());
        try (var c = connection(schema); var s = c.createStatement()) {
            var result = s.executeQuery("SELECT id FROM legacy_marker");
            assertTrue(result.next());
            assertEquals(42, result.getInt(1));
        }
    }

    @Test void v5PreservesV4AuditAndAddsAdministrationGuardAndMetadataConstraints() throws Exception {
        String schema = schema();
        assertEquals(4, config(schema).target("4").load().migrate().migrationsExecuted);
        try(var c=connection(schema);var s=c.createStatement()) {
            s.executeUpdate("INSERT INTO app_user(username,password_hash,display_name,role,auth_version) VALUES ('synthetic-admin','test-only-hash','Synthetic','ADMIN',1)");
            s.executeUpdate("INSERT INTO account_security_event(account_id,occurred_at,action,auth_version) VALUES (1,'2026-01-01T00:00:00Z','PASSWORD_CHANGED',1)");
        }
        assertEquals(1,config(schema).load().migrate().migrationsExecuted);
        try(var c=connection(schema);var s=c.createStatement()) {
            var r=s.executeQuery("SELECT action,auth_version,actor_id FROM account_security_event WHERE id=1");
            assertTrue(r.next()); assertEquals("PASSWORD_CHANGED",r.getString(1)); assertEquals(1,r.getLong(2)); assertNull(r.getObject(3));
            r=s.executeQuery("SELECT count(*) FROM account_management_guard WHERE id=1"); assertTrue(r.next()); assertEquals(1,r.getInt(1));
            assertThrows(java.sql.SQLException.class,()->s.executeUpdate("INSERT INTO account_security_event(account_id,occurred_at,action,auth_version) VALUES (1,CURRENT_TIMESTAMP,'ACCOUNT_UPDATED',2)"));
            assertThrows(java.sql.SQLException.class,()->s.executeUpdate("UPDATE account_security_event SET action='PASSWORD_CHANGED' WHERE id=1"));
            assertThrows(java.sql.SQLException.class,()->s.executeUpdate("DELETE FROM account_security_event WHERE id=1"));
        }
    }
}
