package pe.upeu.agrosierra.domain.usecase
import pe.upeu.agrosierra.domain.model.*
import pe.upeu.agrosierra.domain.repository.CampoRepository
class SincronizarUseCase(private val repo: CampoRepository) { suspend operator fun invoke()=repo.sincronizar() }
class GuardarLaborUseCase(private val repo: CampoRepository) {
    suspend operator fun invoke(l: Labor): Labor {
        val errores=buildMap { if(!Regex("\\d{4}-\\d{2}-\\d{2}").matches(l.fecha)) put("fecha","Use AAAA-MM-DD"); if(!l.cantidad.isFinite() || l.cantidad<=0) put("cantidad","Ingrese una cantidad mayor que cero"); if(l.observacion.length>500) put("observacion","Máximo 500 caracteres") }
        if(errores.isNotEmpty()) throw ErrorApiException(ErrorApi.Validacion(errores))
        return repo.guardar(l)
    }
}
class FiltrarLotesUseCase { operator fun invoke(lotes: List<Lote>,busqueda: String,valle: String,soloAbiertas: Boolean=false)=lotes.filter { (!soloAbiertas || !it.campaniaCerrada) && (valle.isBlank() || it.valle==valle) && (it.nombre.contains(busqueda,true) || it.codigo.contains(busqueda,true)) } }
