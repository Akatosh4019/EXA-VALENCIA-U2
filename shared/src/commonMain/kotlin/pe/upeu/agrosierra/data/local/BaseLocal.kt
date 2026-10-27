package pe.upeu.agrosierra.data.local

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import pe.upeu.agrosierra.domain.model.*

class BaseLocal(private val db: AgroDatabase) {
    private val q=db.agroDbQueries
    private fun lote(id: Long,codigo: String,nombre: String,hectareas: Double,cultivo: String,valle: String,cerrada: Long,dosis: Double)=Lote(id,codigo,nombre,hectareas,cultivo,valle,cerrada!=0L,dosis)
    private fun labor(id: Long,loteId: Long,tipo: String,fecha: String,responsable: String,cantidad: Double,observacion: String,pendiente: Long,autor: Long,registrada: String,clave: String,propietario: String)=Labor(id,loteId,TipoLabor.valueOf(tipo),fecha,responsable,cantidad,observacion,pendiente!=0L,autor,registrada,clave,propietario)
    fun lotes()=q.observarLotes(::lote).asFlow().mapToList(Dispatchers.Default)
    fun labores()=q.observarLabores(::labor).asFlow().mapToList(Dispatchers.Default)
    fun pendientes(usuario: String)=q.pendientes(usuario,::labor).executeAsList()
    fun buscar(id: Long)=q.buscarLabor(id,::labor).executeAsOneOrNull()
    fun siguienteId()=minOf(-1L,q.siguienteIdLocal().executeAsOne()-1)
    fun guardar(l: Labor) { q.guardarLabor(l.id,l.loteId,l.tipo.name,l.fecha,l.responsable,l.cantidad,l.observacion,if(l.pendiente)1 else 0,l.autorId,l.registradaEn,l.claveCliente,l.propietarioPendiente) }
    fun borrar(id: Long)=q.borrarLabor(id)
    fun reemplazar(id: Long,l: Labor) { db.transaction { q.borrarLabor(id); guardar(l.copy(pendiente=false)) } }
    fun guardarLote(l: Lote)=q.guardarLote(l.id,l.codigo,l.nombre,l.hectareas,l.cultivo,l.valle,if(l.campaniaCerrada)1 else 0,l.dosisMaximaPorHectarea)
    fun actualizar(lotes: List<Lote>,labores: List<Labor>,instante: Long) {
        db.transaction {
            q.borrarLotes(); lotes.forEach(::guardarLote)
            q.borrarSincronizadas()
            labores.forEach { if(buscar(it.id)?.pendiente!=true) guardar(it) }
            q.guardarSincronizacion(instante)
        }
    }
    fun ultimaSincronizacion()=q.ultimaSincronizacion().executeAsOneOrNull()
}
