# TaskScheduler

Aplicación web en Spring Boot para administrar áreas, tareas, empleados, catálogos, equipos y planillas.

La producción utiliza un Web Service Docker en Render y MySQL administrado en Aiven.

**Acceso personal publicado:** esta versión incorpora Auth0, la biblioteca `personal-auth-core` y `AppUser`. La portada es pública; el perfil, el panel `/app`, los seis módulos y las descargas requieren identidad OIDC, correo verificado y cuenta activa. Cada cuenta consulta y modifica sus propios datos. El 16 de octubre de 2026 se verificaron en Render/Aiven el login de la cuenta pública y de una cuenta Google, sesión, logout, reingreso, aislamiento en ambos sentidos, generación, historial y Excel. Los datos históricos se asignaron a la cuenta pública de prueba conservando todos sus campos originales.

[Abrir TaskScheduler](https://task-scheduler-app-demo-0-2.onrender.com/). Desde la portada puedes iniciar sesión o crear una cuenta.

## Requisitos

- Java 17 o superior
- Maven Wrapper incluido en el proyecto
- Docker y Docker Compose para levantar la demo completa

## Ejecutar en local

```powershell
.\mvnw.cmd -f workspace/pom.xml verify
.\Start-TaskScheduler.ps1
```

El reactor construye la biblioteca, TaskScheduler y la demo independiente, sin descargar un artefacto privado. El lanzador activa `local,auth0`, usa H2 en memoria y escucha solo en loopback. Pide issuer, Client ID y secreto con entrada oculta, o usa las variables `AUTH0_ISSUER`, `AUTH0_CLIENT_ID`, `AUTH0_CLIENT_SECRET` ya definidas en el proceso. No guarda credenciales en archivos. Al reiniciar H2 se pierden los usuarios locales de esta prueba; la identidad se vuelve a resolver al entrar.

Configurar en el cliente web Auth0 de TaskScheduler el callback `http://localhost:8080/login/oauth2/code/auth0` y logout `http://localhost:8080/`. Las URLs de la demo de 8082 son distintas. El lanzador admite `-Port`, comprueba disponibilidad y ejecuta una copia del JAR para permitir reconstrucciones en Windows. Un cambio de puerto requiere las URLs correspondientes en Auth0. El cliente propio de desarrollo se configuró el 15 de octubre; sus detalles y la comprobación real están en [la guía de integración](docs/INTEGRACION_AUTH_TASKSCHEDULER.md).

Para arrancar sin proveedor y comprobar que la app permanece cerrada: `java -jar target/task-scheduler-0.0.1-SNAPSHOT.jar --spring.profiles.active=local`. La portada informa que el acceso no está disponible y ninguna ruta de negocio se abre. No se habilita un usuario o contraseña de desarrollo alternativo.

Abrir:

- http://localhost:8080

## Ejecutar con Docker

Este proyecto incluye un entorno con MySQL y la app. En este incremento el perfil `docker` no configura Auth0: permite comprobar el arranque y la portada de acceso no disponible; los módulos siguen cerrados.

```powershell
docker compose up --build
```

Compose crea una red privada entre `app` y `mysql`, espera a que MySQL esté saludable y conserva sus datos en el volumen `mysql_data`. La base no publica su puerto en el equipo anfitrión.

Los valores predeterminados sirven para desarrollo. Para personalizarlos, copia `.env.example` como `.env` y cambia `APP_PORT`, `MYSQL_DATABASE`, `MYSQL_USER`, `MYSQL_PASSWORD` o `MYSQL_ROOT_PASSWORD`. El archivo `.env` está excluido de Git.

Para detener los contenedores sin borrar la base:

```powershell
docker compose down
```

Para eliminar también el volumen local de MySQL y empezar desde cero:

```powershell
docker compose down --volumes
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

El despliegue con acceso personal necesita además el perfil `auth0`, las variables `AUTH0_ISSUER`, `AUTH0_CLIENT_ID`, `AUTH0_CLIENT_SECRET`, callbacks/logout HTTPS exactos y configuración del proxy. La cookie es HttpOnly, SameSite=Lax, Secure fuera de `local`, con 30 minutos de inactividad. Las migraciones manuales `database/migrations/001_create_app_user.mysql.sql` a `004_require_owners.mysql.sql` no se ejecutan automáticamente: prod usa `ddl-auto=validate` y no ejecuta `schema.sql`. Deben aplicarse con el servicio cerrado antes de arrancar esta versión; no mezclarla con instancias antiguas de acceso global. Ya están aplicadas en la instancia Aiven de esta demo: no repetirlas sobre esa base.

En prod, definir también `APP_PUBLIC_URL` con el origen HTTPS público sin barra final. El callback y logout utilizan ese origen validado; el listener usa `PORT` (8080 como respaldo) y procesa las cabeceras del proxy. [Preparación y secuencia de publicación](docs/PUBLICACION_AUTH_PRODUCCION.md). En Render se comprobaron callback HTTPS al cliente productivo, cookies Secure/HttpOnly/SameSite=Lax, redirección de rutas privadas sin sesión y rechazo del logout sin CSRF.

Se restauraron respaldos reales y se ensayaron las cuatro migraciones en MySQL 8.4.8 aislado: campos originales de las 13 tablas conservados, datos históricos asignados a la identidad pública explícita, NOT NULL y unicidad de equipos por cuenta. Pasaron los casos de error y la lectura/validación de entidades con Java 17. El 16 de octubre se aplicó el paquete verificado en Aiven tras comparar esquema y todas las filas con un respaldo fresco; el cliente confirmó conservación de originales y propietarios. La ejecución se hizo con la política de claves primarias de Aiven activa. Detalles locales en [el informe del ensayo](docs/ENSAYO_MIGRACION_MYSQL_2026-10-15.md).

Ejemplo:

```powershell
$env:SPRING_PROFILES_ACTIVE = "prod,auth0"
java -jar target/task-scheduler-0.0.1-SNAPSHOT.jar
```

## Notas

- El archivo `docker-compose.yml` está pensado para que otra persona pueda probar la app sin configurar una base de datos externa.
- Los datos históricos se asignarán explícitamente a la cuenta pública de prueba acordada, identificada por issuer y subject de Auth0. No se entregan al primer usuario que entre. Esa cuenta será normal y compartirá datos/cambios entre sus visitantes; no hay modo invitado ni permisos especiales.
- Render no utiliza este archivo: producción mantiene un Web Service Docker conectado a Aiven.

## Pruebas

Ejecuta la suite JUnit desde esta carpeta:

```powershell
.\mvnw.cmd -f workspace/pom.xml verify
```

Las 124 pruebas usan bases H2 en memoria e identidades OIDC sintéticas, sin conectar Auth0 ni producción. Comprueban identidad local, correo verificado, sesiones, CSRF, logout y aislamiento con dos cuentas: consultas, relaciones, escrituras, historial, ambas descargas y generación concurrente. Incluyen retorno HTTPS detrás del proxy, cookie segura y configuración local; conservan las pruebas de cobertura y rotación del motor. La comprobación manual real del 15 de octubre verificó por separado login/logout, dos cuentas, generación, historial y XLSX. Ni la suite ni esa comprobación sobre H2 sustituyen el ensayo de migración MySQL.

## Biblioteca de identidad en desarrollo

[Personal Auth Core](libraries/personal-auth-core/README.md) es un módulo Maven independiente para reutilizar el acceso a identidades OIDC en apps Java/Spring. Obtiene el usuario actual desde Spring Security y permite exigir correo verificado. TaskScheduler y la demo lo consumen como dependencia; las reglas de propiedad de áreas, empleados, tareas y planillas permanecen en la app principal.

Para construir y probar únicamente la biblioteca:

```powershell
mvn -f libraries/personal-auth-core/pom.xml -B -ntp verify
```

El [consumidor de prueba OIDC](libraries/personal-auth-demo/README.md) utiliza esa biblioteca como dependencia y prueba sesiones, perfiles protegidos y logout con CSRF. Para construir ambos módulos juntos:

```powershell
mvn -f libraries/pom.xml -B -ntp verify
```

La demo ya completó un primer login real con Auth0 y conservó la sesión al recargar el perfil. Su interfaz usa el estilo de TaskScheduler; los detalles de arranque y la vista previa están en el README del consumidor. El 15 de octubre de 2026 se publicó también el tema oscuro y logo en Universal Login de Auth0. TaskScheduler consume la biblioteca, persiste AppUser y separa sus datos por cuenta; el acceso de la pública y de una cuenta Google, logout, recuperación de datos al reingresar y rechazo de recursos ajenos se comprobaron tanto localmente como en Render/MySQL. SSO entre distintas apps y recuperación por correo son comprobaciones distintas todavía pendientes.
