package pe.upeu.agrosierra.domain.model

sealed interface ErrorApi {
    data class Validacion(val porCampo: Map<String, String>) : ErrorApi
    data object NoEncontrado : ErrorApi
    data object NoAutenticado : ErrorApi
    data object CredencialesInvalidas : ErrorApi
    data object SinPermiso : ErrorApi
    data class Conflicto(val mensaje: String) : ErrorApi
    data object Servidor : ErrorApi
    data object SinConexion : ErrorApi
    data object TiempoAgotado : ErrorApi
    data object RespuestaInvalida : ErrorApi
}

fun ErrorApi.mensajeUsuario(): String = when (this) {
    is ErrorApi.Validacion -> "Revisa los datos marcados en el formulario"
    ErrorApi.NoEncontrado -> "El recurso ya no está disponible"
    ErrorApi.NoAutenticado -> "Tu sesión expiró. Inicia sesión nuevamente"
    ErrorApi.CredencialesInvalidas -> "Usuario o contraseña incorrectos"
    ErrorApi.SinPermiso -> "No tienes permiso para realizar esta acción"
    is ErrorApi.Conflicto -> mensaje
    ErrorApi.Servidor -> "El servidor no pudo completar la operación. Inténtalo de nuevo"
    ErrorApi.SinConexion -> "Sin conexión con el servidor. Comprueba tu red e inténtalo de nuevo"
    ErrorApi.TiempoAgotado -> "La solicitud tardó demasiado. Inténtalo de nuevo"
    ErrorApi.RespuestaInvalida -> "La respuesta del servidor no tiene el formato esperado"
}

class ErrorApiException(val error: ErrorApi) : Exception(error.mensajeUsuario())
