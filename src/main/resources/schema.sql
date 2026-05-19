CREATE TABLE IF NOT EXISTS catalogo_tarea_detalle (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    catalogo_id BIGINT NOT NULL,
    tarea_id BIGINT NOT NULL,
    UNIQUE KEY uk_catalogo_tarea (catalogo_id, tarea_id)
);

CREATE TABLE IF NOT EXISTS tarea_equipo (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tarea_id BIGINT NOT NULL,
    equipo_id BIGINT NOT NULL,
    UNIQUE KEY uk_tarea_equipo (tarea_id, equipo_id)
);

CREATE TABLE IF NOT EXISTS empleado_equipo (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    empleado_id BIGINT NOT NULL,
    equipo_id BIGINT NOT NULL,
    UNIQUE KEY uk_empleado_equipo (empleado_id, equipo_id)
);

CREATE TABLE IF NOT EXISTS equipo_area (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    equipo_id BIGINT NOT NULL,
    area_id BIGINT NOT NULL,
    UNIQUE KEY uk_equipo_area (equipo_id, area_id)
);

CREATE TABLE IF NOT EXISTS equipo_catalogo (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    equipo_id BIGINT NOT NULL,
    catalogo_id BIGINT NOT NULL,
    UNIQUE KEY uk_equipo_catalogo (equipo_id, catalogo_id)
);