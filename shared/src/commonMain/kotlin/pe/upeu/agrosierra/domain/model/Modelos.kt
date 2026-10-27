package pe.upeu.agrosierra.domain.model

data class Lote(val id: Long, val codigo: String, val nombre: String, val hectareas: Double, val cultivo: String, val valle: String, val campaniaCerrada: Boolean, val dosisMaximaPorHectarea: Double)
enum class TipoLabor { RIEGO, FUMIGACION, COSECHA }
data class Labor(val id: Long, val loteId: Long, val tipo: TipoLabor, val fecha: String, val responsable: String, val cantidad: Double, val observacion: String, val pendiente: Boolean, val autorId: Long = 0, val registradaEn: String = "", val claveCliente: String = "", val propietarioPendiente: String = "", val jornales: Double = 0.0)
data class Sesion(val token: String, val usuario: String, val rol: String, val privilegios: Set<String>, val id: Long = 0, val nombre: String = usuario, val puedeEditarAjenas: Boolean = false) {
    override fun toString() = "Sesion(usuario=$usuario, rol=$rol, token=***)"
}
data class ResumenSync(val enviados: Int, val lotes: Int, val labores: Int)
