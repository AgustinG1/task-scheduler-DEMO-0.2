# TaskScheduler

Aplicación web en Spring Boot para administrar áreas, tareas, empleados, catálogos, equipos y planillas.

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