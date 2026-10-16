-- Expansión preparatoria para MySQL 8, con acceso público cerrado y respaldo previo.
-- Revisar la línea base real: no ejecutar si estas columnas/índices ya existen.
-- Los DDL de MySQL realizan commits implícitos; este archivo no es transaccional.
-- No asigna datos. El backfill y los NOT NULL se prepararán para el issuer/subject
-- concretos de la cuenta pública antes de reabrir el servicio.
ALTER TABLE areas ADD COLUMN owner_id BIGINT NULL,
    ADD INDEX ix_areas_owner (owner_id),
    ADD CONSTRAINT fk_areas_owner FOREIGN KEY (owner_id) REFERENCES app_user(id);
ALTER TABLE empleados ADD COLUMN owner_id BIGINT NULL,
    ADD INDEX ix_empleados_owner_active (owner_id, active),
    ADD CONSTRAINT fk_empleados_owner FOREIGN KEY (owner_id) REFERENCES app_user(id);
ALTER TABLE tareas ADD COLUMN owner_id BIGINT NULL,
    ADD INDEX ix_tareas_owner (owner_id),
    ADD CONSTRAINT fk_tareas_owner FOREIGN KEY (owner_id) REFERENCES app_user(id);
ALTER TABLE catalogos_tareas ADD COLUMN owner_id BIGINT NULL,
    ADD INDEX ix_catalogos_owner (owner_id),
    ADD CONSTRAINT fk_catalogos_owner FOREIGN KEY (owner_id) REFERENCES app_user(id);
ALTER TABLE equipos ADD COLUMN owner_id BIGINT NULL,
    ADD CONSTRAINT fk_equipos_owner FOREIGN KEY (owner_id) REFERENCES app_user(id),
    ADD CONSTRAINT uk_equipo_owner_nombre UNIQUE (owner_id, nombre);
ALTER TABLE planillas ADD COLUMN owner_id BIGINT NULL,
    ADD INDEX ix_planillas_owner_estado (owner_id, estado),
    ADD CONSTRAINT fk_planillas_owner FOREIGN KEY (owner_id) REFERENCES app_user(id);

-- Antes de reabrir: identificar mediante SHOW INDEX FROM equipos y retirar el
-- índice UNIQUE antiguo que cubra solamente nombre. No adivinar su nombre.
