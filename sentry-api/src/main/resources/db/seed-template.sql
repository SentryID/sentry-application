-- organizations
INSERT INTO organizations (name, external_code, uuid, is_active, is_deleted, created_by, created_at, updated_by, updated_at)
VALUES ('Kyros Consultoria', 'KYROS', gen_random_uuid(), TRUE, FALSE, 'system', now(), 'system', now());


INSERT INTO systems (id_organization, external_code, description, url, scopes, uuid, is_active, is_deleted, created_by, created_at, updated_by, updated_at)
VALUES (1, 'SENTRYID', 'Sentry Identity Provider', 'sentry.auth.com.br', '...', gen_random_uuid(), TRUE, FALSE, 'system', now(), 'system', now());


INSERT INTO actions (cd_action, action_type, id_system, is_active, is_deleted, created_by, created_at, updated_by, updated_at)
VALUES ('SENTRY.USER.VIEW', 'SEARCH', 1, true, FALSE, 'system', now(), 'system', now());


INSERT INTO roles (id_system, id_organization, system_role, external_role, role_type, uuid, is_active, is_deleted, created_by, created_at, updated_by, updated_at)
VALUES (1, 1, 'SENTRY.ADMIN', 'KYROS.SENTRY.ADMIN', 'ACCESS', gen_random_uuid(), TRUE, FALSE, 'system', now(), 'system', now());


INSERT INTO role_actions (id_role, cd_action, is_active, is_deleted, created_by, created_at, updated_by, updated_at)
VALUES (1, 'SENTRY.USER.VIEW', TRUE, FALSE, 'system', now(), 'system', now());


INSERT INTO users (uuid, name, authentication_type, username, password, salt, email, is_active, is_deleted, created_by, created_at, updated_by, updated_at)
VALUES (gen_random_uuid(), 'Julian Degutis', 'DATABASE', 'juliandfgarcia', 'Exsdjhwh1', '3378', 'juliandfgarcia@gmail.com', TRUE, FALSE, 'system', now(), 'system', now());


INSERT INTO role_users (id_role, id_user, is_active, is_deleted, created_by, created_at, updated_by, updated_at)
VALUES (1, 1, TRUE, FALSE, 'system', now(), 'system', now());


