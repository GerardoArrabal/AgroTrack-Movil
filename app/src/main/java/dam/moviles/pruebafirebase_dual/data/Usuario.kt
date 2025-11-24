package dam.moviles.pruebafirebase_dual.data

data class Usuario(
    val id: Int,
    val nombre: String,
    val apellidos: String,
    val email: String,
    val username: String,
    val rol: Rol
) {
    val nombreCompleto: String
        get() = listOf(nombre, apellidos)
            .filter { it.isNotBlank() }
            .joinToString(" ")

    enum class Rol {
        ADMIN,
        USUARIO
    }
}

