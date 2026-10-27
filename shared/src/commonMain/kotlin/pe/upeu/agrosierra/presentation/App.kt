package pe.upeu.agrosierra.presentation

import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import pe.upeu.agrosierra.domain.model.*
import pe.upeu.agrosierra.presentation.listado.CampoViewModel
import kotlin.time.Clock

@Composable
fun App(vm: CampoViewModel) {
    val s by vm.ui.collectAsState()
    var perfil by remember { mutableStateOf(false) }
    var formulario by remember { mutableStateOf<Labor?>(null) }
    var confirmarEliminar by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(s.sesion?.usuario) { formulario=null;perfil=false }
    LaunchedEffect(s.guardado) { formulario=null }
    val lote=s.lotes.firstOrNull { it.id==s.seleccionado }
    MaterialTheme(colorScheme=lightColorScheme(primary=Color(0xFF246B46),secondary=Color(0xFF74602C),background=Color(0xFFF4F7F1))) {
        Surface(modifier=Modifier.fillMaxSize()) {
            LazyColumn(modifier=Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                item { Text("AgroCampo",style=MaterialTheme.typography.headlineLarge,color=MaterialTheme.colorScheme.primary);Text("AgroSierra · Labores de campo",style=MaterialTheme.typography.bodyMedium) }
                if(s.ocupado) item { LinearProgressIndicator(modifier=Modifier.fillMaxWidth()); Text("Procesando…") }
                s.error?.let { mensaje -> item { Text(mensaje,color=MaterialTheme.colorScheme.error);if(s.reintentar && s.sesion!=null) TextButton(onClick=vm::actualizar,enabled=!s.ocupado) { Text("Reintentar") } } }
                s.mensaje?.let { item { Text(it,color=MaterialTheme.colorScheme.primary) } }
                if(s.sesion==null) {
                    item { Login(s.ocupado,vm::login) }
                } else {
                    val sesion=s.sesion!!
                    item { Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) { OutlinedButton(onClick={perfil=!perfil;formulario=null}) { Text(if(perfil) "Volver" else "Perfil") };OutlinedButton(onClick=vm::salir,enabled=!s.ocupado) { Text("Cerrar sesión") } } }
                    if(perfil) {
                        item { Text(sesion.nombre,style=MaterialTheme.typography.titleLarge);Text("Usuario: ${sesion.usuario}\nRol: ${sesion.rol}\nPrivilegios:");sesion.privilegios.sorted().forEach { Text("• $it") } }
                    } else if(formulario!=null) {
                        item { Formulario(formulario!!,s.campos,s.ocupado,vm::guardar) { formulario=null } }
                    } else if(lote!=null) {
                        item {
                            TextButton(onClick={vm.seleccionar(null)}) { Text("← Volver al padrón") }
                            Text(lote.nombre,style=MaterialTheme.typography.headlineSmall)
                            Text("${lote.codigo} · ${lote.valle}\n${lote.cultivo} · ${lote.hectareas} ha\nDosis máxima/ha: ${lote.dosisMaximaPorHectarea}\nCampaña ${if(lote.campaniaCerrada) "cerrada" else "abierta"}")
                            if(!lote.campaniaCerrada && "LABOR_CREAR" in sesion.privilegios) Button(onClick={formulario=Labor(0,lote.id,TipoLabor.RIEGO,Clock.System.now().toString().take(10),sesion.nombre,1.0,"",true)},enabled=!s.ocupado) { Text("Registrar labor") }
                            if(!lote.campaniaCerrada && "CAMPANIA_CERRAR" in sesion.privilegios) OutlinedButton(onClick={vm.cerrar(lote.id)},enabled=!s.ocupado) { Text("Cerrar campaña") }
                        }
                        val labores=s.labores.filter { it.loteId==lote.id }
                        if(labores.isEmpty()) item { Text("Aún no hay labores registradas") }
                        items(labores,key={it.id}) { labor ->
                            Card(modifier=Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                                Text("${labor.tipo} · ${labor.fecha}",style=MaterialTheme.typography.titleMedium)
                                Text("Responsable: ${labor.responsable}\nCantidad: ${labor.cantidad}\n${labor.observacion}")
                                if(labor.pendiente) Text("Pendiente de sincronización",color=MaterialTheme.colorScheme.primary)
                                Row {
                                    if(!lote.campaniaCerrada && vm.puedeEditar(labor)) TextButton(onClick={formulario=labor},enabled=!s.ocupado) { Text("Corregir") }
                                    if("LABOR_ELIMINAR" in sesion.privilegios) TextButton(onClick={confirmarEliminar=labor.id},enabled=!s.ocupado) { Text("Eliminar") }
                                }
                            } }
                        }
                    } else {
                        item {
                            Text("Padrón de lotes",style=MaterialTheme.typography.headlineSmall)
                            val minutos=s.ultima?.let { ((Clock.System.now().toEpochMilliseconds()-it)/60000).coerceAtLeast(0) }
                            Text(if(minutos==null) "Nunca se ha sincronizado" else if(minutos<1) "Actualizado hace menos de un minuto" else "Actualizado hace $minutos minutos")
                            Button(onClick=vm::actualizar,enabled=!s.ocupado) { Text("Sincronizar") }
                            OutlinedTextField(value=s.busqueda,onValueChange=vm::buscar,label={Text("Buscar nombre o código")},modifier=Modifier.fillMaxWidth())
                            var abierto by remember { mutableStateOf(false) }
                            Box { OutlinedButton(onClick={abierto=true}) { Text("Valle: ${s.valle.ifBlank { "Todos" }}") };DropdownMenu(expanded=abierto,onDismissRequest={abierto=false}) { (listOf("")+s.lotes.map { it.valle }.distinct()).forEach { valle -> DropdownMenuItem(text={Text(valle.ifBlank { "Todos" })},onClick={vm.valle(valle);abierto=false}) } } }
                        }
                        if(s.visibles.isEmpty() && !s.ocupado) item { Text(if(s.lotes.isEmpty()) "Aún no hay datos descargados. Conéctate y sincroniza." else "No hay lotes que coincidan con los filtros") }
                        items(s.visibles,key={it.id}) { item -> Card(onClick={vm.seleccionar(item.id)},modifier=Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) { Text(item.nombre,style=MaterialTheme.typography.titleLarge);Text("${item.codigo} · ${item.valle}\n${item.cultivo} · ${item.hectareas} ha");Text(if(item.campaniaCerrada) "Campaña cerrada" else "Campaña abierta",color=MaterialTheme.colorScheme.primary) } } }
                    }
                }
                item { Text("Roberto Samuel Valencia Saavedra",style=MaterialTheme.typography.labelSmall) }
            }
            confirmarEliminar?.let { id -> AlertDialog(onDismissRequest={confirmarEliminar=null},title={Text("¿Eliminar labor?")},text={Text("Esta acción elimina el registro.")},confirmButton={TextButton(onClick={confirmarEliminar=null;vm.eliminar(id)}) { Text("Eliminar") }},dismissButton={TextButton(onClick={confirmarEliminar=null}) { Text("Cancelar") }}) }
        }
    }
}

