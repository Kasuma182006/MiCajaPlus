package com.example.micaja

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.addTextChangedListener
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
            startActivity(Intent(this, chat_Tienda::class.java))
            finish()
            return
        }

        // NO llames enableEdgeToEdge() si quieres que el teclado empuje el layout
        WindowCompat.setDecorFitsSystemWindows(window, true)
        @Suppress("DEPRECATION")
        window.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN
        )
        // Forzar ajuste del layout cuando aparece el teclado
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        binding = LoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
            val imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            if (imeVisible) {
                // Teclado abierto: ocultar logo para dar espacio
                binding.logoApp.visibility = View.GONE
            } else {
                // Teclado cerrado: mostrar logo
                binding.logoApp.visibility = View.VISIBLE
            }

            v.setPadding(systemBars.left, systemBars.top, systemBars.right, imeHeight)
            insets
        }
        binding.cedulaInput.addTextChangedListener {
            val texto = it.toString()
            val textoSoloNumeros = texto.replace(Regex("[^0-9]"), "")
            if (texto.isNotEmpty() && texto != textoSoloNumeros){
                binding.cedulaInput.error = "La cédula solo debe contener números"
            } else {
                binding.cedulaInput.error = null
                if (texto.length>10){
                    binding.cedulaInput.error = "La cédula debe tener una longitud máxima de 10 dígitos."
                }else{
                    binding.cedulaInput.error = null
                }
            }
        }
        binding.telefonoInput.addTextChangedListener{
            val texto = it.toString()
            val textoSoloNumeros = texto.replace(Regex("[^0-9]"), "")
            if (texto.isNotEmpty() && texto != textoSoloNumeros){
                binding.telefonoInput.error = "El teléfono solo debe contener números"
            }else{
                binding.telefonoInput.error = null
                if(texto.length>10){
                    binding.telefonoInput.error = "Longitud permitida: 10 dígitos"
                }else{
                    binding.telefonoInput.error = null
                }
            }
        }
        configurarBotones()
    }

    private fun configurarBotones() {
        binding.tvRegistrate.setOnClickListener { irARegistro() }
        binding.BtnRegistrar.setOnClickListener { iniciarSesion() }
        binding.plus.setOnClickListener { irAChatTienda() }
    }

    private fun iniciarSesion() {
        val cedula = binding.cedulaInput.text.toString().trim()
        val telefono = binding.telefonoInput.text.toString().trim()

        if (cedula.isEmpty() || telefono.isEmpty()) {
            Toast.makeText(this, "Por favor diligencie todos los campos.", Toast.LENGTH_SHORT).show()
            return
        }

        if(binding.cedulaInput.error != null || binding.telefonoInput.error != null){
            Toast.makeText(this,"Por favor verifique los campos.", Toast.LENGTH_SHORT).show()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val apiService = ConexionServiceTienda.create()
                val registrarNuevoTendero = Tendero(cedula = cedula, telefono = telefono, nombre = "", fechaCreacion = "")
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
                        editor.putString("telefono", tendero.telefono)
                        editor.apply()

                        Toast.makeText(
                            this@Login,
                            "Inicio de sesión exitoso.",
                            Toast.LENGTH_SHORT
                        ).show()
                        irAChatTienda()
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

    private fun irAChatTienda() {
        val intent = Intent(this, chat_Tienda::class.java)
        startActivity(intent)
        finish()
    }

    private fun irARegistro() {
        val intent = Intent(this, Registro::class.java)
        startActivity(intent)
    }
}