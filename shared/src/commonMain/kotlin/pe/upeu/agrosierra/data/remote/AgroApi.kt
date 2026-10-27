package pe.upeu.agrosierra.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.*
import pe.upeu.agrosierra.data.remote.dto.*
import pe.upeu.agrosierra.domain.model.*

class AgroApi(private val client: HttpClient) {
    suspend fun login(usuario: String,password: String): Sesion = ejecutarLlamada { client.post("auth/login") { setBody(LoginRequest(usuario,password)) }.body<SesionDto>().aDominio() }
    suspend fun me(): SesionDto=ejecutarLlamada { client.get("usuarios/me").body() }
    suspend fun lotes(): List<Lote> = ejecutarLlamada {
        val lista=mutableListOf<Lote>(); var pagina=0
        do { val p=client.get("lotes") { parameter("pagina",pagina++);parameter("tamanio",20) }.body<PaginaDto<LoteDto>>(); lista+=p.contenido.map { it.aDominio() } } while(!p.ultima)
        lista
    }
    suspend fun labores(id: Long): List<Labor> = ejecutarLlamada {
        val lista=mutableListOf<Labor>(); var pagina=0
        do { val p=client.get("lotes/$id/labores") { parameter("pagina",pagina++);parameter("tamanio",20) }.body<PaginaDto<LaborDto>>();lista+=p.contenido.map { it.aDominio() } } while(!p.ultima)
        lista
    }
    suspend fun guardar(l: Labor): Labor=ejecutarLlamada {
        val response=if(l.id<0) client.post("labores") { setBody(l.aRequest()) } else client.put("labores/${l.id}") { setBody(l.aRequest()) }
        response.body<LaborDto>().aDominio()
    }
    suspend fun eliminar(id: Long): Unit=ejecutarLlamada { client.delete("labores/$id"); Unit }
    suspend fun cerrar(id: Long): Lote=ejecutarLlamada { client.post("lotes/$id/cerrar-campania").body<LoteDto>().aDominio() }
}
