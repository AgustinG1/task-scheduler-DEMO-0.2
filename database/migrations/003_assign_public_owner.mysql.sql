-- Manual, MySQL 8.4. Requiere 001/002 y todas las escrituras detenidas.
-- Identidad comprobada mediante login real. No resuelve por email ni por id de H2.
-- El cliente debe detenerse al primer error (NO usar mysql --force).
SET NAMES utf8mb4;
DELIMITER $$
CREATE PROCEDURE auth_assert_ownership()
SQL SECURITY INVOKER
BEGIN
    IF EXISTS (
        SELECT owner_id FROM areas WHERE owner_id IS NULL
        UNION ALL SELECT owner_id FROM empleados WHERE owner_id IS NULL
        UNION ALL SELECT owner_id FROM tareas WHERE owner_id IS NULL
        UNION ALL SELECT owner_id FROM catalogos_tareas WHERE owner_id IS NULL
        UNION ALL SELECT owner_id FROM equipos WHERE owner_id IS NULL
        UNION ALL SELECT owner_id FROM planillas WHERE owner_id IS NULL
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Hay registros sin propietario';
    END IF;
    IF EXISTS (
        SELECT e.id FROM empleados e LEFT JOIN areas a ON a.id=e.area_id
        WHERE a.id IS NULL OR e.owner_id<>a.owner_id
        UNION ALL SELECT x.area_id FROM tarea_area x
        LEFT JOIN areas a ON a.id=x.area_id LEFT JOIN tareas t ON t.id=x.tarea_id
        WHERE a.id IS NULL OR t.id IS NULL OR a.owner_id<>t.owner_id
        UNION ALL SELECT x.catalogo_id FROM catalogo_tarea_detalle x
        LEFT JOIN catalogos_tareas c ON c.id=x.catalogo_id LEFT JOIN tareas t ON t.id=x.tarea_id
        WHERE c.id IS NULL OR t.id IS NULL OR c.owner_id<>t.owner_id
        UNION ALL SELECT x.equipo_id FROM equipo_area x
        LEFT JOIN equipos e ON e.id=x.equipo_id LEFT JOIN areas a ON a.id=x.area_id
        WHERE e.id IS NULL OR a.id IS NULL OR e.owner_id<>a.owner_id
        UNION ALL SELECT x.equipo_id FROM equipo_catalogo x
        LEFT JOIN equipos e ON e.id=x.equipo_id LEFT JOIN catalogos_tareas c ON c.id=x.catalogo_id
        WHERE e.id IS NULL OR c.id IS NULL OR e.owner_id<>c.owner_id
        UNION ALL SELECT x.empleado_id FROM empleado_equipo x
        LEFT JOIN empleados p ON p.id=x.empleado_id LEFT JOIN equipos e ON e.id=x.equipo_id
        WHERE p.id IS NULL OR e.id IS NULL OR p.owner_id<>e.owner_id
        UNION ALL SELECT x.tarea_id FROM tarea_equipo x
        LEFT JOIN tareas t ON t.id=x.tarea_id LEFT JOIN equipos e ON e.id=x.equipo_id
        WHERE t.id IS NULL OR e.id IS NULL OR t.owner_id<>e.owner_id
        UNION ALL SELECT p.id FROM planillas p LEFT JOIN equipos e ON e.id=p.equipo_id
        WHERE p.equipo_id IS NOT NULL AND (e.id IS NULL OR p.owner_id<>e.owner_id)
        UNION ALL SELECT x.id FROM asignaciones x
        LEFT JOIN planillas p ON p.id=x.planilla_id LEFT JOIN empleados e ON e.id=x.empleado_id
        LEFT JOIN tareas t ON t.id=x.tarea_id
        WHERE p.id IS NULL OR e.id IS NULL OR p.owner_id<>e.owner_id
            OR (x.tarea_id IS NOT NULL AND (t.id IS NULL OR p.owner_id<>t.owner_id))
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Relacion huerfana o entre propietarios distintos';
    END IF;
    IF EXISTS (SELECT owner_id FROM planillas WHERE estado='ACTIVE' GROUP BY owner_id HAVING COUNT(*)>1) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Mas de una planilla activa por usuario';
    END IF;
END$$

CREATE PROCEDURE auth_assign_public_owner()
SQL SECURITY INVOKER
BEGIN
    DECLARE public_issuer VARCHAR(512) DEFAULT 'https://dev-ddgeakv1hckqsd7z.us.auth0.com/';
    DECLARE public_subject VARCHAR(255) DEFAULT 'auth0|6ad1611ca88ce64aa99f1dda';
    DECLARE public_key CHAR(64);
    DECLARE public_id BIGINT;
    DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;
    -- SHA256: longitud UTF-8 issuer en cuatro bytes big endian + issuer + subject.
    -- Mismo formato que AppUserService.identityKey, NO SHA256(issuer || subject).
    SET public_key=LOWER(SHA2(CONCAT(UNHEX(LPAD(HEX(OCTET_LENGTH(public_issuer)),8,'0')),
        CAST(public_issuer AS BINARY),CAST(public_subject AS BINARY)),256));
    START TRANSACTION;
    IF EXISTS (SELECT id FROM app_user WHERE
        (identity_key=public_key AND (CAST(issuer AS BINARY)<>CAST(public_issuer AS BINARY)
            OR CAST(subject AS BINARY)<>CAST(public_subject AS BINARY) OR active<>1))
        OR (CAST(issuer AS BINARY)=CAST(public_issuer AS BINARY)
            AND CAST(subject AS BINARY)=CAST(public_subject AS BINARY) AND identity_key<>public_key)) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Identidad publica incompatible o inactiva';
    END IF;
    INSERT INTO app_user(identity_key,issuer,subject,email,display_name,active,created_at,version)
    SELECT public_key,public_issuer,public_subject,'demo@account.example','Cuenta publica',1,UTC_TIMESTAMP(6),0
    WHERE NOT EXISTS(SELECT id FROM app_user WHERE identity_key=public_key);
    SELECT id INTO public_id FROM app_user WHERE identity_key=public_key FOR UPDATE;
    UPDATE areas SET owner_id=public_id WHERE owner_id IS NULL;
    UPDATE empleados SET owner_id=public_id WHERE owner_id IS NULL;
    UPDATE tareas SET owner_id=public_id WHERE owner_id IS NULL;
    UPDATE catalogos_tareas SET owner_id=public_id WHERE owner_id IS NULL;
    UPDATE equipos SET owner_id=public_id WHERE owner_id IS NULL;
    UPDATE planillas SET owner_id=public_id WHERE owner_id IS NULL;
    CALL auth_assert_ownership();
    COMMIT;
END$$
DELIMITER ;
CALL auth_assign_public_owner();
DROP PROCEDURE auth_assign_public_owner;
-- auth_assert_ownership se conserva hasta finalizar 004.
