# Inventario de los archivos originales

Revisión del 26 de septiembre de 2026. Referencia: `main` en `a6c203360fcd1da6d218bb849782019c6d8fa057`. Son los 71 archivos originales; se excluyen de este inventario los documentos añadidos durante la revisión y la carpeta de compilación `target/`.

Los documentos y fuentes se leyeron completos. El Word se revisó mediante extracción de texto e inspección de sus tres imágenes; de la base binaria se examinaron estructura y agregados sobre una copia de solo lectura.

## Propósito de cada archivo

| Archivo | Papel en el proyecto |
|---|---|
| [.gitattributes](../.gitattributes) | Política de finales de línea de los lanzadores. |
| [.gitignore](../.gitignore) | Exclusiones de compilación, IDE y variables locales; también excluye analysis_results.md y .env.example. |
| [.mvn/wrapper/maven-wrapper.properties](../.mvn/wrapper/maven-wrapper.properties) | Wrapper 3.3.4 y distribución Maven 3.9.15. |
| [Dockerfile](../Dockerfile) | Construcción multietapa Maven/Java 17 y ejecución del JAR. |
| [README.md](../README.md) | Instrucciones de ejecución local, Docker y producción. |
| [analysis_results.md](../analysis_results.md) | Análisis previo; contiene afirmaciones obsoletas contrastadas en la memoria nueva. |
| [data/taskdb.mv.db](../data/taskdb.mv.db) | Base H2 histórica; metadatos y conteos examinados sobre copia de solo lectura. |
| [data/taskdb.trace.db](../data/taskdb.trace.db) | Trazas históricas de errores SQL manuales. |
| [docker-compose.yml](../docker-compose.yml) | Servicios MySQL 8.4 y aplicación, healthcheck y volumen. |
| [intro.docx](../intro.docx) | Objetivos originales, propuestas de API y tres diagramas revisados. |
| [mvnw](../mvnw) | Lanzador Maven Wrapper para sistemas POSIX. |
| [mvnw.cmd](../mvnw.cmd) | Lanzador Maven Wrapper para Windows; CRLF local equivale a LF del blob remoto. |
| [pom.xml](../pom.xml) | Dependencias, Java 17, procesador Lombok y plugin de empaquetado Spring Boot. |
| [src/main/java/com/taskapp/task_scheduler/TaskSchedulerApplication.java](../src/main/java/com/taskapp/task_scheduler/TaskSchedulerApplication.java) | Arranque de Spring Boot. |
| [src/main/java/com/taskapp/task_scheduler/config/DataInitializer.java](../src/main/java/com/taskapp/task_scheduler/config/DataInitializer.java) | Ejemplo de carga inicial completamente comentado y con firma antigua del motor. |
| [src/main/java/com/taskapp/task_scheduler/controller/AreaController.java](../src/main/java/com/taskapp/task_scheduler/controller/AreaController.java) | Rutas y formularios de áreas. |
| [src/main/java/com/taskapp/task_scheduler/controller/EmployeeController.java](../src/main/java/com/taskapp/task_scheduler/controller/EmployeeController.java) | Rutas de empleados, edición, desactivación y borrado. |
| [src/main/java/com/taskapp/task_scheduler/controller/HomeController.java](../src/main/java/com/taskapp/task_scheduler/controller/HomeController.java) | Inicio y conteo de módulos con datos. |
| [src/main/java/com/taskapp/task_scheduler/controller/PayrollController.java](../src/main/java/com/taskapp/task_scheduler/controller/PayrollController.java) | Planilla activa, generación, historial, borrado y descarga. |
| [src/main/java/com/taskapp/task_scheduler/controller/PayrollExportController.java](../src/main/java/com/taskapp/task_scheduler/controller/PayrollExportController.java) | Descarga Excel por /api/payrolls/{id}/download. |
| [src/main/java/com/taskapp/task_scheduler/controller/TaskController.java](../src/main/java/com/taskapp/task_scheduler/controller/TaskController.java) | Rutas de tareas; vincula una autorización de área en la creación. |
| [src/main/java/com/taskapp/task_scheduler/controller/TaskGroupController.java](../src/main/java/com/taskapp/task_scheduler/controller/TaskGroupController.java) | Catálogos, selección de tareas y desvinculación al borrar. |
| [src/main/java/com/taskapp/task_scheduler/controller/TeamController.java](../src/main/java/com/taskapp/task_scheduler/controller/TeamController.java) | Equipos, áreas/catálogos y edición global del empleado desde un modal. |
| [src/main/java/com/taskapp/task_scheduler/model/Area.java](../src/main/java/com/taskapp/task_scheduler/model/Area.java) | Entidad de área, con relación inversa a equipos. |
| [src/main/java/com/taskapp/task_scheduler/model/Assignment.java](../src/main/java/com/taskapp/task_scheduler/model/Assignment.java) | Entidad de tarea o descanso para un empleado, planilla y semana. |
| [src/main/java/com/taskapp/task_scheduler/model/AssignmentStatus.java](../src/main/java/com/taskapp/task_scheduler/model/AssignmentStatus.java) | Estados ASSIGNED, REST y MANUALLY_MODIFIED; este último sin flujo. |
| [src/main/java/com/taskapp/task_scheduler/model/Employee.java](../src/main/java/com/taskapp/task_scheduler/model/Employee.java) | Entidad de empleado con área obligatoria y estado activo. |
| [src/main/java/com/taskapp/task_scheduler/model/Payroll.java](../src/main/java/com/taskapp/task_scheduler/model/Payroll.java) | Entidad de planilla con fechas, estado y equipo opcional. |
| [src/main/java/com/taskapp/task_scheduler/model/PayrollStatus.java](../src/main/java/com/taskapp/task_scheduler/model/PayrollStatus.java) | Estados ACTIVE y ARCHIVED. |
| [src/main/java/com/taskapp/task_scheduler/model/Task.java](../src/main/java/com/taskapp/task_scheduler/model/Task.java) | Entidad de tarea, autorizaciones, etiqueta visual y desvinculación de catálogos al borrar. |
| [src/main/java/com/taskapp/task_scheduler/model/TaskArea.java](../src/main/java/com/taskapp/task_scheduler/model/TaskArea.java) | Entidad de autorización de tarea por área. |
| [src/main/java/com/taskapp/task_scheduler/model/TaskAreaId.java](../src/main/java/com/taskapp/task_scheduler/model/TaskAreaId.java) | Clave compuesta de tarea y área. |
| [src/main/java/com/taskapp/task_scheduler/model/TaskGroup.java](../src/main/java/com/taskapp/task_scheduler/model/TaskGroup.java) | Entidad de catálogo de tareas reutilizable. |
| [src/main/java/com/taskapp/task_scheduler/model/TaskType.java](../src/main/java/com/taskapp/task_scheduler/model/TaskType.java) | Tipos GENERAL y SPECIFIC. |
| [src/main/java/com/taskapp/task_scheduler/model/Team.java](../src/main/java/com/taskapp/task_scheduler/model/Team.java) | Entidad de equipo con áreas, catálogos y relaciones inversas. |
| [src/main/java/com/taskapp/task_scheduler/repository/AreaRepository.java](../src/main/java/com/taskapp/task_scheduler/repository/AreaRepository.java) | CRUD JPA de áreas y búsqueda por nombre. |
| [src/main/java/com/taskapp/task_scheduler/repository/AssignmentRepository.java](../src/main/java/com/taskapp/task_scheduler/repository/AssignmentRepository.java) | Asignaciones por planilla, empleado o tarea. |
| [src/main/java/com/taskapp/task_scheduler/repository/EmployeeRepository.java](../src/main/java/com/taskapp/task_scheduler/repository/EmployeeRepository.java) | CRUD y búsquedas por área o estado activo. |
| [src/main/java/com/taskapp/task_scheduler/repository/PayrollRepository.java](../src/main/java/com/taskapp/task_scheduler/repository/PayrollRepository.java) | CRUD y búsqueda por estado que devuelve una lista. |
| [src/main/java/com/taskapp/task_scheduler/repository/TaskAreaRepository.java](../src/main/java/com/taskapp/task_scheduler/repository/TaskAreaRepository.java) | CRUD de clave compuesta y eliminación por tarea. |
| [src/main/java/com/taskapp/task_scheduler/repository/TaskGroupRepository.java](../src/main/java/com/taskapp/task_scheduler/repository/TaskGroupRepository.java) | CRUD JPA de catálogos. |
| [src/main/java/com/taskapp/task_scheduler/repository/TaskRepository.java](../src/main/java/com/taskapp/task_scheduler/repository/TaskRepository.java) | CRUD JPA de tareas y búsqueda por tipo. |
| [src/main/java/com/taskapp/task_scheduler/repository/TeamRepository.java](../src/main/java/com/taskapp/task_scheduler/repository/TeamRepository.java) | CRUD JPA de equipos. |
| [src/main/java/com/taskapp/task_scheduler/service/AreaService.java](../src/main/java/com/taskapp/task_scheduler/service/AreaService.java) | CRUD de áreas. |
| [src/main/java/com/taskapp/task_scheduler/service/AssignmentAlgorithm.java](../src/main/java/com/taskapp/task_scheduler/service/AssignmentAlgorithm.java) | Motor vigente, transaccional, por equipo y semanas. |
| [src/main/java/com/taskapp/task_scheduler/service/EmployeeService.java](../src/main/java/com/taskapp/task_scheduler/service/EmployeeService.java) | CRUD, activación inicial y desactivación de empleados. |
| [src/main/java/com/taskapp/task_scheduler/service/ExcelExportService.java](../src/main/java/com/taskapp/task_scheduler/service/ExcelExportService.java) | Generación de libro XLSX y estilos con Apache POI. |
| [src/main/java/com/taskapp/task_scheduler/service/FeasibilityValidator.java](../src/main/java/com/taskapp/task_scheduler/service/FeasibilityValidator.java) | Comprobación limitada a listas de empleados y tareas no vacías. |
| [src/main/java/com/taskapp/task_scheduler/service/PayrollService.java](../src/main/java/com/taskapp/task_scheduler/service/PayrollService.java) | Primera activa, historial, borrado transaccional y creador alternativo no usado. |
| [src/main/java/com/taskapp/task_scheduler/service/TaskService.java](../src/main/java/com/taskapp/task_scheduler/service/TaskService.java) | CRUD, autorización por área y conversión histórica a REST al borrar tareas. |
| [src/main/java/com/taskapp/task_scheduler/service/algoritmoantes.txt](../src/main/java/com/taskapp/task_scheduler/service/algoritmoantes.txt) | Tres variantes previas del motor y configuración antigua H2 en archivo; no se compila. |
| [src/main/resources/application-docker.properties](../src/main/resources/application-docker.properties) | MySQL interno para contenedores. |
| [src/main/resources/application-local.properties](../src/main/resources/application-local.properties) | H2 en memoria y consola local. |
| [src/main/resources/application-prod.properties](../src/main/resources/application-prod.properties) | MySQL mediante variables DB_* y TLS obligatorio. |
| [src/main/resources/application.properties](../src/main/resources/application.properties) | Configuración común y selección predeterminada de prod. |
| [src/main/resources/schema.sql](../src/main/resources/schema.sql) | Cinco tablas puente; inicialización parcial no versionada. |
| [src/main/resources/templates/area/editar.html](../src/main/resources/templates/area/editar.html) | Edición de nombre y descripción de área. |
| [src/main/resources/templates/area/lista.html](../src/main/resources/templates/area/lista.html) | Alta y listado de áreas. |
| [src/main/resources/templates/employee/editar.html](../src/main/resources/templates/employee/editar.html) | Edición de nombre, apellido y área. |
| [src/main/resources/templates/employee/lista.html](../src/main/resources/templates/employee/lista.html) | Alta y listado de empleados. |
| [src/main/resources/templates/index.html](../src/main/resources/templates/index.html) | Panel principal con módulos y accesos directos. |
| [src/main/resources/templates/layout/base.html](../src/main/resources/templates/layout/base.html) | Layout común, CSS oscuro, dependencias CDN, menú y estilos móviles. |
| [src/main/resources/templates/payroll/historial.html](../src/main/resources/templates/payroll/historial.html) | Listado de todas las planillas, descarga y borrado. |
| [src/main/resources/templates/payroll/vista.html](../src/main/resources/templates/payroll/vista.html) | Generador por equipo/semanas y matriz semanal activa. |
| [src/main/resources/templates/task/editar.html](../src/main/resources/templates/task/editar.html) | Edición de datos y tipo de tarea, sin gestionar autorizaciones. |
| [src/main/resources/templates/task/lista.html](../src/main/resources/templates/task/lista.html) | Alta y listado de tareas; selector condicional de área. |
| [src/main/resources/templates/taskgroups/task-group-edit.html](../src/main/resources/templates/taskgroups/task-group-edit.html) | Edición de catálogo y selección de tareas. |
| [src/main/resources/templates/taskgroups/task-groups.html](../src/main/resources/templates/taskgroups/task-groups.html) | Alta y tarjetas de catálogos. |
| [src/main/resources/templates/teams/team-edit.html](../src/main/resources/templates/teams/team-edit.html) | Áreas/catálogos del equipo, directorio por área y modales de empleados. |
| [src/main/resources/templates/teams/teams.html](../src/main/resources/templates/teams/teams.html) | Alta y listado de equipos con áreas operativas. |
| [src/test/java/com/taskapp/task_scheduler/TaskSchedulerApplicationTests.java](../src/test/java/com/taskapp/task_scheduler/TaskSchedulerApplicationTests.java) | Una prueba contextLoads con perfil local. |

