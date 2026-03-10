package com.example.micaja

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.micaja.ConexionService.ConexionServiceTienda
import com.example.micaja.databinding.ActivityEditarClientesBinding
import com.example.micaja.models.cliente

class editar_clientes : AppCompatActivity() {

    // 1. Declarar binding a nivel de clase como en Login/Registro
    private lateinit var binding: ActivityEditarClientesBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 2. Inicializar la variable de clase (sin el 'val')
        binding = ActivityEditarClientesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.btnRetroceso.setOnClickListener {
            finish()
        }

        binding.btnBuscarCliente.setOnClickListener {
            val cedulaCliente = binding.etBuscarCliente.text.toString().trim()
            if (cedulaCliente.isNotEmpty()) {
                // 3. Ya no necesitas pasar 'binding' como parámetro
                buscarCliente(cedulaCliente)
            } else {
                Toast.makeText(this, "Por favor ingresa la cédula", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 4. Simplificar la función usando el binding de la clase
    private fun buscarCliente(cedulaCliente: String) {
        // Obtenemos el valor actual de la lista de clientes
        val listaClientes = ConexionServiceTienda.obtenerClientes().value

        // Usamos .find para buscar de forma más eficiente
        val clienteEncontrado = listaClientes.find { it.cedula == cedulaCliente }

        if (clienteEncontrado != null) {
            binding.apply {
                etNombreCliente.setText(clienteEncontrado.nombre)
                etCedulaCliente.setText(clienteEncontrado.cedula)
                etTelefonoCliente.setText(clienteEncontrado.celular)
                etValorCredito.setText(clienteEncontrado.total?.toString() ?: "0")

                // Habilita el formulario (quita el alpha de 0.5)
                binding.cvFormulario.alpha = 1f
                btnGuardarCliente.isEnabled = true
            }
        } else {
            Toast.makeText(this, "Cliente no encontrado", Toast.LENGTH_SHORT).show()
            binding.btnGuardarCliente.isEnabled = false
        }
    }
}