package pe.upeu.agrosierra

import kotlin.test.*
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlinx.io.IOException
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import pe.upeu.agrosierra.data.local.*
import pe.upeu.agrosierra.data.remote.*
import pe.upeu.agrosierra.data.remote.dto.*
import pe.upeu.agrosierra.data.repository.CampoRepositorioOffline
import pe.upeu.agrosierra.domain.model.*

internal expect fun baseEnMemoria(): AgroDatabase
private class MemoriaSegura: AlmacenSeguro {
    private var token: String?=null
    override fun leerToken()=token
    override fun guardarToken(token: String) { this.token=token }
    override fun borrarToken() { token=null }
}
class FlujoOfflineTest {
    private fun sesion()=EstadoSesion(MemoriaSegura()).apply { establecer(Sesion("token-prueba","tecnico1","TECNICO",setOf("LABOR_CREAR"),1,"Técnico")) }
    private fun labor()=Labor(0,1,TipoLabor.RIEGO,"2026-10-27","Técnico",2.0,"Prueba",true)
    private val headers=headersOf(HttpHeaders.ContentType,"application/json")
    private val remoto="""{"id":25,"loteId":1,"tipo":"RIEGO","fecha":"2026-10-27","responsable":"Técnico","cantidad":2.0,"observacion":"Prueba"}"""
    private val lote="""{"id":1,"codigo":"LOT-001","nombre":"Parcela","hectareas":3.0,"cultivo":"Maíz","valle":"Mantaro","campaniaCerrada":false,"dosisMaximaPorHectarea":5.0}"""
    @Test fun convierteDtoSinContaminarDominio() {
        val dto=Json { ignoreUnknownKeys=true }.decodeFromString<LoteDto>(lote.dropLast(1)+",\"campoNuevo\":true}")
        val model=dto.aDominio();assertEquals("LOT-001",model.codigo);assertEquals(3.0,model.hectareas);assertFalse(model.campaniaCerrada)
    }
    @Test fun traduceValidacion400ConCampo()=runTest {
        val estado=sesion();val client=crearCliente(MockEngine { respond("""{"message":"Dosis excesiva","validationErrors":{"cantidad":"Máximo 15"}}""",HttpStatusCode.BadRequest,headers) },estado,"http://localhost/api/v1/")
        try { val error=assertFailsWith<ErrorApiException> { AgroApi(client).guardar(labor().copy(id=-1)) };assertEquals("Máximo 15",(error.error as ErrorApi.Validacion).porCampo["cantidad"]) } finally { client.close() }
    }
    @Test fun registraSinRedYEmiteDesdeSQLite()=runTest {
        val estado=sesion();val local=BaseLocal(baseEnMemoria());val client=crearCliente(MockEngine { throw IOException("sin red") },estado,"http://localhost/api/v1/")
        try { val repo=CampoRepositorioOffline(AgroApi(client),local,estado);val guardado=repo.guardar(labor());assertTrue(guardado.id<0);assertTrue(guardado.pendiente);assertEquals(guardado.id,repo.observarLabores().first().single().id) } finally { client.close() }
    }
    @Test fun subeAntesDeDescargarYNoDuplicaAlRepetir()=runTest {
        val pasos=mutableListOf<String>();val estado=sesion();val local=BaseLocal(baseEnMemoria())
        local.guardar(labor().copy(id=-1,propietarioPendiente="tecnico1",claveCliente="op-1"))
        val client=crearCliente(MockEngine { request ->
            pasos+="${request.method.value} ${request.url.encodedPath}"
            when {
                request.method==HttpMethod.Post -> respond(remoto,HttpStatusCode.Created,headers)
                request.url.encodedPath.endsWith("/lotes") -> respond("""{"contenido":[$lote],"ultima":true}""",headers=headers)
                else -> respond("""{"contenido":[$remoto],"ultima":true}""",headers=headers)
            }
        },estado,"http://localhost/api/v1/")
        try {
            val repo=CampoRepositorioOffline(AgroApi(client),local,estado)
            assertEquals(1,repo.sincronizar().enviados);assertTrue(pasos.first().startsWith("POST"))
            assertEquals(0,repo.sincronizar().enviados);assertEquals(1,pasos.count { it.startsWith("POST") })
            val lista=repo.observarLabores().first();assertEquals(1,lista.size);assertEquals(25,lista.single().id);assertFalse(lista.single().pendiente)
        } finally { client.close() }
    }
    @Test fun falloDeDescargaConservaLosDatosLocales()=runTest {
        val estado=sesion();val local=BaseLocal(baseEnMemoria());local.guardar(labor().copy(id=7,pendiente=false))
        val client=crearCliente(MockEngine { throw IOException("sin red") },estado,"http://localhost/api/v1/")
        try { val repo=CampoRepositorioOffline(AgroApi(client),local,estado);assertFailsWith<ErrorApiException> { repo.sincronizar() };assertEquals(7,repo.observarLabores().first().single().id) } finally { client.close() }
    }
    @Test fun cerrarSesionBorraElTokenGuardado() {
        val almacen=MemoriaSegura();val estado=EstadoSesion(almacen);estado.establecer(Sesion("secreto","u","r",emptySet()));estado.limpiar();assertNull(almacen.leerToken());assertNull(estado.sesion.value)
    }
}
