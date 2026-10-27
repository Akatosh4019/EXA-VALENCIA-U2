package pe.upeu.agrosierra.presentation.listado

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import pe.upeu.agrosierra.domain.model.*
import pe.upeu.agrosierra.domain.repository.*
import pe.upeu.agrosierra.domain.usecase.*
import pe.upeu.agrosierra.data.local.EstadoSesion
import pe.upeu.agrosierra.data.remote.AgroApi
import kotlin.time.Clock
import kotlin.time.Instant

data class CampoUiState(val sesion: Sesion?=null,val lotes: List<Lote> = emptyList(),val visibles: List<Lote> = emptyList(),val labores: List<Labor> = emptyList(),val seleccionado: Long?=null,val busqueda: String="",val valle: String="",val soloAbiertas: Boolean=false,val ocupado: Boolean=false,val mensaje: String?=null,val error: String?=null,val campos: Map<String,String> = emptyMap(),val ultima: Long?=null,val reintentar: Boolean=false,val guardado: Int=0,val pendientes: Map<Long,Long> = emptyMap())

class CampoViewModel(private val repo: CampoRepository,private val api: AgroApi,private val estado: EstadoSesion,private val conexion: Conexion,private val sincronizar: SincronizarUseCase,private val guardarLabor: GuardarLaborUseCase,private val filtrar: FiltrarLotesUseCase): ViewModel() {
    private val _ui=MutableStateFlow(CampoUiState(sesion=estado.sesion.value,ultima=repo.ultimaSincronizacion()))
    val ui=_ui.asStateFlow()
    init {
        viewModelScope.launch { repo.observarPendientes().collect { cantidades -> _ui.update { it.copy(pendientes=cantidades) } } }
        viewModelScope.launch { repo.observarLotes().collect { lista -> _ui.update { it.copy(lotes=lista,visibles=filtrar(lista,it.busqueda,it.valle,it.soloAbiertas)) } } }
        viewModelScope.launch { repo.observarLabores().collect { lista -> _ui.update { it.copy(labores=lista) } } }
        viewModelScope.launch { estado.sesion.collect { s -> _ui.update { it.copy(sesion=s,seleccionado=null,error=estado.aviso.value) } } }
        viewModelScope.launch { conexion.observar().distinctUntilChanged().collect { disponible -> if(disponible && estado.sesion.value!=null) actualizar() } }
        if(estado.sesion.value!=null) actualizar()
    }
    private fun tarea(bloque: suspend () -> Unit) {
        if(_ui.value.ocupado) return
        _ui.update { it.copy(ocupado=true,error=null,campos=emptyMap(),mensaje=null,reintentar=false) }
        viewModelScope.launch {
            try { bloque() } catch(e: CancellationException) { throw e } catch(e: Exception) {
                val error=(e as? ErrorApiException)?.error
                _ui.update { it.copy(error=e.message ?: "No se pudo completar la operación",campos=(error as? ErrorApi.Validacion)?.porCampo.orEmpty(),reintentar=error in setOf(ErrorApi.Servidor,ErrorApi.SinConexion,ErrorApi.TiempoAgotado)) }
                if(error==ErrorApi.NoEncontrado) { runCatching { sincronizar() }; _ui.update { it.copy(seleccionado=null) } }
            } finally { _ui.update { it.copy(ocupado=false) } }
        }
    }
    fun login(usuario: String,password: String)=tarea {
        val s=try { api.login(usuario,password) } catch(e: ErrorApiException) { if(e.error==ErrorApi.NoAutenticado) throw ErrorApiException(ErrorApi.CredencialesInvalidas); throw e }
        estado.establecer(s); val r=sincronizar(); _ui.update { it.copy(ultima=repo.ultimaSincronizacion(),mensaje="${r.lotes} lotes y ${r.labores} labores descargados") }
    }
    fun actualizar()=tarea {
        val usuario=api.me(); estado.sesion.value?.let { estado.establecer(it.copy(privilegios=usuario.privilegios,rol=usuario.rol,puedeEditarAjenas=usuario.puedeEditarAjenas)) }
        val r=sincronizar(); _ui.update { it.copy(ultima=repo.ultimaSincronizacion(),mensaje="Enviados: ${r.enviados}. Recibidos: ${r.lotes} lotes y ${r.labores} labores") }
    }
    fun buscar(texto: String) { _ui.update { it.copy(busqueda=texto,visibles=filtrar(it.lotes,texto,it.valle,it.soloAbiertas)) } }
    fun valle(valor: String) { _ui.update { it.copy(valle=valor,visibles=filtrar(it.lotes,it.busqueda,valor,it.soloAbiertas)) } }
    fun soloAbiertas(valor: Boolean) { _ui.update { it.copy(soloAbiertas=valor,visibles=filtrar(it.lotes,it.busqueda,it.valle,valor)) } }
    fun seleccionar(id: Long?) { _ui.update { it.copy(seleccionado=id,error=null,campos=emptyMap(),mensaje=null) } }
    fun guardar(l: Labor)=tarea { val resultado=guardarLabor(l); _ui.update { it.copy(guardado=it.guardado+1,mensaje=if(resultado.pendiente) "Labor guardada localmente. Pendiente de sincronizar" else "Labor guardada en el servidor") } }
    fun eliminar(id: Long)=tarea { repo.eliminar(id); _ui.update { it.copy(mensaje="Labor eliminada") } }
    fun cerrar(id: Long)=tarea { repo.cerrar(id); _ui.update { it.copy(mensaje="Campaña cerrada") } }
    fun salir() { estado.limpiar(); _ui.update { CampoUiState(lotes=it.lotes,labores=it.labores) } }
    fun puedeEditar(l: Labor): Boolean {
        val s=_ui.value.sesion ?: return false
        if("LABOR_EDITAR" !in s.privilegios) return false
        return s.puedeEditarAjenas || (l.autorId==s.id && runCatching { Clock.System.now()-Instant.parse(l.registradaEn) < kotlin.time.Duration.parse("24h") }.getOrDefault(false))
    }
}
