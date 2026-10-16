-- Solo lectura. Recuentos exactos en la copia restaurada, no en el origen activo.
SELECT 'areas' AS tabla, COUNT(*) AS filas FROM areas
UNION ALL SELECT 'empleados', COUNT(*) FROM empleados
UNION ALL SELECT 'tareas', COUNT(*) FROM tareas
UNION ALL SELECT 'catalogos_tareas', COUNT(*) FROM catalogos_tareas
UNION ALL SELECT 'equipos', COUNT(*) FROM equipos
UNION ALL SELECT 'planillas', COUNT(*) FROM planillas
UNION ALL SELECT 'asignaciones', COUNT(*) FROM asignaciones;