@Composable private fun Login(ocupado: Boolean,ingresar: (String,String)->Unit) {
    var usuario by remember { mutableStateOf("") };var password by remember { mutableStateOf("") }
    Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text("Iniciar sesión",style=MaterialTheme.typography.headlineSmall)
        OutlinedTextField(usuario,{usuario=it},label={Text("Usuario")},singleLine=true,modifier=Modifier.fillMaxWidth())
        OutlinedTextField(password,{password=it},label={Text("Contraseña")},singleLine=true,visualTransformation=PasswordVisualTransformation(),modifier=Modifier.fillMaxWidth())
        Button(onClick={ingresar(usuario,password)},enabled=!ocupado && usuario.isNotBlank() && password.isNotBlank(),modifier=Modifier.fillMaxWidth()) { Text("Ingresar") }
    }
}
@Composable private fun Formulario(labor: Labor,errores: Map<String,String>,ocupado: Boolean,guardar: (Labor)->Unit,cancelar: ()->Unit) {
    var tipo by remember(labor.id) { mutableStateOf(labor.tipo) };var fecha by remember(labor.id) { mutableStateOf(labor.fecha) };var cantidad by remember(labor.id) { mutableStateOf(labor.cantidad.toString()) };var observacion by remember(labor.id) { mutableStateOf(labor.observacion) }
    Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Text(if(labor.id==0L) "Registrar labor" else "Corregir labor",style=MaterialTheme.typography.headlineSmall)
        TipoLabor.entries.forEach { t -> Row { RadioButton(selected=tipo==t,onClick={tipo=t});Text(t.name,modifier=Modifier.padding(top=12.dp)) } }
        OutlinedTextField(fecha,{fecha=it},label={Text("Fecha AAAA-MM-DD")},isError="fecha" in errores,modifier=Modifier.fillMaxWidth());errores["fecha"]?.let { Text(it,color=MaterialTheme.colorScheme.error) }
        OutlinedTextField(cantidad,{cantidad=it},label={Text("Cantidad aplicada")},isError="cantidad" in errores,modifier=Modifier.fillMaxWidth());errores["cantidad"]?.let { Text(it,color=MaterialTheme.colorScheme.error) }
        OutlinedTextField(observacion,{observacion=it},label={Text("Observación")},isError="observacion" in errores,modifier=Modifier.fillMaxWidth());errores["observacion"]?.let { Text(it,color=MaterialTheme.colorScheme.error) }
        Button(onClick={guardar(labor.copy(tipo=tipo,fecha=fecha,cantidad=cantidad.toDoubleOrNull() ?: Double.NaN,observacion=observacion))},enabled=!ocupado) { Text("Guardar") }
        TextButton(onClick=cancelar,enabled=!ocupado) { Text("Cancelar") }
    }
}
