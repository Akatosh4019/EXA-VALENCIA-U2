# AgroCampo Valencia - Aplicacion Android

**Examen Parcial II - Unidad 2**  
**Estudiante:** Roberto Samuel Valencia Saavedra

AgroCampo es el cliente Android del caso AgroSierra. La interfaz consulta el servicio REST AgroAPI y conserva lotes y labores en SQLite mediante SQLDelight. La pantalla principal observa la base local, por lo que los datos descargados siguen disponibles sin conexion. Las labores creadas sin red quedan pendientes y se envian al recuperar la conexion.

## Repositorios y arquitectura

- **Aplicacion:** [EXA-VALENCIA-U2](https://github.com/Akatosh4019/EXA-VALENCIA-U2)
- **Servicio REST:** [BACKEND-VALENCIA-U2](https://github.com/Akatosh4019/BACKEND-VALENCIA-U2)
- **Etiqueta evaluada:** `v1.0-unidad2`

El proyecto usa Kotlin Multiplatform con los modulos `androidApp` y `shared`. En `shared/src/commonMain/kotlin/pe/upeu/agrosierra` se organizan las capas:

```text
presentation/   App.kt y listado/CampoViewModel.kt; pantallas y estado observable
domain/         modelos, contratos de repositorio y casos de uso
 data/local/    SQLDelight, estado de sesion y contrato de almacenamiento seguro
 data/remote/   Cliente Ktor, AgroApi, DTO y traduccion de errores
 data/repository/ repositorio offline-first que coordina red y persistencia
 di/            dependencias de Koin
```

La base SQLDelight se define en `shared/src/commonMain/sqldelight/pe/upeu/agrosierra/AgroDb.sq`. El modulo `androidApp` contiene `MainActivity`, la configuracion Android y la implementacion del almacenamiento cifrado. Las pruebas compartidas estan en `shared/src/commonTest`.

**Recorrido de datos:** `App` envia acciones al `CampoViewModel`; este usa casos de uso y el repositorio. El repositorio guarda o consulta SQLite y coordina Ktor con AgroAPI. Los cambios de la base emiten flujos observables; el ViewModel actualiza su estado y Compose vuelve a dibujar la pantalla.

## Contrato del API consumido

La URL base esta centralizada en `shared/src/commonMain/kotlin/pe/upeu/agrosierra/data/remote/ClienteHttp.kt`:

- Emulador Android: `http://10.0.2.2:8082/api/v1/`
- Swagger del backend local: `http://localhost:8082/swagger-ui/index.html`

| Operacion | Solicitud | Respuesta / uso |
|---|---|---|
| Inicio de sesion | `POST /auth/login` con `usuario` y `password` | JWT, nombre, rol y privilegios |
| Sesion actual | `GET /usuarios/me` | Datos del usuario autenticado |
| Listar lotes | `GET /lotes?pagina=0&tamanio=20` | Paginas con `contenido`, `pagina`, `tamanio`, `totalElementos`, `totalPaginas` y `ultima` |
| Ver labores de lote | `GET /lotes/{id}/labores?pagina=0&tamanio=20` | Paginas de labores |
| Crear labor | `POST /labores` | Labor creada; incluye `loteId`, `tipo`, `fecha`, `cantidad`, `jornales`, `observacion` y `claveCliente` |
| Editar labor | `PUT /labores/{id}` | Labor actualizada |
| Eliminar labor | `DELETE /labores/{id}` | Eliminacion sin cuerpo |
| Cerrar campania | `POST /lotes/{id}/cerrar-campania` | Lote actualizado, si el usuario tiene privilegio |

Los DTO remotos se mantienen en `data/remote/dto` y se convierten a modelos de dominio; la interfaz no usa directamente las respuestas JSON. El cliente Ktor se configura una vez en DI y agrega centralmente `Authorization: Bearer <token>` a las rutas protegidas. El token se cifra con AES/GCM y una clave custodiada por Android Keystore.

## Roles y privilegios del caso

El backend es la autoridad que valida permisos; ocultar una accion en la pantalla no sustituye esa verificacion.

| Rol | Privilegios principales |
|---|---|
| `TECNICO` | `LOTE_LEER`, `LABOR_LEER`, `LABOR_CREAR`, `LABOR_EDITAR` |
| `JEFE_CAMPO` | Lectura de lotes y labores, crear/editar/eliminar labores y `CAMPANIA_CERRAR`; puede editar labores ajenas |
| `SUPERVISOR_CALIDAD` | Lectura de lotes y labores, `LABOR_EDITAR`; puede editar labores ajenas |
| `ADMINISTRADOR` | Lectura de lotes y labores, `USUARIO_GESTIONAR`; no tiene `LABOR_CREAR` |

Cuentas de demostracion sembradas por el backend: `tecnico1`, `jefe1`, `supervisor1` y `admin`; contrasena local de cada cuenta: `Agro2026*`.

## Estados, errores y modo sin conexion

- `401`: falta un token valido o la sesion vencio; la aplicacion limpia la sesion cuando corresponde.
- `403`: el usuario esta autenticado, pero no tiene el privilegio de la operacion.
- `400`: formulario o regla de validacion no aceptada; se muestran errores por campo cuando el servidor los devuelve.
- `404` y `409`: recurso ausente o conflicto/regla de negocio.
- Sin red: se muestran los datos locales y las labores nuevas se marcan pendientes. La sincronizacion sube pendientes con `claveCliente` para evitar duplicados y luego descarga los datos actualizados.

## Ejecutar en Android Studio

1. Clona `EXA-VALENCIA-U2` y abre la carpeta del proyecto en Android Studio.
2. Espera la sincronizacion de Gradle y selecciona un emulador Android.
3. Clona y levanta el backend `BACKEND-VALENCIA-U2` con Docker siguiendo su README. Debe escuchar en el puerto `8082`.
4. Ejecuta la configuracion `androidApp`. En el emulador, `10.0.2.2` apunta al equipo anfitrion.
5. Inicia sesion con `tecnico1` / `Agro2026*`, sincroniza y abre un lote para registrar una labor.

Para que las peticiones funcionen, el backend debe estar activo y accesible desde el emulador. El proyecto usa Android SDK 36 y requiere un dispositivo/emulador compatible con `minSdk 24`.

## Pruebas

Desde la raiz del repositorio, en PowerShell:

```powershell
.\gradlew.bat :shared:allTests
```

Las pruebas compartidas cubren filtros y comportamiento offline. La coleccion Postman para el contrato HTTP esta en `postman/AgroSierra.postman_collection.json`; los ejemplos de API requieren el backend ejecutandose.
