-- Manual, MySQL 8.4. Despues de 003; app cerrada hasta verificar todo.
-- DDL NO transaccional. Un fallo exige diagnostico/restauracion, no reintento ciego.
CALL auth_assert_ownership();
DELIMITER $$
CREATE PROCEDURE auth_assert_team_indexes()
SQL SECURITY INVOKER
BEGIN
    -- Nombre y forma observados en la copia real del 15 de octubre de 2026.
    IF (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE()
        AND table_name='equipos' AND index_name='UK4r8c31cj9qstoo4ra6ud41beh'
        AND non_unique=0 AND column_name='nombre' AND seq_in_index=1 AND sub_part IS NULL)<>1
        OR (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE()
        AND table_name='equipos' AND index_name='UK4r8c31cj9qstoo4ra6ud41beh')<>1 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El indice global de equipos no coincide con la linea base';
    END IF;
    IF (SELECT GROUP_CONCAT(column_name ORDER BY seq_in_index) FROM information_schema.statistics
        WHERE table_schema=DATABASE() AND table_name='equipos'
        AND index_name='uk_equipo_owner_nombre' AND non_unique=0 AND sub_part IS NULL)<>'owner_id,nombre'
        OR (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE()
        AND table_name='equipos' AND index_name='uk_equipo_owner_nombre')<>2 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Falta el indice unico por propietario y nombre';
    END IF;
END$$
DELIMITER ;
CALL auth_assert_team_indexes();
ALTER TABLE areas MODIFY owner_id BIGINT NOT NULL;
ALTER TABLE empleados MODIFY owner_id BIGINT NOT NULL;
ALTER TABLE tareas MODIFY owner_id BIGINT NOT NULL;
ALTER TABLE catalogos_tareas MODIFY owner_id BIGINT NOT NULL;
ALTER TABLE equipos MODIFY owner_id BIGINT NOT NULL;
ALTER TABLE planillas MODIFY owner_id BIGINT NOT NULL;
ALTER TABLE equipos DROP INDEX UK4r8c31cj9qstoo4ra6ud41beh;
DROP PROCEDURE auth_assert_team_indexes;
DROP PROCEDURE auth_assert_ownership;
