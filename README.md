# Chronicle

Chronicle es un servicio de procesamiento documental asincrono con lifecycle explicito, runtime con lease y API REST para control operativo, status, results y activity.

## Ejecutar localmente

Requisitos:

- Java 21
- Maven 3.9+

Arranque local:

```bash
mvn spring-boot:run
```

Accesos locales:

- API publica: `http://localhost:8080/api/v1`
- Actuator: `http://localhost:8080/actuator`
- H2 console: `http://localhost:8080/h2-console`

Configuracion H2 por defecto:

- JDBC URL: `jdbc:h2:mem:chronicle`
- user: `sa`
- password: vacio

## Tests

```bash
mvn test
```

## Endpoints publicos disponibles hoy

- `POST /api/v1/processes`
- `POST /api/v1/processes/{process_id}/authorize`
- `POST /api/v1/processes/{process_id}/pause`
- `POST /api/v1/processes/{process_id}/resume`
- `POST /api/v1/processes/{process_id}/stop`
- `GET /api/v1/processes`
- `GET /api/v1/processes/{process_id}/status`
- `GET /api/v1/processes/{process_id}/results`
- `GET /api/v1/processes/{process_id}/activity`

## Actuator expuesto hoy

- `GET /actuator/health`
- `GET /actuator/health/readiness`
- `GET /actuator/info`
- `GET /actuator/metrics`

## Runtime actual

- dispatcher con scan periodico de procesos `RUNNING`
- worker por `process_id`
- lease persistido con expiracion y renewal
- checkpoint seguro al cierre de documento
- recovery ligero para documentos huerfanos en `PROCESSING`

## Limitaciones actuales

- H2 es in-memory
- no hay persistencia durable entre reinicios
- `results` y `activity` son funcionales pero siguen siendo una proyeccion minima de la evidencia disponible
- el runtime actual es suficiente para demo y validacion funcional, no para despliegue distribuido o durabilidad productiva
