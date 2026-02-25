package com.example.micaja

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.example.micaja.ConexionService.ConexionServiceTienda
import com.example.micaja.databinding.LoginBinding
import com.example.micaja.models.Tendero
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class Login : AppCompatActivity() {
    private lateinit var binding: LoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Verificar si ya está logueado
        val prefs = getSharedPreferences("SesionTendero", MODE_PRIVATE)
        val logueado = prefs.getBoolean("logueado", false)

        if (logueado) {
            // Ya está logueado, ir directamente a Principal
            startActivity(Intent(this, Principal::class.java))
            finish()
            return
        }

        enableEdgeToEdge()

        // Forzar que la ventana deje que el sistema gestione insets (necesario para adjustResize)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        // Forzar ajuste del layout cuando aparece el teclado
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        binding = LoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        configurarBotones()
    }

    private fun configurarBotones() {
        binding.tvRegistrate.setOnClickListener { irARegistro() }
        binding.BtnRegistrar.setOnClickListener { iniciarSesion() }
    }

    private fun iniciarSesion() {
        val cedula = binding.cedulaInput.text.toString().trim()
        val telefono = binding.telefonoInput.text.toString().trim()

        if (cedula.isEmpty() || telefono.isEmpty()) {
            Toast.makeText(this, "Por favor diligencie todos los campos.", Toast.LENGTH_SHORT)
                .show()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val apiService = ConexionServiceTienda.create()
                val registrarNuevoTendero = Tendero(cedula = cedula, telefono = telefono, nombre = "")
                val response = apiService.login(registrarNuevoTendero)

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {

                        val tendero=response.body()!!

                        //Guardar sesión
                        val prefs = getSharedPreferences("SesionTendero", MODE_PRIVATE)
                        val editor = prefs.edit()
                        editor.putBoolean("logueado", true)
                        editor.putString("cedula", cedula)
                        editor.putString("nombre", tendero.nombre)
                        editor.apply()

                        Toast.makeText(
                            this@Login,
                            "Inicio de sesión exitoso.",
                            Toast.LENGTH_SHORT
                        ).show()
                        irAPrincipalActivity()
                    } else {
                        Toast.makeText(
                            this@Login,
                            "Credenciales incorrectas.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@Login,
                        "Error de conexión: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun irAPrincipalActivity() {
        val intent = Intent(this, Principal::class.java)
        startActivity(intent)
        finish()
    }

    private fun irARegistro() {
        val intent = Intent(this, Registro::class.java)
        startActivity(intent)
    }
}