package pe.upeu.agrosierra.data.local

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.*
import kotlin.io.encoding.Base64
import kotlin.time.Clock
import pe.upeu.agrosierra.domain.model.Sesion

class EstadoSesion(private val almacen: AlmacenSeguro) {
    private val actual=MutableStateFlow(restaurar())
    val sesion=actual.asStateFlow()
    val aviso=MutableStateFlow<String?>(null)
    fun establecer(s: Sesion) { almacen.guardarToken(s.token); actual.value=s; aviso.value=null }
    fun limpiar(mensaje: String?=null) { almacen.borrarToken(); actual.value=null; aviso.value=mensaje }
    private fun restaurar(): Sesion? {
        val token=almacen.leerToken() ?: return null
        return runCatching {
            val parte=token.split('.')[1]
            val obj=Json.parseToJsonElement(Base64.UrlSafe.decode(parte.padEnd((parte.length+3)/4*4,'=')).decodeToString()).jsonObject
            if ((obj["exp"]?.jsonPrimitive?.long ?: 0) <= Clock.System.now().epochSeconds) { almacen.borrarToken(); return null }
            Sesion(token,obj.getValue("sub").jsonPrimitive.content,obj.getValue("rol").jsonPrimitive.content,
                obj.getValue("privilegios").jsonArray.map { it.jsonPrimitive.content }.toSet(),
                obj.getValue("usuarioId").jsonPrimitive.long,obj.getValue("nombre").jsonPrimitive.content,
                obj["puedeEditarAjenas"]?.jsonPrimitive?.booleanOrNull ?: false)
        }.getOrElse { almacen.borrarToken(); null }
    }
}
