package pe.upeu.agrosierra.di
import org.koin.dsl.module
import org.koin.core.module.dsl.viewModel
import pe.upeu.agrosierra.data.local.*
import pe.upeu.agrosierra.data.remote.*
import pe.upeu.agrosierra.data.repository.CampoRepositorioOffline
import pe.upeu.agrosierra.domain.repository.CampoRepository
import pe.upeu.agrosierra.domain.usecase.*
import pe.upeu.agrosierra.presentation.listado.CampoViewModel
val redModule=module { single { EstadoSesion(get()) }; single { AgroApi(get()) } }
val dataModule=module { single { BaseLocal(get()) };single<CampoRepository> { CampoRepositorioOffline(get(),get(),get()) } }
val domainModule=module { factory { SincronizarUseCase(get()) };factory { GuardarLaborUseCase(get()) };factory { FiltrarLotesUseCase() } }
val presentationModule=module { viewModel { CampoViewModel(get(),get(),get(),get(),get(),get(),get()) } }
