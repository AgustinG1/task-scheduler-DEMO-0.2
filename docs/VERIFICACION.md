# Evidencia de la revisión del proyecto

Fecha: 26 de septiembre de 2026. Código: `main`, commit `a6c203360fcd1da6d218bb849782019c6d8fa057`.

## Correspondencia con GitHub

Se obtuvo el árbol completo mediante la conexión integrada de GitHub. Contiene 71 archivos y no estaba truncado. Se calculó el hash Git de cada archivo local con el encabezado de blob y sus bytes. Resultado: 70 coincidencias exactas y una coincidencia después de normalizar finales de línea, `mvnw.cmd`. No había archivos originales adicionales ni faltantes.

La carpeta descargada carece de `.git`, por lo que no se creó commit, rama ni pull request. El inventario conserva hashes SHA-256 locales y hashes de los blobs remotos para futuras comparaciones. La revisión no cambió los archivos originales.

## Compilación y prueba existente

Comando ejecutado: `mvn test -B -ntp`, en la carpeta de `pom.xml`.

Entorno: Windows 11, Java 22.0.1, Maven 3.9.11. Configuración del compilador: `release 17`. Perfil de la prueba: `local`, fijado por `@ActiveProfiles("local")`.

Resultado: **BUILD SUCCESS**, una prueba ejecutada, cero fallos, cero errores, cero omitidas. El contexto de Spring Boot 4.0.6 arrancó con ocho repositorios JPA y H2 en memoria. La prueba `contextLoads()` no tiene aserciones de las reglas del algoritmo.

La primera comprobación en modo sin conexión no pudo resolver el padre de Maven. Tras autorizar la resolución normal de dependencias, se completó la compilación y prueba. No se modificó el POM para lograrlo.

## Comprobación HTTP con datos temporales

Se arrancaron las clases compiladas con perfil `local`, H2 en memoria y servidor limitado a `127.0.0.1` en un puerto asignado automáticamente. La base histórica y MySQL no se conectaron a esa instancia. El proceso se detuvo al terminar y se comprobó que el puerto ya no escuchaba.

Las ocho páginas principales devolvieron HTTP 200: inicio, áreas, empleados, tareas, catálogos, equipos, planilla e historial. Se crearon datos ficticios mediante los formularios HTTP: área, empleado, tarea general, catálogo, equipo y relaciones. Las altas y actualizaciones completaron sus redirecciones y mostraron páginas HTTP 200. La generación de dos semanas también completó la redirección correctamente.

Esto valida renderizado de esas rutas y un recorrido básico del servidor; no constituye una inspección visual de CSS, interacción en navegador ni prueba exhaustiva de todos los formularios.

## Casos de negocio observados

### Repetición consecutiva

Con un empleado activo, una tarea general y dos semanas, se generó el mismo empleado en esa tarea en ambas semanas. El archivo descargado se abrió como ZIP OOXML y se leyeron sus celdas, verificando que era un Excel real. Este caso confirma que el nivel 3 permite repetición.

### Cobertura incompleta con especialista disponible en el equipo

Con tres empleados (uno en área A, dos en área B), una tarea específica de A y una general, se generaron tres semanas. La matriz exportada mostró:

| Semana | Específica A | General | Descansos |
|---|---|---|---|
| 1 | Empleado A | B1 | B2 |
| 2 | `-` | B2 | B1 y A |
| 3 | Empleado A | B2 | B1 |

La segunda semana dejó sin cubrir la específica mientras la persona autorizada descansaba. Las asignaciones respetaron las áreas: el problema es el descanso prefijado y la falta de reconsideración de candidatos.

### Cero semanas y cambio de equipo

Se creó un segundo equipo con las mismas áreas y catálogo y se envió `POST /payroll/generar` con `semanas=0`. La petición terminó correctamente. El historial mostró dos planillas anteriores archivadas y una nueva activa con inicio y fin el mismo día. La pantalla activa identificó al segundo equipo.

