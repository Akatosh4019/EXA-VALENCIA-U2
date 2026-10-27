# AgroAPI_Valencia — Examen Unidad 2

Backend REST del caso AgroSierra. Estudiante: Roberto Samuel Valencia Saavedra.

## Requisitos y ejecución

Docker Desktop en modo Linux. En PowerShell:

```powershell
# crear .env con un secreto local (no se versiona)
"JWT_SECRET=$( [Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(32)) )" | Set-Content -Encoding ascii .env
docker compose up --build -d
docker compose ps
```

API local: `http://localhost:8082/api/v1`; Swagger UI: `http://localhost:8082/swagger-ui/index.html`. La base H2 persiste en un volumen Docker. Para parar: `docker compose down` (conserva volumen); `docker compose down -v` elimina los datos locales.

## Usuarios semilla

| Usuario | Clave | Rol |
|---|---|---|
| tecnico1 | Agro2026* | TECNICO |
| jefe1 | Agro2026* | JEFE_CAMPO |
| admin | Agro2026* | ADMINISTRADOR |
| supervisor1 | Agro2026* | SUPERVISOR_CALIDAD |

Son cuentas para evaluación local.

## Rutas principales

- `POST /auth/login`, `GET /usuarios/me`
- `GET /lotes?pagina=0&tamanio=20`, `GET /lotes/{id}`, `GET /lotes/{id}/labores`
- `POST /labores`, `PUT /labores/{id}`, `DELETE /labores/{id}`
- `POST /lotes/{id}/cerrar-campania`

Las rutas de datos requieren JWT Bearer. El servicio valida privilegios, campañas cerradas, ventanas de corrección, dosis y jornales. Las altas incluyen `claveCliente` para hacer idempotentes los reintentos.

## Verificación local

```powershell
mvn -B test
python ..\probar_api.py
```

`probar_api.py` comprueba autenticación, permisos, errores de validación, alta/corrección de labor, jornales e idempotencia contra Docker. No guarda tokens y deja un resumen JSON en `Entregables/resultados-api.json`.

## Cambios Parte II

SC-B añade el supervisor de calidad con permiso para corregir labores ajenas y sin permiso para crear o cerrar campañas. SC-C añade jornales a la entidad, contratos, validación y semilla. SC-D corresponde al cliente móvil, que presenta pendientes locales por lote.
