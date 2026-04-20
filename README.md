# Chronicle

Chronicle es un servicio de procesamiento documental asincrono con lifecycle explicito, runtime con lease y API REST para control operativo, status, results y activity.

La base H2 in-memory se inicializa automaticamente al arrancar mediante `schema.sql`. No hace falta ejecutar scripts manuales ni preparar la base por fuera de la aplicacion.

---

## Ejecutar localmente

### Requisitos

- Java 21
- Maven 3.9+

### Arranque local

```bash
mvn spring-boot:run
```

### Accesos locales

* API publica: `http://localhost:8080/api/v1`
* Actuator: `http://localhost:8080/actuator`
* H2 console: `http://localhost:8080/h2-console`

La consola H2 sirve solo para inspeccion manual. No es necesaria para que Chronicle cree el esquema o funcione.

### Configuracion H2 por defecto

* JDBC URL: `jdbc:h2:mem:chronicle`
* user: `sa`
* password: vacio

---

## Tests

```bash
mvn test
```

---

## Datos de demo

Los archivos de demo para procesamiento se encuentran en:

```text
sample-data/docs
```

Esa carpeta contiene los documentos `.txt` de ejemplo que pueden usarse para crear procesos y validar el funcionamiento del runtime, el progreso, los results y la activity.

---

## Endpoints publicos disponibles hoy

* `POST /api/v1/processes`
* `POST /api/v1/processes/{process_id}/authorize`
* `POST /api/v1/processes/{process_id}/pause`
* `POST /api/v1/processes/{process_id}/resume`
* `POST /api/v1/processes/{process_id}/stop`
* `GET /api/v1/processes`
* `GET /api/v1/processes/{process_id}/status`
* `GET /api/v1/processes/{process_id}/results`
* `GET /api/v1/processes/{process_id}/activity`

---

## Actuator expuesto hoy

* `GET /actuator/health`
* `GET /actuator/health/readiness`
* `GET /actuator/info`
* `GET /actuator/metrics`

En observabilidad, los scans vacios del dispatcher no se emiten en `INFO` para evitar ruido cuando el sistema esta idle. Los eventos operativamente relevantes del runtime siguen visibles en `INFO`.

---

## Runtime actual

* dispatcher con scan periodico de procesos `RUNNING`
* worker por `process_id`
* lease persistido con expiracion y renewal
* checkpoint seguro al cierre de documento
* recovery ligero para documentos huerfanos en `PROCESSING`

---

## Limitaciones actuales

* H2 es in-memory
* no hay persistencia durable entre reinicios
* `results` y `activity` son funcionales pero siguen siendo una proyeccion minima de la evidencia disponible
* el runtime actual es suficiente para demo y validacion funcional, no para despliegue distribuido o durabilidad productiva

---

## Guia rapida para el evaluador

### 1. Documentacion del proyecto

La carpeta `documents/` contiene la documentacion principal del desafio y del servicio. Alli se encuentra, entre otros materiales:

* el PDF original del challenge
* el RFC principal del lifecycle
* los PDR de ownership y runtime
* los ADR de runtime, persistencia, politica de fallo y stack tecnico
* el addendum arquitectonico
* el SDD del servicio
* la especificacion OpenAPI
* las convenciones generales
* los reportes de implementacion por incrementos
* la coleccion Postman
* el environment local de Postman
* el README especifico de Postman

### 2. Orden sugerido de lectura

Para una revision tecnica rapida y ordenada, se recomienda este recorrido:

1. `documents/00. Plural IT - Challenge.pdf`
2. `documents/11. Convenciones generales.md`
3. `documents/10. OpenAPI.md`
4. `documents/08. SDD-DPS-SERVICE-001.md`
5. `documents/09. ADR-DPS-TECH-STACK-004.md`
6. reportes de incrementos (`documents/12...` en adelante), para seguir la implementacion paso a paso

### 3. Validacion funcional sugerida

Un flujo simple de validacion seria:

1. levantar el servicio
2. verificar `health` y `readiness`
3. crear un proceso apuntando a archivos en `sample-data/docs`
4. autorizar el proceso
5. consultar `status`
6. consultar `results`
7. consultar `activity`

### 4. Coleccion Postman

La carpeta `documents/` incluye:

* `Document-Processing-Service.postman_collection.json`
* `local.postman_environment.json`
* `README-postman.md`

Se recomienda usarlos para ejecutar la validacion manual de la API.

---

## Estructura documental relevante

### `documents/`

Contiene la documentacion funcional, arquitectonica y de seguimiento del proyecto.

### `sample-data/docs/`

Contiene los archivos de ejemplo para demo y procesamiento documental.

---

## Nota final

Chronicle fue desarrollado con una estrategia incremental, documentada paso a paso. La carpeta `documents/` no es solo material de apoyo: funciona tambien como evidencia del proceso de diseno, decisiones tecnicas e implementacion del servicio.
