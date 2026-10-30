-- Simulação de login: usuário juliang (senha Kyros@123) no sistema KYROSPORTAL, com o papel PORTAL.COLABORADOR e a
-- ação PORTAL.PONTO. Dados fictícios, só para ambiente local. Ver docs/simulacao-login-kyrosportal.md.
-- Idempotente: pode ser rodado de novo sem duplicar nada.
--
-- Os hashes são Argon2id no formato do PasswordService (m=19456, t=2, p=1):
--   senha do usuário        Kyros@123
--   segredo do sistema      kyros-portal-segredo-local

BEGIN;

-- 1. Organização dona do sistema
INSERT INTO organizations (name, external_code, uuid, is_active, is_deleted, created_by, created_at, updated_by, updated_at)
VALUES ('Kyros', 'KYROS', gen_random_uuid(), TRUE, FALSE, 'simulacao', now(), 'simulacao', now())
ON CONFLICT (external_code) DO NOTHING;

-- 2. Sistema KYROSPORTAL: o external_code é o client_id. Confidencial (tem segredo) e confiável (libera o login
--    direto, F1-11). A URI de LOGIN habilita também o fluxo PKCE pela tela de login.
INSERT INTO systems (id_organization, external_code, description, url, scopes, client_type, client_secret_hash,
                     is_trusted, uuid, is_active, is_deleted, created_by, created_at, updated_by, updated_at)
SELECT id_organization, 'KYROSPORTAL', 'Portal Kyros', 'http://localhost:4200', 'openid profile email offline_access',
       'CONFIDENTIAL',
       '{argon2}$argon2id$v=19$m=19456,t=2,p=1$hnTm0V9TwWYPGpGxEk9GNw$OAfHdW4oggIcCoJ1U5pRx2ZuBCi2yS3Ybhp8GaMcVv8',
       TRUE, gen_random_uuid(), TRUE, FALSE, 'simulacao', now(), 'simulacao', now()
FROM organizations WHERE external_code = 'KYROS'
ON CONFLICT (external_code) DO NOTHING;

INSERT INTO system_redirect_uris (id_system, uri, uri_type)
SELECT id_system, 'http://localhost:4200/callback', 'LOGIN' FROM systems WHERE external_code = 'KYROSPORTAL'
UNION ALL
SELECT id_system, 'http://localhost:4200/', 'LOGOUT' FROM systems WHERE external_code = 'KYROSPORTAL'
ON CONFLICT DO NOTHING;

-- 3. Permissão (ação) PORTAL.PONTO do sistema
INSERT INTO actions (cd_action, action_type, id_system, is_active, is_deleted, created_by, created_at, updated_by, updated_at)
SELECT 'PORTAL.PONTO', 'EXECUTE', id_system, TRUE, FALSE, 'simulacao', now(), 'simulacao', now()
FROM systems WHERE external_code = 'KYROSPORTAL'
ON CONFLICT (cd_action) DO NOTHING;

-- 4. Grupo (papel) PORTAL.COLABORADOR do sistema
INSERT INTO roles (id_system, id_organization, system_role, external_role, role_type, uuid, is_active, is_deleted,
                   created_by, created_at, updated_by, updated_at)
SELECT s.id_system, s.id_organization, 'PORTAL.COLABORADOR', 'KYROS.PORTAL.COLABORADOR', 'ACCESS', gen_random_uuid(),
       TRUE, FALSE, 'simulacao', now(), 'simulacao', now()
FROM systems s
WHERE s.external_code = 'KYROSPORTAL'
  AND NOT EXISTS (SELECT 1 FROM roles r WHERE r.id_system = s.id_system AND r.system_role = 'PORTAL.COLABORADOR');

-- 5. O grupo recebe a permissão
INSERT INTO role_actions (id_role, cd_action, is_active, is_deleted, created_by, created_at, updated_by, updated_at)
SELECT r.id_role, 'PORTAL.PONTO', TRUE, FALSE, 'simulacao', now(), 'simulacao', now()
FROM roles r JOIN systems s ON s.id_system = r.id_system
WHERE s.external_code = 'KYROSPORTAL' AND r.system_role = 'PORTAL.COLABORADOR'
  AND NOT EXISTS (SELECT 1 FROM role_actions ra WHERE ra.id_role = r.id_role AND ra.cd_action = 'PORTAL.PONTO');

-- 6. Usuário juliang, autenticado pelo banco (DATABASE)
INSERT INTO users (uuid, name, authentication_type, username, password, email, is_active, is_deleted,
                   created_by, created_at, updated_by, updated_at)
VALUES (gen_random_uuid(), 'Julian G', 'DATABASE', 'juliang',
        '{argon2}$argon2id$v=19$m=19456,t=2,p=1$BRAWg10+lW7ZkDyI/IBLhw$K1pf/A+l3n4No6Sk2Stm4SqI0wMHkZDyO2GpwIuF7OM',
        'juliang@kyros.local', TRUE, FALSE, 'simulacao', now(), 'simulacao', now())
ON CONFLICT (username) DO NOTHING;

-- 7. O usuário entra no grupo: é o que libera o login no KYROSPORTAL (RN-F1-02.5)
INSERT INTO role_users (id_role, id_user, is_active, is_deleted, created_by, created_at, updated_by, updated_at)
SELECT r.id_role, u.id_user, TRUE, FALSE, 'simulacao', now(), 'simulacao', now()
FROM roles r JOIN systems s ON s.id_system = r.id_system, users u
WHERE s.external_code = 'KYROSPORTAL' AND r.system_role = 'PORTAL.COLABORADOR' AND u.username = 'juliang'
  AND NOT EXISTS (SELECT 1 FROM role_users ru WHERE ru.id_role = r.id_role AND ru.id_user = u.id_user);

COMMIT;

-- Conferência: deve trazer uma linha com juliang / PORTAL.COLABORADOR / PORTAL.PONTO
SELECT u.username, u.uuid AS sub, s.external_code AS client_id, r.system_role, ra.cd_action
FROM users u
JOIN role_users ru ON ru.id_user = u.id_user AND ru.is_active AND NOT ru.is_deleted
JOIN roles r ON r.id_role = ru.id_role AND r.is_active AND NOT r.is_deleted
JOIN systems s ON s.id_system = r.id_system
LEFT JOIN role_actions ra ON ra.id_role = r.id_role AND ra.is_active AND NOT ra.is_deleted
WHERE u.username = 'juliang' AND s.external_code = 'KYROSPORTAL';
