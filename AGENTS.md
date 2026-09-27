# Memoria del proyecto

Antes de hacer cambios, leer `MEMORIA_PROYECTO.md`. Para modificar el motor, leer también `docs/ALGORITMO.md`.
La evidencia de las comprobaciones está en `docs/VERIFICACION.md`; el inventario de la versión analizada, en `docs/INVENTARIO_ARCHIVOS.md`.
La topología real de producción, los identificadores operativos y la relación Render–Aiven están en `docs/DESPLIEGUE.md`. No copiar valores secretos desde los paneles a archivos del proyecto.
La suite JUnit y las correcciones posteriores se resumen en `docs/PLAN_TESTING.md`. Los dos fallos iniciales quedaron resueltos el 27 de septiembre de 2026. Conservar las pruebas de regresión y la prioridad de cobertura compatible.
La prueba unitaria `avoidsRepeatingATaskWhenTheEmployeeHasAnAlternative` protege la prioridad del ciclo corregida: no desactivarla ni cambiar su expectativa.

Esta memoria corresponde a `main` en `a6c203360fcd1da6d218bb849782019c6d8fa057`, revisado el 26 de septiembre de 2026. Contrastar con el código actual y actualizar la documentación cuando cambien reglas, arquitectura o configuración.

`analysis_results.md`, `intro.docx` y `service/algoritmoantes.txt` son antecedentes; contienen afirmaciones o propuestas que no describen exactamente la implementación vigente.
Para pruebas locales usar el perfil `local` (H2 en memoria). El perfil predeterminado es `prod` y requiere variables de conexión externas. `data/taskdb.mv.db` es una base histórica incluida, no la base del perfil `local` actual.
