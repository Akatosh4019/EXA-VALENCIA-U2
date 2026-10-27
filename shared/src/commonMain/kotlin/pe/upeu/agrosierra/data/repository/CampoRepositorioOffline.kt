package pe.upeu.agrosierra.data.repository

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import pe.upeu.agrosierra.data.local.*
import pe.upeu.agrosierra.data.remote.AgroApi
import pe.upeu.agrosierra.domain.model.*
import pe.upeu.agrosierra.domain.repository.CampoRepository
import kotlin.time.Clock
import kotlin.random.Random

class CampoRepositorioOffline(private val api: AgroApi,private val local: BaseLocal,private val sesion: EstadoSesion): CampoRepository {
    private val mutex=Mutex()
    override fun observarLotes()=local.lotes()
    override fun observarLabores()=local.labores()
    override fun ultimaSincronizacion()=local.ultimaSincronizacion()
    override suspend fun sincronizar(): ResumenSync=mutex.withLock {
        val usuario=sesion.sesion.value?.usuario ?: throw ErrorApiException(ErrorApi.NoAutenticado)
        val pendientes=local.pendientes(usuario)
        pendientes.forEach { local.reemplazar(it.id,api.guardar(it)) }
        val lotes=api.lotes()
        val labores=lotes.flatMap { api.labores(it.id) }
        local.actualizar(lotes,labores,Clock.System.now().toEpochMilliseconds())
        ResumenSync(pendientes.size,lotes.size,labores.size)
    }
    override suspend fun guardar(labor: Labor): Labor=mutex.withLock {
        val u=sesion.sesion.value ?: throw ErrorApiException(ErrorApi.NoAutenticado)
        val anterior=if(labor.id!=0L) local.buscar(labor.id) else null
        val pendiente=labor.copy(id=if(labor.id==0L) local.siguienteId() else labor.id,pendiente=true,
            propietarioPendiente=u.usuario, responsable=anterior?.responsable ?: u.nombre,
            autorId=anterior?.autorId ?: u.id,registradaEn=anterior?.registradaEn ?: Clock.System.now().toString(),
            claveCliente=anterior?.claveCliente?.takeIf { it.isNotBlank() } ?: "${u.id}-${Clock.System.now().toEpochMilliseconds()}-${Random.nextLong()}")
        local.guardar(pendiente)
        try { api.guardar(pendiente).also { local.reemplazar(pendiente.id,it) } }
        catch(e: Exception) {
            if((e as? ErrorApiException)?.error in setOf(ErrorApi.SinConexion,ErrorApi.TiempoAgotado)) pendiente
            else { if(anterior!=null) local.guardar(anterior) else local.borrar(pendiente.id); throw e }
        }
    }
    override suspend fun eliminar(id: Long) { mutex.withLock { if(id>0) api.eliminar(id); local.borrar(id) } }
    override suspend fun cerrar(id: Long) { mutex.withLock { local.guardarLote(api.cerrar(id)) } }
}
