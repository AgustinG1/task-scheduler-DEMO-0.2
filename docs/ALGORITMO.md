# Algoritmo actual de asignación

Actualizado el 27 de septiembre de 2026. Implementación: `AssignmentAlgorithm.generatePayroll`.

## Entradas y selección

El servicio admite de 1 a 52 semanas. Rechaza otros valores antes de leer o modificar planillas. Obtiene el equipo por ID, incluye a los empleados activos cuya área está entre las áreas del equipo y reúne las tareas distintas de sus catálogos. `FeasibilityValidator` exige al menos una persona y una tarea. Al generar una nueva planilla, archiva todas las activas del sistema dentro de la misma transacción; la regla de planilla activa es global.

## Orden de la plantilla y rotación

Agrupa las personas por área, baraja cada grupo e intercala una persona de cada área. En cada semana rota la lista con un paso `max(1, empleados - min(empleados, tareas))`. Este orden da preferencia a distintas personas a lo largo de las semanas. Ninguna persona queda apartada de antemano para descanso: todas pueden optar a una tarea autorizada.

## Prioridad de tareas y candidatos

Baraja las tareas y luego ordena por: menos candidatos ideales, menos candidatos autorizados y tareas específicas antes de generales. Un candidato ideal está autorizado, no hizo esa tarea en su última asignación y todavía no la hizo en su ciclo actual.

Para cada tarea, ordena a las personas autorizadas según las tres capas originales: (1) ideal, (2) no repite la última tarea aunque ya esté en su ciclo y (3) cualquier persona autorizada. Desempata por menor cantidad de tareas en el ciclo y por el orden rotado de la semana.

El motor busca una persona libre para la tarea. Si la candidata preferida ya tiene otra, intenta mover esa otra tarea a una persona distinta mediante una búsqueda recursiva con control de visitados. Solo deja la tarea sin asignar cuando no existe una cadena de cambios que aumente el número de tareas cubiertas. Es una extensión local del algoritmo; no se incorporó ninguna librería de optimización.

La búsqueda maximiza la cantidad de tareas cubiertas cada semana según las autorizaciones. Las preferencias de rotación y variedad son criterios de selección, no garantías absolutas: pueden ceder para conservar cobertura. Si la configuración hace imposible cubrir todas las tareas —por ejemplo, hay más tareas que personas o varias tareas dependen de una sola persona— la planilla conserva el comportamiento anterior de cobertura parcial. Todavía no almacena explícitamente qué tareas quedaron sin cubrir.

## Persistencia e historial

Cada persona recibe exactamente un registro por semana: tarea con estado `ASSIGNED` o descanso con estado `REST` y tarea nula. No se asigna una tarea a más de una persona en la misma semana. Después de decidir la semana completa, el motor actualiza la última tarea y el conjunto de tareas del ciclo de cada persona asignada. Cuando la persona ha hecho todas las tareas para las que está autorizada, vacía su conjunto de ciclo; la última tarea permanece para intentar evitar una repetición. El historial solo vive durante la generación actual.

La operación completa usa `@Transactional`. Un fallo revierte el archivado y las nuevas asignaciones. Esto no establece exclusión entre generaciones concurrentes; tampoco existe una restricción de base que imponga una única planilla activa.

## Comprobación

`AssignmentAlgorithmIntegrationTests` verifica autorización por área, una asignación por persona y semana, rotación de descanso, cobertura de una tarea específica, cobertura con autorizaciones superpuestas, límites de semanas, archivado y exportación Excel. Ejecutar `mvn test` desde la carpeta que contiene `pom.xml`.
