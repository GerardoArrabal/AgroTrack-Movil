package dam.moviles.pruebafirebase_dual.ui

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import dam.moviles.pruebafirebase_dual.MainActivity
import dam.moviles.pruebafirebase_dual.R
import dam.moviles.pruebafirebase_dual.data.ApiResult
import dam.moviles.pruebafirebase_dual.data.ApiService
import dam.moviles.pruebafirebase_dual.data.Finca
import dam.moviles.pruebafirebase_dual.databinding.ActivityFincasBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class FincasActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_USER_ID = "extra_user_id"
        const val EXTRA_USER_NAME = "extra_user_name"
    }

    private lateinit var binding: ActivityFincasBinding
    private lateinit var adapter: FincaAdapter
    private val fincas: MutableList<Finca> = mutableListOf()
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()

    private var usuarioId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFincasBinding.inflate(layoutInflater)
        setContentView(binding.root)

        usuarioId = intent.getIntExtra(EXTRA_USER_ID, 0)
        val nombreUsuario = intent.getStringExtra(EXTRA_USER_NAME).orEmpty()

        configurarToolbar(nombreUsuario)
        configurarLista()
        binding.btnReintentar.setOnClickListener { cargarFincas() }
        cargarFincas()
    }

    private fun configurarToolbar(nombreUsuario: String) {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = getString(R.string.titulo_fincas)
        binding.textUsuario.text = getString(R.string.fincas_usuario, nombreUsuario)
    }

    private fun configurarLista() {
        adapter = FincaAdapter(this, fincas)
        binding.listFincas.adapter = adapter
        binding.listFincas.setOnItemClickListener { _, _, position, _ ->
            val finca = fincas[position]
            val intent = Intent(this, FincaDetailActivity::class.java).apply {
                putExtra(FincaDetailActivity.EXTRA_FINCA_ID, finca.id)
                putExtra(FincaDetailActivity.EXTRA_USUARIO_ID, usuarioId)
                putExtra(FincaDetailActivity.EXTRA_FINCA_NOMBRE, finca.nombre)
            }
            startActivity(intent)
        }
    }

    private fun cargarFincas() {
        if (usuarioId <= 0) {
            mostrarEstadoError(getString(R.string.error_usuario_no_valido))
            return
        }
        mostrarCargando(true)
        executor.execute {
            val resultado = ApiService.obtenerFincas(usuarioId)
            runOnUiThread {
                mostrarCargando(false)
                when (resultado) {
                    is ApiResult.Success -> mostrarFincas(resultado.data)
                    is ApiResult.Error -> mostrarEstadoError(resultado.message)
                }
            }
        }
    }

    private fun mostrarFincas(lista: List<Finca>) {
        adapter.updateData(lista)
        binding.textEstado.visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
        binding.textEstado.text = if (lista.isEmpty()) {
            getString(R.string.mensaje_sin_fincas)
        } else {
            ""
        }
        binding.btnReintentar.visibility = View.GONE
    }

    private fun mostrarEstadoError(mensaje: String) {
        binding.textEstado.visibility = View.VISIBLE
        binding.textEstado.text = mensaje
        binding.btnReintentar.visibility = View.VISIBLE
        Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show()
    }

    private fun mostrarCargando(mostrar: Boolean) {
        binding.progressBar.visibility = if (mostrar) View.VISIBLE else View.GONE
        if (mostrar) {
            binding.btnReintentar.visibility = View.GONE
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_fincas, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                cerrarSesion()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun cerrarSesion() {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        executor.shutdownNow()
    }
}

