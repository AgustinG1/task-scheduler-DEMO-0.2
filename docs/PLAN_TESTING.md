# Estado de las pruebas JUnit

Actualizado el 27 de septiembre de 2026.

`mvn test -B -ntp` ejecutó **11 pruebas: 11 correctas, 0 fallidas, 0 omitidas**. Compiló el código con objetivo Java 17 y utilizó el perfil `local` con H2 en memoria para las pruebas de integración. La clase de integración usa transacciones que revierten los datos al terminar cada prueba.

Las dos pruebas que fallaban en la primera revisión ahora pasan: una tarea específica compatible se cubre cada semana y cero semanas se rechaza antes de archivar una planilla previa. Se añadieron dos pruebas adicionales para semanas negativas o superiores a 52 y para tareas específicas con autorizaciones superpuestas que requieren redistribuir personas. También siguen pasando las pruebas de rotación, autorización por área, asignación única semanal, archivo global, Excel, validación de listas vacías y carga del contexto.

El motor conserva su diseño: intercalado aleatorio por área, rotación semanal, prioridad a tareas con pocos candidatos, historial de ciclos y tres niveles de preferencia para personas. La búsqueda interna de reasignación evita huecos cuando una combinación completa es posible. No depende de una librería externa para esa búsqueda.

## Límites aún abiertos

Cuando la configuración no permite cubrir todas las tareas, el motor genera cobertura parcial sin registrar ni señalar cuáles faltaron. No se ha fijado el contrato de producto para ese caso. Las preferencias de descanso y variedad pueden ceder ante la cobertura; no son garantías absolutas. Tampoco se han probado aún solicitudes de generación concurrentes, el esquema MySQL real ni la presentación HTTP de todos los errores de configuración.

Los próximos casos de prueba útiles son: tareas específicas sin áreas autorizadas; más tareas que personas; tareas duplicadas entre catálogos; y repetición inevitable. Esos casos deben formular primero la expectativa de negocio. No son necesarios para validar las dos correcciones entregadas en esta etapa.

Desde la carpeta con `pom.xml`:

```powershell
.\mvnw.cmd test
```

El reporte detallado se guarda en `target/surefire-reports/`.
