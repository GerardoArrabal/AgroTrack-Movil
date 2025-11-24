package dam.moviles.pruebafirebase_dual.data

data class Coordenada(
    val latitud: Double,
    val longitud: Double
)

data class Finca(
    val id: Int,
    val nombre: String,
    val ubicacion: String?,
    val superficie: Double?,
    val tipoSuelo: String?,
    val sistemaRiego: String?,
    val estado: String,
    val coordenadas: List<Coordenada>?
)

data class FincaDetalle(
    val finca: Finca,
    val fechaRegistro: String?,
    val cultivos: List<Cultivo>
)

