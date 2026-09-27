# TaskScheduler

Aplicación web en Spring Boot para administrar áreas, tareas, empleados, catálogos, equipos y planillas.

La producción utiliza un Web Service Docker en Render y MySQL administrado en Aiven. Consulta [la arquitectura de despliegue](docs/DESPLIEGUE.md) para conocer el flujo, los perfiles y las comprobaciones operativas sin exponer secretos.

## Requisitos

- Java 17 o superior
- Maven Wrapper incluido en el proyecto
- Docker y Docker Compose para levantar la demo completa

## Ejecutar en local

```powershell
$env:SPRING_PROFILES_ACTIVE = "local"
.\mvnw.cmd spring-boot:run
```

La aplicación usa H2 en memoria solo cuando activas explícitamente el perfil `local`.

Abrir:

- http://localhost:8080

## Ejecutar con Docker

Este proyecto incluye un entorno listo para demo con MySQL y la app.

```powershell
docker compose up --build
```

Abrir:

- http://localhost:8080

## Perfil de producción

Para despliegue real se usa el perfil `prod` por defecto. Define estas variables de entorno:

- `DB_HOST`
- `DB_PORT`
- `DB_NAME`
- `DB_USERNAME`
- `DB_PASSWORD`

Ejemplo:

```powershell
$env:SPRING_PROFILES_ACTIVE = "prod"
.\mvnw.cmd spring-boot:run
```

## Notas

- El archivo `docker-compose.yml` está pensado para que otra persona pueda probar la app sin configurar una base de datos externa.
- Si quieres datos iniciales para demo, puedes cargarlos desde la aplicación o agregar un script de inicialización.

## Pruebas

Ejecuta la suite JUnit desde esta carpeta:

```powershell
.\mvnw.cmd test
```

Las pruebas de integración usan H2 en memoria y deshacen sus datos al terminar. La suite comprueba la cobertura de tareas compatibles, los límites de semanas, la rotación, el archivado y la exportación Excel. Consulta [el estado y los próximos pasos](docs/PLAN_TESTING.md).
