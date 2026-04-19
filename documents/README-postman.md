# Postman Test Pack — Document Processing Service

This package contains:

- `Document-Processing-Service.postman_collection.json`
- `local.postman_environment.json`

## Assumptions

The collection is aligned with the contract discussed for the challenge:

- `POST /api/v1/processes` creates a process in `PENDING`
- `POST /api/v1/processes/{id}/authorize` moves `PENDING -> RUNNING`
- `POST /api/v1/processes/{id}/pause` is valid only for `RUNNING`
- `POST /api/v1/processes/{id}/resume` is valid only for `PAUSED`
- `POST /api/v1/processes/{id}/stop` is valid for `PENDING`, `RUNNING`, `PAUSED`
- `GET /api/v1/processes/{id}/results` distinguishes `NONE`, `PARTIAL`, `FINAL`

## Recommended execution order

1. Import the environment and adjust:
   - `base_url`
   - `source_folder`
   - `selected_file_1`
   - `selected_file_2`
2. Run folder `00 - Bootstrap`
3. Run folder `01 - Lifecycle Happy Path`
4. Run folder `02 - Stop Scenarios`
5. Run folders `03 - Negative State Transitions`, `04 - Validation Errors`, `05 - Results Semantics`
6. Run `06 - Activity Log` only if the activity endpoint is implemented
7. Run `07 - List & Filters`

## Notes

- The polling request (`Poll Status Until Target State`) uses collection variables:
  - `target_status`
  - `max_retries`
  - `retries`
- Some negative tests depend on prior scenario setup and may return `404` if the referenced process id was not captured.
- The `activity` endpoint is optional relative to the original challenge, even though activity logs themselves are required by the specification analysis.
- The collection is intentionally strict about invalid state transitions and expects `409 Conflict` in those cases.

## Newman

Example:

```bash
newman run Document-Processing-Service.postman_collection.json -e local.postman_environment.json
```
