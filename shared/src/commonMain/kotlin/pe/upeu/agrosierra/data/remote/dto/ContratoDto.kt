package pe.upeu.agrosierra.data.remote.dto

import kotlinx.serialization.Serializable
import pe.upeu.agrosierra.domain.model.*

@Serializable data class PaginaDto<T>(val contenido: List<T>,val pagina: Int=0,val tamanio: Int=20,val totalElementos: Long=0,val totalPaginas: Int=0,val ultima: Boolean=true)
@Serializable data class LoteDto(val id: Long,val codigo: String,val nombre: String,val hectareas: Double,val cultivo: String,val valle: String,val campaniaCerrada: Boolean,val dosisMaximaPorHectarea: Double)
fun LoteDto.aDominio()=Lote(id,codigo,nombre,hectareas,cultivo,valle,campaniaCerrada,dosisMaximaPorHectarea)
@Serializable data class LaborDto(val id: Long,val loteId: Long,val tipo: String,val fecha: String,val responsable: String,val cantidad: Double,val observacion: String,val autorId: Long=0,val registradaEn: String="",val claveCliente: String?=null)
fun LaborDto.aDominio()=Labor(id,loteId,TipoLabor.valueOf(tipo),fecha,responsable,cantidad,observacion,false,autorId,registradaEn,claveCliente.orEmpty())
@Serializable data class LaborRequest(val loteId: Long,val tipo: String,val fecha: String,val cantidad: Double,val observacion: String,val claveCliente: String?=null)
fun Labor.aRequest()=LaborRequest(loteId,tipo.name,fecha,cantidad,observacion,claveCliente.takeIf { it.isNotBlank() })
@Serializable data class LoginRequest(val usuario: String,val password: String)
@Serializable data class SesionDto(val token: String="",val usuario: String,val nombre: String,val rol: String,val privilegios: Set<String>,val id: Long,val puedeEditarAjenas: Boolean=false)
fun SesionDto.aDominio()=Sesion(token,usuario,rol,privilegios,id,nombre,puedeEditarAjenas)
