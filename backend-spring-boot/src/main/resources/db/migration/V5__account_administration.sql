CREATE TABLE account_management_guard (
    id BIGINT PRIMARY KEY CHECK (id = 1)
);
INSERT INTO account_management_guard(id) VALUES (1);

ALTER TABLE account_security_event DROP CONSTRAINT account_security_event_action_check;
ALTER TABLE account_security_event ADD CONSTRAINT account_security_event_action_check
    CHECK (action IN ('PASSWORD_CHANGED','ACCOUNT_UPDATED','ACCOUNT_UNLOCKED','SESSIONS_REVOKED'));
ALTER TABLE account_security_event ADD COLUMN actor_id BIGINT REFERENCES app_user(id);
ALTER TABLE account_security_event ADD COLUMN reason VARCHAR(500);
ALTER TABLE account_security_event ADD COLUMN old_role VARCHAR(32);
ALTER TABLE account_security_event ADD COLUMN new_role VARCHAR(32);
ALTER TABLE account_security_event ADD COLUMN old_active BOOLEAN;
ALTER TABLE account_security_event ADD COLUMN new_active BOOLEAN;
-- Eventos V4 permanecem intactos; os novos eventos administrativos exigem autor/motivo.
ALTER TABLE account_security_event ADD CONSTRAINT account_security_event_admin_metadata
    CHECK (action = 'PASSWORD_CHANGED' OR
        (actor_id IS NOT NULL AND reason IS NOT NULL AND char_length(trim(reason)) BETWEEN 10 AND 500));
