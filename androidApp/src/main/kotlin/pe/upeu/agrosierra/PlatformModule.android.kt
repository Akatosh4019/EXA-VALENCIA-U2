package pe.upeu.agrosierra
import android.content.Context
import android.net.*
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import org.koin.dsl.module
import org.koin.android.ext.koin.androidContext
import pe.upeu.agrosierra.data.local.*
import pe.upeu.agrosierra.data.remote.crearCliente
import pe.upeu.agrosierra.domain.repository.Conexion
class ConexionAndroid(context: Context): Conexion {
    private val manager=context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    override fun observar()=callbackFlow {
        fun estado() { val capacidades=manager.getNetworkCapabilities(manager.activeNetwork);trySend(capacidades?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)==true) }
        val callback=object: ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { trySend(true) }
            override fun onLost(network: Network) { estado() }
        }
        manager.registerDefaultNetworkCallback(callback);estado()
        awaitClose { manager.unregisterNetworkCallback(callback) }
    }
}
val platformModule=module {
    single<AlmacenSeguro> { AlmacenSeguroAndroid(androidContext()) }
    single { AgroDatabase(AndroidSqliteDriver(AgroDatabase.Schema,androidContext(),"agrocampo.db")) }
    single { crearCliente(OkHttp.create(),get()) }
    single<Conexion> { ConexionAndroid(androidContext()) }
}
