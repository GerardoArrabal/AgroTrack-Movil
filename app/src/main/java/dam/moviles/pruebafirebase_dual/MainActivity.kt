package dam.moviles.pruebafirebase_dual

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import dam.moviles.pruebafirebase_dual.data.ApiResult
import dam.moviles.pruebafirebase_dual.data.ApiService
import dam.moviles.pruebafirebase_dual.data.Usuario
import dam.moviles.pruebafirebase_dual.databinding.ActivityMainBinding
import dam.moviles.pruebafirebase_dual.ui.FincasActivity
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        configurarInteracciones()
    }

    private fun configurarInteracciones() {
        binding.btnIniciarSesion.setOnClickListener {
            intentarLogin()
        }
        binding.inputPassword.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                intentarLogin()
                true
            } else {
                false
            }
        }
    }

    private fun intentarLogin() {
        limpiarErrores()
        val usuario = binding.inputUsuario.text.toString().trim()
        val password = binding.inputPassword.text.toString()

        var valido = true
        if (usuario.isEmpty()) {
            binding.layoutUsuario.error = getString(R.string.error_usuario_obligatorio)
            valido = false
        }
        if (password.isEmpty()) {
            binding.layoutPassword.error = getString(R.string.error_password_obligatorio)
            valido = false
        }
        if (!valido) {
            return
        }

        if (usuario.equals("admin", ignoreCase = true) && password == "admin") {
            AlertDialog.Builder(this)
                .setTitle(R.string.dialog_admin_titulo)
                .setMessage(R.string.mensaje_admin_credenciales)
                .setPositiveButton(android.R.string.ok, null)
                .show()
            return
        }

        mostrarCargando(true)
        executor.execute {
            val resultado = ApiService.login(usuario, password)
            runOnUiThread {
                mostrarCargando(false)
                when (resultado) {
                    is ApiResult.Success -> manejarLoginExitoso(resultado.data)
                    is ApiResult.Error -> mostrarError(resultado.message)
                }
            }
        }
    }

    private fun manejarLoginExitoso(usuario: Usuario) {
        if (usuario.rol == Usuario.Rol.ADMIN) {
            AlertDialog.Builder(this)
                .setTitle(R.string.dialog_admin_titulo)
                .setMessage(R.string.dialog_admin_mensaje)
                .setPositiveButton(android.R.string.ok, null)
                .show()
            return
        }

        val intent = Intent(this, FincasActivity::class.java).apply {
            putExtra(FincasActivity.EXTRA_USER_ID, usuario.id)
            putExtra(FincasActivity.EXTRA_USER_NAME, usuario.nombreCompleto)
        }
        startActivity(intent)
    }

    private fun mostrarCargando(mostrar: Boolean) {
        binding.progressBar.visibility = if (mostrar) View.VISIBLE else View.GONE
        binding.btnIniciarSesion.isEnabled = !mostrar
    }

    private fun mostrarError(mensaje: String) {
        binding.textError.visibility = View.VISIBLE
        binding.textError.text = mensaje
        Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show()
    }

    private fun limpiarErrores() {
        binding.layoutUsuario.error = null
        binding.layoutPassword.error = null
        binding.textError.visibility = View.GONE
    }

    override fun onDestroy() {
        super.onDestroy()
        executor.shutdownNow()
    }
}