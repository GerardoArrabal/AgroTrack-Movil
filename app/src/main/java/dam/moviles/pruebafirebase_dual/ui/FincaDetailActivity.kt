package dam.moviles.pruebafirebase_dual.ui

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import dam.moviles.pruebafirebase_dual.R
import dam.moviles.pruebafirebase_dual.data.ApiResult
import dam.moviles.pruebafirebase_dual.data.ApiService
import dam.moviles.pruebafirebase_dual.data.Cultivo
import dam.moviles.pruebafirebase_dual.data.FincaDetalle
import dam.moviles.pruebafirebase_dual.databinding.ActivityFincaDetailBinding
import dam.moviles.pruebafirebase_dual.databinding.ItemCultivoBinding
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class FincaDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_FINCA_ID = "extra_finca_id"
        const val EXTRA_USUARIO_ID = "extra_usuario_id"
        const val EXTRA_FINCA_NOMBRE = "extra_finca_nombre"
    }

    private lateinit var binding: ActivityFincaDetailBinding
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()

    private var fincaId: Int = 0
    private var usuarioId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFincaDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fincaId = intent.getIntExtra(EXTRA_FINCA_ID, 0)
        usuarioId = intent.getIntExtra(EXTRA_USUARIO_ID, 0)
        val fincaNombre = intent.getStringExtra(EXTRA_FINCA_NOMBRE).orEmpty()

        configurarToolbar(fincaNombre)
        if (fincaId <= 0 || usuarioId <= 0) {
            mostrarError(getString(R.string.error_finca_no_valida))
        } else {
            cargarDetalle()
        }
    }

    private fun configurarToolbar(nombre: String) {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = nombre
    }

    private fun cargarDetalle() {
        mostrarCargando(true)
        executor.execute {
            val resultado = ApiService.obtenerDetalleFinca(fincaId, usuarioId)
            runOnUiThread {
                mostrarCargando(false)
                when (resultado) {
                    is ApiResult.Success -> mostrarDetalle(resultado.data)
                    is ApiResult.Error -> mostrarError(resultado.message)
                }
            }
        }
    }

    private fun mostrarDetalle(detalle: FincaDetalle) {
        val finca = detalle.finca
        binding.textUbicacion.text = finca.ubicacion ?: "-"
        binding.textEstado.text = finca.estado
        binding.textTipoSuelo.text = finca.tipoSuelo ?: "-"
        binding.textRiego.text = finca.sistemaRiego ?: "-"
        binding.textSuperficie.text = finca.superficie?.let {
            String.format(Locale.getDefault(), "%.2f ha", it)
        } ?: "-"
        binding.textFechaRegistro.text = detalle.fechaRegistro ?: "-"

        val coordenadasDescripcion = when (val coords = finca.coordenadas) {
            null -> getString(R.string.coordenadas_no_disponibles)
            else -> getString(R.string.coordenadas_puntos, coords.size)
        }
        binding.textCoordenadas.text = coordenadasDescripcion
        binding.textEstadoDetalle.visibility = View.GONE

        binding.contenedorCultivos.removeAllViews()
        if (detalle.cultivos.isEmpty()) {
            agregarMensajeSimple(getString(R.string.sin_cultivos_registrados))
        } else {
            detalle.cultivos.forEach { cultivo ->
                agregarCultivo(cultivo)
            }
        }
    }

    private fun agregarMensajeSimple(texto: String) {
        val mensajeBinding = ItemCultivoBinding.inflate(layoutInflater, binding.contenedorCultivos, false)
        mensajeBinding.textNombre.text = texto
        mensajeBinding.textEstado.visibility = View.GONE
        mensajeBinding.textFechas.visibility = View.GONE
        mensajeBinding.textProduccion.visibility = View.GONE
        binding.contenedorCultivos.addView(mensajeBinding.root)
    }

    private fun agregarCultivo(cultivo: Cultivo) {
        val itemBinding = ItemCultivoBinding.inflate(layoutInflater, binding.contenedorCultivos, false)
        itemBinding.textNombre.text = cultivo.nombre
        itemBinding.textEstado.text = cultivo.estado

        val fechas = buildString {
            if (!cultivo.fechaSiembra.isNullOrBlank()) {
                append(getString(R.string.fecha_siembra, cultivo.fechaSiembra))
            }
            if (!cultivo.fechaCosecha.isNullOrBlank()) {
                if (isNotEmpty()) append(" • ")
                append(getString(R.string.fecha_cosecha, cultivo.fechaCosecha))
            }
        }.ifEmpty { getString(R.string.fechas_no_disponibles) }
        itemBinding.textFechas.text = fechas

        val produccion = cultivo.produccionKg?.let {
            getString(R.string.produccion_kg, String.format(Locale.getDefault(), "%.2f", it))
        } ?: getString(R.string.produccion_no_disponible)
        itemBinding.textProduccion.text = produccion

        binding.contenedorCultivos.addView(itemBinding.root)
    }

    private fun mostrarCargando(mostrar: Boolean) {
        binding.progressBar.visibility = if (mostrar) View.VISIBLE else View.GONE
    }

    private fun mostrarError(mensaje: String) {
        binding.textEstadoDetalle.visibility = View.VISIBLE
        binding.textEstadoDetalle.text = mensaje
        Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == android.R.id.home) {
            finish()
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        executor.shutdownNow()
    }
}

