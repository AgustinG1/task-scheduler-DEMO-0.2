# Evolución del proyecto

Este registro conserva cada etapa funcional del programa. Cada entrada indica el comportamiento añadido o corregido, los archivos principales, la comprobación realizada y si el cambio llegó a GitHub o producción. No debe contener contraseñas, cadenas de conexión completas ni otros secretos.

## 27 de septiembre de 2026 — Entorno Docker Compose reproducible

Estado: cambio preparado y verificado localmente; se registra en el commit que contiene esta entrada. No se ha enviado a GitHub ni desplegado en Render.

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
- Estado remoto: commit local todavía no enviado a GitHub ni desplegado en Render.

## 27 de septiembre de 2026 — Cobertura compatible, validación y documentación técnica

Commit local: `95ec136` — `Corregir motor y documentar despliegue`.

- Se validó el rango de 1 a 52 semanas antes de modificar planillas.
- Los descansos pasaron a decidirse después de evaluar la cobertura compatible.
- Se añadió reasignación interna para evitar huecos cuando existe una combinación completa.
- Se incorporaron pruebas de integración, documentación del algoritmo, inventario y memoria técnica.
- Se documentó la arquitectura real GitHub–Render–Aiven sin guardar secretos.
- Verificación de esa etapa: 11 pruebas correctas, 0 fallidas.
- Estado remoto: commit local todavía no enviado a GitHub ni desplegado en Render.