## Huellas verificadas

El hash de blob remoto es SHA-1 sobre la representación Git del archivo. SHA-256 corresponde a los bytes locales originales. Los 70 archivos distintos de `mvnw.cmd` coinciden byte por byte con GitHub. El lanzador de Windows coincide al normalizar CRLF a LF.

| Archivo | Bytes locales | SHA-256 local | Blob Git remoto |
|---|---:|---|---|
| `.gitattributes` | 38 | `5775a77ad2f6d12dde053a3fb0cca4309189163f0888f4e7e984cd0ab327028b` | `3b41682ac579fafb665abb4dfcdaa6aaaa712184` |
| `.gitignore` | 462 | `025eab330ba446f77d333ed9da3f314821fe0af6654b2553d884ccc7984aca0f` | `7cc40cc68fd679021c99ef0d04879b41426fa7b7` |
| `.mvn/wrapper/maven-wrapper.properties` | 168 | `402933edce21330da52e94a501e5458630b4687700e4353d2d9f2fd5292f44b9` | `52913720fda62b57fd584c1bae1415049b51e9fa` |
| `Dockerfile` | 416 | `f1b4b5ff956a3c31e641aa568868cac820f223457c53724b94aa346798682b7d` | `e3b729d54f391162b9f895294fdcfd10e4441945` |
| `README.md` | 1201 | `c20bb2afb78bce3341082ebffea67d16ab21e2d79e7bd23534dbfcb8e806f466` | `48bf3e5e2d06712ee0222717c4e1f222fa49cdd1` |
| `analysis_results.md` | 17487 | `1e2c61c89b31667610974f4e4a74d9cd1e2e41d45f12aa5cdcf2a9c75248f223` | `ab681629a5c7a0c0e866e4e13aabdb6c1facb4cf` |
| `data/taskdb.mv.db` | 172032 | `e5eb056da0d4c3e2b0cecb5cd177a45f7201c205857addbde5dd4740481db5c0` | `1a54d4943dc762b207a00f4c4794d981b25423cb` |
| `data/taskdb.trace.db` | 8870 | `3f16cf4d1828ca62df8642943ab5676a68fbb838033cbf5055d62df226d21e39` | `8a07b4589ad5b42f01cbc52aaacc2666b441cb0e` |
| `docker-compose.yml` | 769 | `c393a76ba20a1aa97f18be3321be0454f05fd82334b66c6718f1d6dfcfaba51c` | `0f5226019db562d3e9dc92410fabb98215c6be87` |
| `intro.docx` | 357271 | `217338320ec5b1b44c840c2f80d2847781cda443667cdad63984a6516bdf2a2b` | `98e0f4bca9483261f90ad0314850416fc56bf09c` |
| `mvnw` | 11790 | `cae96cef89ebea3531221f4ae17c23cf8edf67d00eae8306d4186ae1bbed4d02` | `bd8896bf2217b46faa0291585e01ac1a3441a958` |
| `mvnw.cmd` | 8481 | `46eedb8419bd14fe70d5bb2916d7b6f51806e51b39d5b76a42610384ca929c1c` | `92450f93273470af42eeee491874afb2039b700a` |
| `pom.xml` | 4260 | `d0842b285054866632f75a5d6048e84fd8ca6077e41306404660e2d9e4af074d` | `e2b5e1472e8a1c171447ade1f1685273bbdcc733` |
| `src/main/java/com/taskapp/task_scheduler/TaskSchedulerApplication.java` | 333 | `7aa747b2edef69b6ad358a8e40fe02b34a95c1bb03aa87f834c40561650d4b0c` | `b374e5ff0e36a90dec30712600845eb1cd119abb` |
| `src/main/java/com/taskapp/task_scheduler/config/DataInitializer.java` | 5295 | `d8a7f4fe3c7bdf849364963bf32061900a2928e220d837828a40aa436295c136` | `b5189d08f2b71bad965eb54cbff4bdc4b4c9fa41` |
| `src/main/java/com/taskapp/task_scheduler/controller/AreaController.java` | 1817 | `2ada2e2c33a0d37ea8b9cd345b87ee6540defad00e563d6c82cb317bcb583d23` | `b44b61058d5539af03c789313e950aa4d5c38370` |
| `src/main/java/com/taskapp/task_scheduler/controller/EmployeeController.java` | 3681 | `576fdac40329142395367584fbc9e06572abe22312247ed92a913992986d8bdc` | `51b7fe4c35a82b607e77252e43599445d64f944b` |
| `src/main/java/com/taskapp/task_scheduler/controller/HomeController.java` | 1700 | `41147d23aad8748271d730d45fc09e22eedbb00bc722a601525df0a392a5f8ff` | `b3533701bf02e7fb00974ae4ec8f838fa41e268f` |
| `src/main/java/com/taskapp/task_scheduler/controller/PayrollController.java` | 4575 | `21dfa53e1f227b01734ef88f8ed41b92f718b710964f0b58f3b0eeba8bb37c3b` | `7850c3f06b0668ea2f06ee866f1b312a75e392b5` |
| `src/main/java/com/taskapp/task_scheduler/controller/PayrollExportController.java` | 1298 | `229459fb0e22e0f1543d5c3670cc69a3d344d58ba1a76e3dd74219507af367fd` | `d1a784cb154a34e7571c322a187a6b03212962d2` |
| `src/main/java/com/taskapp/task_scheduler/controller/TaskController.java` | 2409 | `1467be5ef275790be324c8980334da5f7920408de64fd764a0f7cd2a77a7a55d` | `9e3ff3ac3b434cace8e5aa4048b883b05a02a6fe` |
| `src/main/java/com/taskapp/task_scheduler/controller/TaskGroupController.java` | 3487 | `0e4ca1b134deb5bcaee3046788e6451f975120cd7136469cddf9a38ad038fcea` | `90e3fcc274077da224217c04320f63a47432ebe8` |
| `src/main/java/com/taskapp/task_scheduler/controller/TeamController.java` | 6274 | `8d982a714899e3b4e78e2f667e722af07ebc4f58eebc967d47ff6a05c7fe8a59` | `d3625806c8b1dcea9b0f5a5e174ed4c287f9fe3d` |
| `src/main/java/com/taskapp/task_scheduler/model/Area.java` | 825 | `3d0fc3480e9baada2f64b1128fe6b93aa3ce86d1a58cabe44673bdeda0b7038e` | `a17a3d003475e0efda081238bf021a66f5b37313` |
| `src/main/java/com/taskapp/task_scheduler/model/Assignment.java` | 849 | `ef057befbc6c134edbfaceac2102e6cac5b1ce918950fb03cb57c2c807d0ecb7` | `7880bebb2be3fdc1e70438457ba2359816ace9e9` |
| `src/main/java/com/taskapp/task_scheduler/model/AssignmentStatus.java` | 121 | `e5fd8dbd2b2ef434455be2b6294d7fd25436dad1daa5b3687cdca99aafea6454` | `ef55291db4202307b68cd38b885e47dbf518b9e5` |
| `src/main/java/com/taskapp/task_scheduler/model/Employee.java` | 1503 | `42a4cbdc34699feae32cf7ea0bec9eaf880c548b06d5017bf66e2d75a6931ebd` | `1fe5e6f4010e6735499df3bfd20f4e0b31e3fa0b` |
| `src/main/java/com/taskapp/task_scheduler/model/Payroll.java` | 961 | `2d120e0870780efe24eeaac40cb0f1bc5430c9b398303630375561459f0cf862` | `c095fdcd2df6aeb002cd720fa86831337ac04a8f` |
| `src/main/java/com/taskapp/task_scheduler/model/PayrollStatus.java` | 97 | `15893888a7b0e45f059367a0404846b215087f354dafb04c8741184a68d05910` | `39d2f42efbda3118a178c2beb3450f5dbe6bf56b` |
| `src/main/java/com/taskapp/task_scheduler/model/Task.java` | 2512 | `e7d027cb7a33407dec5d2f9f6bfda24fff11077c45a9f1760fb1d918d896a2ad` | `a1f7c007b2d28ec080150b9ac2beaede81c70fe1` |
| `src/main/java/com/taskapp/task_scheduler/model/TaskArea.java` | 538 | `234bdadd6af0d2cb3865d4de403d9ccd9c84e332d78bc0249292b2284e2564c4` | `3558a8800143ffec684186478972bc22680e99ad` |
| `src/main/java/com/taskapp/task_scheduler/model/TaskAreaId.java` | 358 | `83d17125b2126e73b66b2ed26d522460fba87612ffdd1be5f4dd21886e499cc2` | `41b33945b33d9f6a83486218362a1c13f7c760b7` |
| `src/main/java/com/taskapp/task_scheduler/model/TaskGroup.java` | 1242 | `59cfe43392e7b5b0194d4bc599fcc637513da4318c5763c7fb2e78eadc5294fe` | `627fb980072bf2ec99a459253130915ea4cfee75` |
| `src/main/java/com/taskapp/task_scheduler/model/TaskType.java` | 94 | `a753114a1653db54a34141e3988ba9735207a6bcafd12bbc89e159e65bb33567` | `211a200b866e0eac33b7097f89f025a580e51ee4` |
| `src/main/java/com/taskapp/task_scheduler/model/Team.java` | 1711 | `de54471ee0655f4139b6931bed46093187b646a9f992b32990a03629eb28c011` | `f17e0ee2076c1d969d77b0aa7dc83dfd29ccac62` |
| `src/main/java/com/taskapp/task_scheduler/repository/AreaRepository.java` | 431 | `747eb7caf38072c8438797570532b928857679aa24de02fde408fe057ffce8c3` | `ae444ef6d7516027f2d9ff16c42c36ac1c1dc0ca` |
| `src/main/java/com/taskapp/task_scheduler/repository/AssignmentRepository.java` | 600 | `a090b3d6123d112718e2e99205eaaa1f1f78aad3d276a9806bba93943ac723ce` | `96f34afbf819c03569a718ed970f2a206df9e228` |
| `src/main/java/com/taskapp/task_scheduler/repository/EmployeeRepository.java` | 922 | `db20f55aa827d59e4812026fbb025bc41a48f13ee62ff8f2b5ece3c8a61fb970` | `39c8fa4543c7f2c21589d85301c49aa77408ee8a` |
| `src/main/java/com/taskapp/task_scheduler/repository/PayrollRepository.java` | 490 | `d549f42de58482e30356b13761b2fafb902e10d889ea66e5b25a605d1ef62193` | `f466a21de6c0593b1b74a86c87bd9d8c0044f33d` |
| `src/main/java/com/taskapp/task_scheduler/repository/TaskAreaRepository.java` | 398 | `bef3dbe015b4041307a1dc946c2410637f4ddaae311e9c71830bd2d542225a25` | `e8aeb84941a098731a53dbb621035a280607217d` |
| `src/main/java/com/taskapp/task_scheduler/repository/TaskGroupRepository.java` | 303 | `a2de010fa65a9324ab8b514560fa270fdbb95a028a1d380bd5c1c7beaf9596ad` | `f74e12283111c381aaa3fbf41f48cc18079162b4` |
| `src/main/java/com/taskapp/task_scheduler/repository/TaskRepository.java` | 413 | `d383d4bca5aea732f4522355993c92ffc62dd1c46582478d881660fe08827e91` | `6f50f0e378e05800e2f7c45b23a4f7566317cdd8` |
| `src/main/java/com/taskapp/task_scheduler/repository/TeamRepository.java` | 363 | `35c38baa98c5d99e7bf8711135b5109c637df8a688a0ecdd40b5747e887f0ef3` | `64853084c00895ef1238aafa5c8581ebbeceb1a6` |
| `src/main/java/com/taskapp/task_scheduler/service/AreaService.java` | 1946 | `92a47696b00b121f4e9a83a71040b4ce495e4a8104a413fa719fdbb784e1adfd` | `717735560063f6ee31d2032a16037af1e695303b` |
| `src/main/java/com/taskapp/task_scheduler/service/AssignmentAlgorithm.java` | 10853 | `078112311ac61af68d79bc2ed78f54c8ebeca9c6bcef8275175359c5dcf1616e` | `189181a43a0afa4cb8933e4726a7dfb0fdf7d38f` |
| `src/main/java/com/taskapp/task_scheduler/service/EmployeeService.java` | 2284 | `22b0811841d455b28a49723f49b0f43658145039d94efffad51b5fe21ea32c5f` | `d9f11d433cda139f66fab7279cc12d2ac96b84ac` |
| `src/main/java/com/taskapp/task_scheduler/service/ExcelExportService.java` | 9339 | `8732613b1e3f614512602511dd6a3f8627d143f2d23cb71764af403b3fdbbe1a` | `f6f13262593bff71a7e0fca1a6799d57addc799b` |
| `src/main/java/com/taskapp/task_scheduler/service/FeasibilityValidator.java` | 1182 | `223d559bd18fdfcc6ed8df9f11030beddc83aedf35fe29d9278d1a21c577c113` | `70400c1c7e09e070d8d30cc1954b1bd3d38cb155` |
| `src/main/java/com/taskapp/task_scheduler/service/PayrollService.java` | 2622 | `d3344216015eae3d04689817373240d5942efb8fefa0e067ee93661a7c2aa81d` | `484e84eb24f3aa402fcf2aac1590bd09609880a8` |
| `src/main/java/com/taskapp/task_scheduler/service/TaskService.java` | 3500 | `6a8a248d020cb26de277a42a9af47f47eccce123b4e490ae40214ade375d0576` | `f20242d35501160da59e4b4537917fa82a646e3a` |
| `src/main/java/com/taskapp/task_scheduler/service/algoritmoantes.txt` | 25609 | `d374b9ab32bb9152a91ee1594468c24af3aa4130eb495bbca13c7df9bd5f5619` | `8792861f215324281c917fec22d90ad73a5f44e4` |
| `src/main/resources/application-docker.properties` | 405 | `741bb8b479a7d0abc5a7c82aac6d8482032f60a553358082549408d63e382617` | `40e9478245b7c946a859d2275fa3f8b6817407ea` |
| `src/main/resources/application-local.properties` | 354 | `8d0b97858e62812a7e503906a8bc006ead73a1be20f7569e887613d88b799dce` | `e470f421ad1322e2cecc5f2c4ad1815ff33a1f92` |
| `src/main/resources/application-prod.properties` | 384 | `480891c764d36ece6cc6bed56cf6fcd010f0b3d6a8c11607873a5b56c47536d8` | `3d77018f123b2edf8dbe67ab792d08d87b12cc79` |
| `src/main/resources/application.properties` | 736 | `252da65a58e2a8f66c9a3a8d8d6dbb31297b456fa1d7e96a68ef9b2266693b57` | `470d3a6c4b0d18487865b00938371cffd7b764fb` |
| `src/main/resources/schema.sql` | 1044 | `07903a62a804499c77f0a0a909401a1af68f15a02bc53aeb12e1ffd2ee44e708` | `3d3c255848b5b55643b31e35400844245300c33c` |
| `src/main/resources/templates/area/editar.html` | 2395 | `4bb2207b4964ced1db27fc8808b132e1170ede14af57288546e1cd96ec4895e0` | `51c324e249ea9980342cd1de2076e5700fe35e49` |
| `src/main/resources/templates/area/lista.html` | 4203 | `0c6c5656916e700dc4565a798a7176cc80250f7307ef4e1a7eae6008ee26e9e7` | `07f3f3b559e4c1460674f7f8a0bbdf5612239c8d` |
| `src/main/resources/templates/employee/editar.html` | 3139 | `1a3c096b693f786352f7e9da48d7f2abf975292f322a43cfde98e15de25e08a0` | `fb06bc5f4b865716a19e14caaa9a47d64002b0f5` |
| `src/main/resources/templates/employee/lista.html` | 4589 | `f7246673850a0c22376e4142eafdfacd63a62d871aaca366fd7a46d9a354220a` | `902b3cd2ca1cea85ea9a93a687005ab9709cf974` |
| `src/main/resources/templates/index.html` | 9577 | `b1c8dad600e89b81dcaa7dcd5dd0faa9fc3f2307a2a722cd50ebeb26254c570e` | `5dc4ed56ed9eeba746ee4aa6cc23c78cfec684de` |
| `src/main/resources/templates/layout/base.html` | 22474 | `8ca7c6d495137ba156b3be55b9c79165e2299747ff84d758b2293eb5ad93e7f6` | `c505e796cab75b98d12c5afdb7064fd6fff26619` |
| `src/main/resources/templates/payroll/historial.html` | 3228 | `60550d03135120a0d4ac044dc741826343976615c3e3a9ca77880921e8949000` | `12370ced73f09482b6dbe8fca2ab689ec228f50e` |
| `src/main/resources/templates/payroll/vista.html` | 6406 | `817da469c8ede8afc0e1d55bc31bfc6b551d3411033cedd57de81f634073d433` | `4cca12bcab97b3c27ab00a4c6d3541efa551cb09` |
| `src/main/resources/templates/task/editar.html` | 2831 | `519bc32cfcb63bf13c8b66218cc0c6d4d7a5cfc4fcca3db057f2d9bcb1ff726d` | `60e23d52c4636747024fa9370f2db6f853823adf` |
| `src/main/resources/templates/task/lista.html` | 6599 | `5cc823e90a2a178cfbd95a687586d985f60985b7bcb2b63296af78d505b86050` | `a9a92e6d7cb62a1618c656646db82b4325019590` |
| `src/main/resources/templates/taskgroups/task-group-edit.html` | 4072 | `b1cd56f0e0f480968ec4a399d765ed5ab8dc912406acc2a773082c3462d24aab` | `ca9f1ffe820b943509d734dd49c30a4eb23b494c` |
| `src/main/resources/templates/taskgroups/task-groups.html` | 4957 | `5fe32e8ab00b1dbea17ad9c4552b7b6ec426e9bca48a694f97556f9d65d6e60d` | `b5eb7401618a68f6075426bae008e5e8798f6574` |
| `src/main/resources/templates/teams/team-edit.html` | 10951 | `659a60bb0bb8c2ec60dacf4401e84faea2a440e06422284e274bb274fdd4197c` | `47177ff56be467917aaf43937d027abf7d3a1a80` |
| `src/main/resources/templates/teams/teams.html` | 6602 | `b2789cfefbb74ff1ce59ee4a2d618c8e179afee4197f0b124a0d140c9f1fe244` | `cd6d4cf13f8a2d670f5cb2d602ad4a393864cd04` |
| `src/test/java/com/taskapp/task_scheduler/TaskSchedulerApplicationTests.java` | 306 | `002f2134f5229cc38d6794537ba694343bbbadfcc8bf531ed9126f4ca00894e7` | `3be790df4cf35534abb5d603624d114e98d38df0` |
