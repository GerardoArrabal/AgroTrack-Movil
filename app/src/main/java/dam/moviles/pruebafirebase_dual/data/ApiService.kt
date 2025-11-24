package dam.moviles.pruebafirebase_dual.data

import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

object ApiService {

    fun login(usuario: String, password: String): ApiResult<Usuario> {
        return try {
            val body = JSONObject().apply {
                put("usuario", usuario)
                put("password", password)
            }
            val response = doPost("/login.php", body)
            if (response.optString("status") == "ok") {
                val data = response.optJSONObject("data")?.optJSONObject("usuario")
                if (data != null) {
                    ApiResult.Success(parseUsuario(data))
                } else {
                    ApiResult.Error("Respuesta incompleta del servidor")
                }
            } else {
                val mensaje = response.optString("message", "Credenciales inválidas")
                ApiResult.Error(mensaje)
            }
        } catch (ex: Exception) {
            ApiResult.Error(ex.message ?: "Error de conexión")
        }
    }

    fun obtenerFincas(usuarioId: Int): ApiResult<List<Finca>> {
        return try {
            val response = doGet(
                "/fincas.php",
                mapOf("usuario_id" to usuarioId.toString())
            )
            if (response.optString("status") == "ok") {
                val lista = response.optJSONObject("data")
                    ?.optJSONArray("fincas")
                val fincas = parseFincas(lista)
                ApiResult.Success(fincas)
            } else {
                ApiResult.Error(response.optString("message", "No se pudieron cargar las fincas"))
            }
        } catch (ex: Exception) {
            ApiResult.Error(ex.message ?: "Error de conexión")
        }
    }

    fun obtenerDetalleFinca(fincaId: Int, usuarioId: Int): ApiResult<FincaDetalle> {
        return try {
            val response = doGet(
                "/finca_detalle.php",
                mapOf(
                    "finca_id" to fincaId.toString(),
                    "usuario_id" to usuarioId.toString()
                )
            )
            if (response.optString("status") == "ok") {
                val data = response.optJSONObject("data")
                if (data != null) {
                    val fincaJson = data.optJSONObject("finca")
                    val cultivosJson = data.optJSONArray("cultivos")
                    if (fincaJson != null) {
                        val finca = parseFinca(fincaJson)
                        val cultivos = parseCultivos(cultivosJson)
                        val detalle = FincaDetalle(
                            finca = finca,
                            fechaRegistro = fincaJson.optString("fecha_registro", null),
                            cultivos = cultivos
                        )
                        ApiResult.Success(detalle)
                    } else {
                        ApiResult.Error("Datos de finca no disponibles")
                    }
                } else {
                    ApiResult.Error("Respuesta incompleta del servidor")
                }
            } else {
                ApiResult.Error(response.optString("message", "No se pudo cargar la finca"))
            }
        } catch (ex: Exception) {
            ApiResult.Error(ex.message ?: "Error de conexión")
        }
    }

    private fun parseUsuario(json: JSONObject): Usuario {
        val rolTexto = json.optString("rol", "USUARIO").uppercase(Locale.US)
        val rol = runCatching { Usuario.Rol.valueOf(rolTexto) }
            .getOrDefault(Usuario.Rol.USUARIO)

        return Usuario(
            id = json.optInt("id"),
            nombre = json.optString("nombre"),
            apellidos = json.optString("apellidos"),
            email = json.optString("email"),
            username = json.optString("username"),
            rol = rol
        )
    }

