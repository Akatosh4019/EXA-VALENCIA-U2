package pe.upeu.agrosierra.data.remote

import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ServerResponseException
import kotlinx.coroutines.CancellationException
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException
import pe.upeu.agrosierra.data.remote.dto.ErrorResponseDto
import pe.upeu.agrosierra.domain.model.ErrorApi
import pe.upeu.agrosierra.domain.model.ErrorApiException

/** Único límite entre excepciones de red y errores del dominio. */
suspend fun <T> ejecutarLlamada(bloque: suspend () -> T): T = try {
    bloque()
} catch (e: HttpRequestTimeoutException) {
    throw ErrorApiException(ErrorApi.TiempoAgotado)
} catch (e: CancellationException) {
    // Ktor puede presentar el timeout de petición como CancellationException.
    if (e.message?.contains("Request timeout has expired", ignoreCase = true) == true) {
        throw ErrorApiException(ErrorApi.TiempoAgotado)
    }
    throw e
} catch (e: ClientRequestException) {
    val cuerpo = runCatching { e.response.body<ErrorResponseDto>() }.getOrNull()
    val error = when (e.response.status.value) {
        400 -> ErrorApi.Validacion(cuerpo?.validationErrors.orEmpty())
        401 -> ErrorApi.NoAutenticado
        403 -> ErrorApi.SinPermiso
        404 -> ErrorApi.NoEncontrado
        409 -> ErrorApi.Conflicto(cuerpo?.message ?: "Operación no permitida")
        else -> ErrorApi.Servidor
    }
    throw ErrorApiException(error)
} catch (e: ServerResponseException) {
    throw ErrorApiException(ErrorApi.Servidor)
} catch (e: SerializationException) {
    throw ErrorApiException(ErrorApi.RespuestaInvalida)
} catch (e: IOException) {
    throw ErrorApiException(ErrorApi.SinConexion)
}
