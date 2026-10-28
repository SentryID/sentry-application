-- Dados fictícios para desenvolvimento. Só entra no perfil `dev` (ver application-dev.yaml).
-- Nenhuma senha aqui: o usuário de exemplo recebe senha pelo bootstrap/API, sempre com hash.
-- Idempotente: pode ser reaplicado pelo Flyway sempre que este arquivo mudar.

INSERT INTO organizations (name, external_code, uuid, is_active, is_deleted, created_by, created_at, updated_by, updated_at)
VALUES ('Acme Exemplo', 'ACME', gen_random_uuid(), TRUE, FALSE, 'seed', now(), 'seed', now())
ON CONFLICT (external_code) DO NOTHING;

INSERT INTO systems (id_organization, external_code, description, url, scopes, uuid, is_active, is_deleted, created_by, created_at, updated_by, updated_at)
SELECT id_organization, 'SYS001', 'Sistema de Exemplo', 'http://localhost:3000', 'openid profile email', gen_random_uuid(), TRUE, FALSE, 'seed', now(), 'seed', now()
FROM organizations WHERE external_code = 'ACME'
ON CONFLICT (external_code) DO NOTHING;

INSERT INTO actions (cd_action, action_type, id_system, is_active, is_deleted, created_by, created_at, updated_by, updated_at)
SELECT 'SYS001.REPORT.VIEW', 'SEARCH', id_system, TRUE, FALSE, 'seed', now(), 'seed', now()
FROM systems WHERE external_code = 'SYS001'
ON CONFLICT (cd_action) DO NOTHING;

INSERT INTO roles (id_system, id_organization, system_role, external_role, role_type, uuid, is_active, is_deleted, created_by, created_at, updated_by, updated_at)
SELECT s.id_system, s.id_organization, 'SYS001.VIEWER', 'ACME.SYS001.VIEWER', 'ACCESS', gen_random_uuid(), TRUE, FALSE, 'seed', now(), 'seed', now()
FROM systems s
WHERE s.external_code = 'SYS001'
  AND NOT EXISTS (SELECT 1 FROM roles r WHERE r.id_system = s.id_system AND r.system_role = 'SYS001.VIEWER');

INSERT INTO role_actions (id_role, cd_action, is_active, is_deleted, created_by, created_at, updated_by, updated_at)
SELECT r.id_role, 'SYS001.REPORT.VIEW', TRUE, FALSE, 'seed', now(), 'seed', now()
FROM roles r
WHERE r.system_role = 'SYS001.VIEWER'
  AND NOT EXISTS (SELECT 1 FROM role_actions ra WHERE ra.id_role = r.id_role AND ra.cd_action = 'SYS001.REPORT.VIEW');

INSERT INTO users (uuid, name, authentication_type, username, password, email, is_active, is_deleted, created_by, created_at, updated_by, updated_at)
VALUES (gen_random_uuid(), 'John Doe', 'DATABASE', 'john.doe', NULL, 'john.doe@example.com', TRUE, FALSE, 'seed', now(), 'seed', now())
ON CONFLICT (username) DO NOTHING;

INSERT INTO role_users (id_role, id_user, is_active, is_deleted, created_by, created_at, updated_by, updated_at)
SELECT r.id_role, u.id_user, TRUE, FALSE, 'seed', now(), 'seed', now()
FROM roles r, users u
WHERE r.system_role = 'SYS001.VIEWER'
  AND u.username = 'john.doe'
  AND NOT EXISTS (SELECT 1 FROM role_users ru WHERE ru.id_role = r.id_role AND ru.id_user = u.id_user);
