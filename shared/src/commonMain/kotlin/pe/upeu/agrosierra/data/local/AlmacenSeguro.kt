package pe.upeu.agrosierra.data.local
interface AlmacenSeguro { fun guardarToken(token: String); fun leerToken(): String?; fun borrarToken() }