    private fun parseFincas(array: JSONArray?): List<Finca> {
        if (array == null) return emptyList()
        val fincas = mutableListOf<Finca>()
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            fincas.add(parseFinca(item))
        }
        return fincas
    }

    private fun parseCultivos(array: JSONArray?): List<Cultivo> {
        if (array == null) return emptyList()
        val cultivos = mutableListOf<Cultivo>()
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            cultivos.add(
                Cultivo(
                    id = item.optInt("id"),
                    nombre = item.optString("nombre"),
                    variedad = item.optString("variedad", null),
                    fechaSiembra = item.optString("fecha_siembra", null),
                    fechaCosecha = item.optString("fecha_cosecha", null),
                    estado = item.optString("estado"),
                    produccionKg = item.optDoubleOrNull("produccion_kg"),
                    rendimientoEstimado = item.optDoubleOrNull("rendimiento_estimado"),
                    rendimientoReal = item.optDoubleOrNull("rendimiento_real")
                )
            )
        }
        return cultivos
    }

    private fun parseFinca(json: JSONObject): Finca {
        return Finca(
            id = json.optInt("id"),
            nombre = json.optString("nombre"),
            ubicacion = json.optString("ubicacion", null),
            superficie = json.optDoubleOrNull("superficie"),
            tipoSuelo = json.optString("tipo_suelo", null),
            sistemaRiego = json.optString("sistema_riego", null),
            estado = json.optString("estado"),
            coordenadas = parseCoordenadas(json.opt("coordenadas"))
        )
    }

    private fun JSONObject.optDoubleOrNull(key: String): Double? {
        if (!has(key) || isNull(key)) return null
        val value = optDouble(key, Double.NaN)
        return if (value.isNaN()) null else value
    }

    private fun JSONArray.optDoubleOrNull(index: Int): Double? {
        if (index !in 0 until length() || isNull(index)) return null
        val value = optDouble(index, Double.NaN)
        return if (value.isNaN()) null else value
    }

    private fun parseCoordenadas(valor: Any?): List<Coordenada>? {
        return when (valor) {
            is JSONArray -> {
                val coords = mutableListOf<Coordenada>()
                for (i in 0 until valor.length()) {
                    val punto = valor.optJSONArray(i) ?: continue
                    val lat = punto.optDoubleOrNull(0) ?: continue
                    val lon = punto.optDoubleOrNull(1) ?: continue
                    coords.add(Coordenada(lat, lon))
                }
                if (coords.isEmpty()) null else coords
            }
            is String -> {
                runCatching {
                    val json = JSONArray(valor)
                    parseCoordenadas(json)
                }.getOrNull()
            }
            else -> null
        }
    }

    private fun doPost(path: String, body: JSONObject): JSONObject {
        val url = URL(ApiConfig.BASE_URL + path)
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = ApiConfig.CONNECT_TIMEOUT
            readTimeout = ApiConfig.READ_TIMEOUT
            doInput = true
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
        }
        connection.outputStream.use { output ->
            BufferedOutputStream(output).use { buffered ->
                buffered.write(body.toString().toByteArray(Charsets.UTF_8))
                buffered.flush()
            }
        }
        return readResponse(connection)
    }

    private fun doGet(path: String, query: Map<String, String>): JSONObject {
        val queryString = if (query.isNotEmpty()) {
            query.entries.joinToString("&") { (key, value) ->
                "${URLEncoder.encode(key, "UTF-8")}=${URLEncoder.encode(value, "UTF-8")}"
            }
        } else {
            ""
        }
        val urlString = buildString {
            append(ApiConfig.BASE_URL)
            append(path)
            if (queryString.isNotEmpty()) {
                append('?')
                append(queryString)
            }
        }
        val url = URL(urlString)
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = ApiConfig.CONNECT_TIMEOUT
            readTimeout = ApiConfig.READ_TIMEOUT
            doInput = true
        }
        return readResponse(connection)
    }

    private fun readResponse(connection: HttpURLConnection): JSONObject {
        val statusCode = connection.responseCode
        val stream = if (statusCode in 200..299) {
            connection.inputStream
        } else {
            connection.errorStream ?: connection.inputStream
        }

        val responseText = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        connection.disconnect()

        val json = runCatching { JSONObject(responseText) }
            .getOrElse {
                throw IllegalStateException("Respuesta inválida del servidor")
            }

        if (statusCode !in 200..299) {
            val message = json.optString("message", "Error del servidor")
            throw IllegalStateException(message)
        }

        return json
    }
}