Se confirmó tanto la falta de validación de duración en servidor como el archivado global entre equipos. El archivado global coincide con el objetivo original de mantener una única planilla activa; no se presume que el requisito de negocio deba ser uno por equipo.

### Descarga de planilla inexistente

`GET /api/payrolls/99999/download` devolvió un libro con 17 filas: una cabecera y 16 semanas vacías. No había una planilla con ese ID en la base temporal. El servicio usa las tareas globales y la duración alternativa cuando no encuentra asignaciones.

## Base histórica incluida

Se copió `data/taskdb.mv.db` a una ruta temporal y se abrió con H2 2.4.240, `ACCESS_MODE_DATA=r` e `IFEXISTS=TRUE`. Se consultaron metadatos y agregados; no se extrajeron a la memoria nombres del personal ni filas completas.

| Tabla | Registros |
|---|---:|
| `areas` | 3 |
| `empleados` | 15 |
| `tareas` | 13 |
| `equipos` | 2 |
| `catalogos_tareas` | 2 |
| `planillas` | 13 |
| `asignaciones` | 1.000 |
| `tarea_area` | 5 |
| `equipo_area` | 3 |
| `equipo_catalogo` | 2 |
| `catalogo_tarea_detalle` | 13 |
| `empleado_equipo` | 0 |
| `tarea_equipo` | 0 |

Planillas: una `ACTIVE`, doce `ARCHIVED`. Asignaciones: 860 `ASSIGNED`, 140 `REST`. Rango de número de semana: 1–30. Las cinco tablas muchos a muchos contienen las dos referencias y no tienen el campo `id` adicional propuesto por `schema.sql`.

## Límites de la evidencia

No se ejecutaron MySQL, Docker, despliegues externos, pruebas concurrentes, carga masiva, auditoría visual o escáner de dependencias. No se modificaron las reglas ni se añadió una suite permanente de pruebas de negocio. El análisis de otros casos, como borrados con claves foráneas, edición de autorizaciones, histórico mutable y posibles problemas de igualdad de entidades, procede de la lectura del código y está identificado como tal en la memoria.

Los archivos temporales de la revisión se encuentran bajo `target/analysis-checks/`, y el reporte de la prueba existente bajo `target/surefire-reports/`. Pueden desaparecer al limpiar la compilación; esta memoria conserva los resultados necesarios para retomar el trabajo.

## Verificación posterior del despliegue — 27 de septiembre de 2026

La sección anterior conserva los límites de la revisión inicial. En una revisión posterior se consultaron, en modo lectura, los paneles autenticados que el propietario proporcionó:

- Render mostró un Web Service Docker gratuito en Ohio, enlazado a GitHub `main`, con el commit `a6c2033` marcado `Live` y despliegues recientes iniciados por Auto-Deploy.
- La configuración de Render confirmó Dockerfile `./Dockerfile`, contexto raíz, sin comando de arranque sustituto y cuatro variables por nombre. Sus valores secretos no se revelaron ni se guardaron.
- Los logs de un arranque en frío confirmaron Java 17, Spring Boot 4.0.6, el perfil predeterminado `prod` y ocho repositorios JPA. La búsqueda `ERROR` no devolvió coincidencias en la última hora. Durante la ventana observada, la URL continuó mostrando la pantalla de reactivación del plan gratuito; no se afirmó una respuesta HTTP final de la aplicación.
- Aiven mostró MySQL 8.4.8 en ejecución, un nodo, `defaultdb`, TLS obligatorio, 1 CPU/1 GB RAM/1 GB de almacenamiento, DigitalOcean `sfo`, acceso por Internet pública y lista de IP abierta.

No se modificaron configuraciones de Render o Aiven, no se mostraron secretos y no se lanzó un despliegue. La topología completa está en `docs/DESPLIEGUE.md`. Las correcciones locales del algoritmo siguen separadas del commit que atiende producción.
