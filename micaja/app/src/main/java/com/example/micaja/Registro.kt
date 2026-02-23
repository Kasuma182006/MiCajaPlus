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
import com.example.micaja.databinding.RegistroBinding
import com.example.micaja.models.Tendero
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class Registro : AppCompatActivity() {

    private lateinit var binding: RegistroBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Forzar que la ventana deje que el sistema gestione insets (necesario para adjustResize)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        // Forzar ajuste del layout cuando aparece el teclado
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        binding = RegistroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        configurarBotones()
    }

    private fun configurarBotones() {
        binding.btnRetroceso.setOnClickListener { cancelarRegistro() }
        binding.btnRegistrar.setOnClickListener { registrarTendero() }
    }

    private fun registrarTendero() {
        val nombre= binding.inputNombre.text.toString().trim().lowercase()
        val cedula = binding.inputCedula.text.toString().trim()
        val telefono = binding.inputTelefono.text.toString().trim()


        if (cedula.isEmpty() || telefono.isEmpty() || nombre.isEmpty()) {
            Toast.makeText(this, "Por favor diligencie los dos campos.", Toast.LENGTH_SHORT).show()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val apiServicer = ConexionServiceTienda.create()
                val registrarNuevoTendero = Tendero(cedula = cedula, telefono = telefono , nombre = nombre)
                val responset = apiServicer.login(registrarNuevoTendero)


                if (responset.isSuccessful && responset.body() != null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@Registro,
                            "La tienda ya está registrada.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    return@launch
                }

                val apiService = ConexionServiceTienda.create()
                val nuevoTendero = Tendero(cedula = cedula, telefono = telefono, nombre=nombre)


                val response = apiService.addTendero(nuevoTendero)

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@Registro, "Registro exitoso.", Toast.LENGTH_SHORT).show()
                        irALogin()
                    } else {
                        Toast.makeText(
                            this@Registro,
                            "Error durante el registro: ${response.code()}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@Registro,
                        "Error de conexión: ${e.localizedMessage}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun irALogin() {
        startActivity(Intent(this, Login::class.java))
        finish()
    }

    private fun cancelarRegistro() {
        Toast.makeText(this, "Registro cancelado", Toast.LENGTH_SHORT).show()
        startActivity(Intent(this, Login::class.java))
        finish()
    }
}