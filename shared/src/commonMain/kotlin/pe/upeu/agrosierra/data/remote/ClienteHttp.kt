package pe.upeu.agrosierra.data.remote

import io.ktor.client.*
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import pe.upeu.agrosierra.data.local.EstadoSesion

object ApiConfig { const val BASE_URL="http://10.0.2.2:8082/api/v1/" }
fun crearCliente(engine: HttpClientEngine, estado: EstadoSesion, urlBase: String=ApiConfig.BASE_URL, debug: Boolean=false)=HttpClient(engine) {
    expectSuccess=true
    install(ContentNegotiation) { json(Json { ignoreUnknownKeys=true; encodeDefaults=true }) }
    install(HttpTimeout) { requestTimeoutMillis=15000; connectTimeoutMillis=10000 }
    install(Logging) { level=if(debug) LogLevel.HEADERS else LogLevel.NONE; sanitizeHeader { it.equals(HttpHeaders.Authorization,true) } }
    defaultRequest {
        url(urlBase); contentType(ContentType.Application.Json)
        if (!url.encodedPath.endsWith("auth/login")) estado.sesion.value?.token?.let { headers.append(HttpHeaders.Authorization,"Bearer $it") }
    }
    HttpResponseValidator {
        handleResponseExceptionWithRequest { error,request ->
            if(error is ClientRequestException && error.response.status.value==401 && !request.url.encodedPath.endsWith("auth/login")) estado.limpiar("Tu sesión expiró. Inicia sesión nuevamente")
        }
    }
}
