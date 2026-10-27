package pe.upeu.agrosierra
import kotlin.test.*
import pe.upeu.agrosierra.domain.model.Lote
import pe.upeu.agrosierra.domain.usecase.FiltrarLotesUseCase
class FiltrosTest {
    private val lotes=listOf(Lote(1,"A1","Norte",2.0,"Papa","Mantaro",false,4.0),Lote(2,"A2","Sur",2.0,"Papa","Mantaro",true,4.0),Lote(3,"B1","Norte",2.0,"Papa","Sagrado",false,4.0))
    @Test fun combinaValleConCampaniasAbiertas() { assertEquals(listOf(1L),FiltrarLotesUseCase()(lotes,"","Mantaro",true).map { it.id }) }
    @Test fun combinaBusquedaYFiltrosSinRed() { assertEquals(listOf(3L),FiltrarLotesUseCase()(lotes,"B1","Sagrado",true).map { it.id });assertTrue(FiltrarLotesUseCase()(lotes,"Sur","Mantaro",true).isEmpty()) }
    @Test fun desactivarFiltroConservaCerradas() { assertEquals(3,FiltrarLotesUseCase()(lotes,"","",false).size) }
}
