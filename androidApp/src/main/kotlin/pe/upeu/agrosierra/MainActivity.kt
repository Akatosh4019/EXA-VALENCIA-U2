package pe.upeu.agrosierra
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import pe.upeu.agrosierra.presentation.App
import pe.upeu.agrosierra.presentation.listado.CampoViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
class MainActivity: ComponentActivity() {
    private val vm: CampoViewModel by viewModel()
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState);setContent { App(vm) } }
}
