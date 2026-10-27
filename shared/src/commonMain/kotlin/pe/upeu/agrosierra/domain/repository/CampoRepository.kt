package pe.upeu.agrosierra.domain.repository
import kotlinx.coroutines.flow.Flow
import pe.upeu.agrosierra.domain.model.*
interface CampoRepository {
    fun observarLotes(): Flow<List<Lote>>
    fun observarLabores(): Flow<List<Labor>>
    fun observarPendientes(): Flow<Map<Long,Long>>
    fun ultimaSincronizacion(): Long?
    suspend fun sincronizar(): ResumenSync
    suspend fun guardar(labor: Labor): Labor
    suspend fun eliminar(id: Long)
    suspend fun cerrar(id: Long)
}
