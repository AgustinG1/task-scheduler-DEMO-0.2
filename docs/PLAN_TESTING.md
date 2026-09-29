# Estado de las pruebas JUnit

Actualizado el 29 de septiembre de 2026.

`mvn test -B -ntp` ejecutó **24 pruebas: 24 correctas, 0 fallidas, 0 omitidas**. Hay 12 ejecuciones de integración y contexto y 12 unitarias. `AssignmentAlgorithmUnitTests` aporta diez casos del motor; `EmployeeServiceTests`, dos casos del ciclo de vida de empleados.

Las dos pruebas que fallaban en la primera revisión ahora pasan: una tarea específica compatible se cubre cada semana y cero semanas se rechaza antes de archivar una planilla previa. Se añadieron dos pruebas adicionales para semanas negativas o superiores a 52 y para tareas específicas con autorizaciones superpuestas que requieren redistribuir personas. También siguen pasando las pruebas de rotación, autorización por área, asignación única semanal, archivo global, Excel, validación de listas vacías y carga del contexto.

## Pruebas unitarias del motor

La suite unitaria simula `FeasibilityValidator` y los cinco repositorios inyectados. Comprueba:

- rechazo de `-1`, `0` y `53` semanas antes de consultar dependencias;
- ausencia de archivado o guardado cuando falla la validación de factibilidad;
- filtrado de empleados por áreas del equipo y eliminación de tareas duplicadas entre catálogos;
- archivado de la planilla activa antes de guardar la nueva;
- fechas, equipo y estado de la nueva planilla;
- cobertura completa mediante reasignación cuando las autorizaciones se superponen;
- una tarea y dos descansos por semana, con rotación para tres personas;
- cambio de tarea cuando una persona dispone de una alternativa dentro de su ciclo;
- repetición de la única tarea cuando no existe alternativa.

Todas estas reglas pasan. La última prueba detectó inicialmente que, con una persona y dos tareas generales, el motor asignaba la misma tarea en las semanas 1 y 2. La causa era la prioridad de `tareasPendientes`: una tarea con cero candidatos ideales se ordenaba antes que otra con un candidato ideal; después la capa de emergencia volvía a asignar la tarea anterior.

La corrección clasifica primero las tareas con candidatos ideales, después las tareas autorizadas que requieren relajar el ciclo y al final las tareas sin personas autorizadas. Conserva dentro de cada nivel la prioridad por escasez y el desempate de tareas específicas. La prueba permanece como regresión y ahora pasa.

El motor conserva su diseño: intercalado aleatorio por área, rotación semanal, prioridad a tareas con pocos candidatos, historial de ciclos y tres niveles de preferencia para personas. La búsqueda interna de reasignación evita huecos cuando una combinación completa es posible. No depende de una librería externa para esa búsqueda.

## Pruebas del retiro de empleados

`EmployeeServiceTests` comprueba que las pantallas reciben únicamente empleados activos y que retirar una persona cambia `active` a `false` sin invocar `delete` ni `deleteById`. La prueba de integración `deactivatesEmployeeWithoutBreakingHistoricalAssignments` genera una planilla, persiste una asignación, desactiva a la persona, fuerza el guardado y confirma que tanto el empleado inactivo como la asignación histórica continúan en la base.

## Límites aún abiertos

Cuando la configuración no permite cubrir todas las tareas, el motor genera cobertura parcial sin registrar ni señalar cuáles faltaron. No se ha fijado el contrato de producto para ese caso. La preferencia de variedad puede ceder cuando conservar cobertura obliga a relajar el ciclo. Tampoco se han probado aún solicitudes de generación concurrentes, el esquema MySQL real ni la presentación HTTP de todos los errores de configuración.

Los próximos casos de prueba útiles son: tareas específicas sin áreas autorizadas; más tareas que personas; y repetición matemáticamente inevitable. Esos casos deben formular primero la expectativa de negocio.

Desde la carpeta con `pom.xml`:

```powershell
.\mvnw.cmd test
```

El reporte detallado se guarda en `target/surefire-reports/`.
