package dam.moviles.pruebafirebase_dual.data

data class Cultivo(
    val id: Int,
    val nombre: String,
    val variedad: String?,
    val fechaSiembra: String?,
    val fechaCosecha: String?,
    val estado: String,
    val produccionKg: Double?,
    val rendimientoEstimado: Double?,
    val rendimientoReal: Double?
)

