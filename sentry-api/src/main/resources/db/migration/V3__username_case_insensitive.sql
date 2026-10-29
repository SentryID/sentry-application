-- RN-F1-02.1: o login não diferencia maiúsculas; o banco garante que não existam dois usernames iguais assim
CREATE UNIQUE INDEX users_username_lower_key ON users (lower(username));
