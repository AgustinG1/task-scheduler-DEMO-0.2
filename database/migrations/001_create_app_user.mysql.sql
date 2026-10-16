-- Preparación para el despliegue futuro; no se ejecuta automáticamente.
-- Requiere revisar primero la línea base del esquema real y contar con respaldo.
-- No asigna los datos históricos: eso requiere identificar la cuenta pública
-- por issuer + subject y la siguiente migración de propiedad.
CREATE TABLE app_user (
    id BIGINT NOT NULL AUTO_INCREMENT,
    identity_key VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    issuer VARCHAR(512) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    email VARCHAR(320) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL,
    created_at DATETIME(6) NOT NULL,
    version BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_app_user_identity UNIQUE (identity_key)
);
