package pe.upeu.agrosierra.domain.repository
import kotlinx.coroutines.flow.Flow
interface Conexion { fun observar(): Flow<Boolean> }
