package com.example.micaja

import android.content.Context
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.example.micaja.ConexionService.ConexionServiceTienda
import com.example.micaja.databinding.ActivityEditarClientesBinding
import com.example.micaja.models.ActualizarCliente
import com.example.micaja.models.Identificacion
import kotlinx.coroutines.launch

class editar_clientes : AppCompatActivity() {
    private lateinit var binding: ActivityEditarClientesBinding
    private val service = ConexionServiceTienda.create()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Forzar que la ventana deje que el sistema gestione insets (necesario para adjustResize)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        // Forzar ajuste del layout cuando aparece el teclado
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        binding = ActivityEditarClientesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.textBuscarCliente.setEndIconOnClickListener {
            // Limpia el campo de busqueda principal
            binding.etBuscarCliente.setText("")

            // Limpia los campos especificos del cliente
            binding.etNombreCliente.setText("")
            binding.etCedulaCliente.setText("")
            binding.etTelefonoCliente.setText("")
            binding.etValorCredito.setText("")

            // Restaura el estado visual y deshabilitar el boton de guardado
            binding.cvFormulario.alpha = 0.5f
            binding.btnGuardarCliente.isEnabled = false

            //Quita el foco y desactiva teclado
            binding.etBuscarCliente.clearFocus()
            WindowInsetsControllerCompat(window, binding.etBuscarCliente)
                .hide(WindowInsetsCompat.Type.ime())
        }

        // Recupera id del tendero desde la sesion iniciada en Login
        val prefs = getSharedPreferences("SesionTendero", Context.MODE_PRIVATE)
        val idTendero = prefs.getString("cedula", "") ?: ""

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.btnRetroceso.setOnClickListener { finish() }

        binding.btnBuscarCliente.setOnClickListener {

            val cedulaBusqueda = binding.etBuscarCliente.text.toString().trim()
            if (cedulaBusqueda.isNotEmpty() && idTendero.isNotEmpty()) {
                buscarCliente(cedulaBusqueda, idTendero)
            } else {
                Toast.makeText(this, "Ingresa una cédula válida", Toast.LENGTH_SHORT).show()
            }
            // Ocultar el teclado al presionar enviar
        WindowInsetsControllerCompat(window, binding.btnBuscarCliente)
            .hide(WindowInsetsCompat.Type.ime())
        }

        binding.btnGuardarCliente.setOnClickListener {
            guardarCambios(idTendero)
        }
    }

    private fun buscarCliente(cedula: String, idTendero: String) {
        lifecycleScope.launch {
            try {
                val response = service.consultarClienteEdit(Identificacion(cedula, idTendero))
                if (response.isSuccessful && response.body() != null) {
                    val c = response.body()!!
                    // Llena el formulario
                    binding.etNombreCliente.setText(c.nombre)
                    binding.etCedulaCliente.setText(c.cedula)
                    binding.etTelefonoCliente.setText(c.telefono)
                    val puntuacionSaldo = c.saldo.toString().replace(Regex("""(\d)(?=(\d{3})+(?!\d))"""), "$1.")
                    binding.etValorCredito.setText(puntuacionSaldo)
                    binding.etNombreCliente.isEnabled = true
                    binding.etTelefonoCliente.isEnabled = true
                    binding.etValorCredito.isEnabled = true


                    // Habilita edicion visualmente
                    binding.cvFormulario.alpha = 1.0f
                    binding.btnGuardarCliente.isEnabled = true
                } else {
                    Toast.makeText(this@editar_clientes, "Cliente no encontrado", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@editar_clientes, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun guardarCambios(idTendero: String) {

        fun String.toCleanInt(): Int {
            return this.replace(".", "").replace(",", "").toIntOrNull() ?: 0
        }
        val datos = ActualizarCliente(
            idTendero = idTendero,
            cedula = binding.etCedulaCliente.text.toString(),
            nombre = binding.etNombreCliente.text.toString(),
            telefono = binding.etTelefonoCliente.text.toString(),
            saldo = binding.etValorCredito.text.toString().toCleanInt()
        )

        lifecycleScope.launch {
            try {
                val response = service.actualizarCliente(datos)
                if (response.isSuccessful) {
                    Toast.makeText(this@editar_clientes, "Cliente actualizado con éxito", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (e: Exception) {
                Toast.makeText(this@editar_clientes, "Error al guardar", Toast.LENGTH_SHORT).show()
            }
        }
    }
}