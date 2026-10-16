-- Inventario de solo lectura para preparar la migracion de propiedad.
-- MySQL 8.4. Seleccionar previamente la base de TaskScheduler en el cliente.
-- No ejecuta DDL, backfill ni cambios de configuracion; no consulta datos personales.
-- TABLE_ROWS es una estimacion InnoDB, no un recuento para validar un backfill.
-- Si DATABASE() es NULL o apunta a otra base, detenerse y seleccionar la correcta.

SELECT DATABASE() AS selected_database,
       VERSION() AS mysql_version,
       @@character_set_database AS database_character_set,
       @@collation_database AS database_collation;

SELECT table_name, engine, table_collation, table_rows AS estimated_rows
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name IN ('app_user', 'areas', 'empleados', 'tareas',
    'catalogos_tareas', 'equipos', 'planillas', 'asignaciones', 'tarea_area',
    'catalogo_tarea_detalle', 'equipo_area', 'equipo_catalogo',
    'empleado_equipo', 'tarea_equipo')
ORDER BY table_name;

SELECT table_name, ordinal_position, column_name, column_type,
       is_nullable, column_default, character_set_name, collation_name, extra
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name IN ('app_user', 'areas', 'empleados', 'tareas',
    'catalogos_tareas', 'equipos', 'planillas', 'asignaciones', 'tarea_area',
    'catalogo_tarea_detalle', 'equipo_area', 'equipo_catalogo',
    'empleado_equipo', 'tarea_equipo')
ORDER BY table_name, ordinal_position;

SELECT table_name, index_name, non_unique, seq_in_index,
       column_name, sub_part, index_type, is_visible, expression
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND table_name IN ('app_user', 'areas', 'empleados', 'tareas',
    'catalogos_tareas', 'equipos', 'planillas', 'asignaciones', 'tarea_area',
    'catalogo_tarea_detalle', 'equipo_area', 'equipo_catalogo',
    'empleado_equipo', 'tarea_equipo')
ORDER BY table_name, index_name, seq_in_index;

SELECT table_name, constraint_name, ordinal_position, column_name,
       referenced_table_schema, referenced_table_name, referenced_column_name
FROM information_schema.key_column_usage
WHERE table_schema = DATABASE()
  AND table_name IN ('app_user', 'areas', 'empleados', 'tareas',
    'catalogos_tareas', 'equipos', 'planillas', 'asignaciones', 'tarea_area',
    'catalogo_tarea_detalle', 'equipo_area', 'equipo_catalogo',
    'empleado_equipo', 'tarea_equipo')
ORDER BY table_name, constraint_name, ordinal_position;

-- Candidatos al indice unico global antiguo de equipos.nombre.
-- Solo identifica: NO genera ni ejecuta DROP INDEX.
SELECT index_name, MIN(column_name) AS column_name,
       MIN(sub_part) AS prefix_length
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND table_name = 'equipos' AND non_unique = 0 AND index_name <> 'PRIMARY'
GROUP BY index_name
HAVING COUNT(*) = 1 AND MIN(column_name) = 'nombre'
ORDER BY index_name;
