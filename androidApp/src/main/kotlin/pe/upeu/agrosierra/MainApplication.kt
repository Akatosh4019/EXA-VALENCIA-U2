package pe.upeu.agrosierra
import android.app.Application
import org.koin.core.context.startKoin
import org.koin.android.ext.koin.androidContext
import pe.upeu.agrosierra.di.*
class MainApplication: Application() {
    override fun onCreate() { super.onCreate(); startKoin { androidContext(this@MainApplication);modules(platformModule,redModule,dataModule,domainModule,presentationModule) } }
}
