# Evolución del proyecto

Este registro conserva cada etapa funcional del programa. Cada entrada indica el comportamiento añadido o corregido, los archivos principales, la comprobación realizada y si el cambio llegó a GitHub o producción. No debe contener contraseñas, cadenas de conexión completas ni otros secretos.

## 29 de septiembre de 2026 — Retiro seguro de empleados con historial

Commit publicado: `f13f26c` — `Evitar error al retirar empleados con historial`. Despliegue `dep-datiqk3bc2fs73bhkvhg` verificado como `live` en Render el 29 de septiembre de 2026 a las 03:18:32 UTC.

- El incidente se reprodujo en producción al abrir `/employees/eliminar/5`. El log de Render de las 03:04:39 UTC confirmó `DataIntegrityViolationException`: MySQL impedía borrar el empleado porque `asignaciones.empleado_id` conserva una clave foránea hacia `empleados.id`.
- Se eliminó el borrado físico del flujo de empleados. La interfaz ahora envía `POST /employees/desactivar/{id}`, marca `active=false` y conserva las asignaciones históricas.
- Las listas de Empleados y de edición de Equipos muestran solo personal activo, por lo que la persona retirada desaparece de la operación diaria sin alterar planillas anteriores.
- La pantalla confirma la acción, explica que se preservará el historial y muestra un mensaje de éxito después de completarla.
- Se añadieron dos pruebas unitarias de `EmployeeService` y una prueba de integración con una asignación persistida.
- Verificación local: `mvn test -B -ntp`, 24 pruebas correctas, 0 fallidas; `git diff --check` sin errores.
- Verificación pública: `/employees` respondió HTTP 200, contiene formularios de desactivación por POST, no contiene enlaces `/employees/eliminar/` y Render no registró errores desde el inicio del despliegue.

## 27 de septiembre de 2026 — Lombok seguro en entidades JPA

Commit publicado: `0009945` — `Reemplazar Data en entidades JPA`. Enviado a `origin/main` el 28 de septiembre de 2026. El despliegue posterior en Render aún no se ha verificado.

- Se reemplazó `@Data` por `@Getter` y `@Setter` en las ocho entidades de `model/`.
- Se conservaron `@NoArgsConstructor` y `@AllArgsConstructor`, por lo que no cambió la forma de construir las entidades.
- Las relaciones JPA dejaron de formar parte automáticamente de `equals`, `hashCode` y `toString`, evitando recursión entre asociaciones bidireccionales y cargas accidentales de colecciones.
- `TaskAreaId`, como clave compuesta `@Embeddable`, conserva `@EqualsAndHashCode` además de getters y setters para cumplir el contrato de identidad requerido por JPA.
- Se actualizaron `MEMORIA_PROYECTO.md` y `analysis_results.md` para marcar el hallazgo como resuelto.
- Verificación: `mvn test -B -ntp`, 21 pruebas correctas, 0 fallidas; búsqueda del paquete `model` sin usos restantes de `@Data`; `git diff --check` sin errores.

## 27 de septiembre de 2026 — Entorno Docker Compose reproducible

Commit publicado: `449b99b` — `Completar entorno Docker Compose y documentar evolucion`. Enviado a `origin/main` el 28 de septiembre de 2026. El despliegue posterior en Render aún no se ha verificado.

- Se confirmó que `pom.xml` contiene una sola dependencia `com.mysql:mysql-connector-j` con alcance `runtime`. La duplicación descrita en `analysis_results.md` correspondía a una versión anterior y ya no existe; se conserva el único conector necesario para MySQL.
- `docker-compose.yml` define los servicios `mysql` y `app`, una red privada implícita, espera por healthcheck, persistencia mediante `mysql_data` y publicación configurable del puerto HTTP.
- Las credenciales de demostración pueden sustituirse mediante `.env`; `.env.example` contiene valores locales no secretos y `.env` permanece fuera de Git.
- Se añadió `.dockerignore` para excluir historial Git, compilaciones, datos locales, configuración del editor y documentación que no participa en la imagen.
- El perfil `docker` continúa formando la URL JDBC con `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME` y `DB_PASSWORD`.
- Se actualizaron `README.md`, `docs/DESPLIEGUE.md`, `MEMORIA_PROYECTO.md` y el análisis histórico para reflejar el estado actual.
- Verificación: el árbol efectivo de Maven muestra únicamente `com.mysql:mysql-connector-j:9.7.0:runtime`; la suite completa terminó con 21 pruebas correctas y 0 fallidas; SnakeYAML 2.5 leyó la raíz de Compose con `name`, `services` y `volumes`; `git diff --check` no detectó errores.
- Limitación: la máquina de trabajo no tiene disponible el comando `docker`, por lo que no se pudo ejecutar `docker compose config`, construir la imagen ni iniciar los contenedores en esta etapa.

## 27 de septiembre de 2026 — Tests unitarios y protección del ciclo

Commit local: `d9a2672` — `Añadir tests unitarios y corregir ciclo de tareas`.

- Se añadieron diez ejecuciones unitarias de `AssignmentAlgorithm` con JUnit y Mockito.
- Se corrigió la prioridad que repetía una tarea aunque hubiera otra alternativa disponible.
- Se conservó la repetición cuando es matemáticamente inevitable y la búsqueda interna de máxima cobertura.
- Verificación: `mvn test -B -ntp`, 21 pruebas correctas, 0 fallidas.
- Estado remoto: enviado a `origin/main` el 28 de septiembre de 2026; despliegue posterior en Render aún no verificado.

## 27 de septiembre de 2026 — Cobertura compatible, validación y documentación técnica

Commit local: `95ec136` — `Corregir motor y documentar despliegue`.

- Se validó el rango de 1 a 52 semanas antes de modificar planillas.
- Los descansos pasaron a decidirse después de evaluar la cobertura compatible.
- Se añadió reasignación interna para evitar huecos cuando existe una combinación completa.
- Se incorporaron pruebas de integración, documentación del algoritmo, inventario y memoria técnica.
- Se documentó la arquitectura real GitHub–Render–Aiven sin guardar secretos.
- Verificación de esa etapa: 11 pruebas correctas, 0 fallidas.
- Estado remoto: enviado a `origin/main` el 28 de septiembre de 2026; despliegue posterior en Render aún no verificado.
