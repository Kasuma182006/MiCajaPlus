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
import kotlinx.coroutines.flow.MutableStateFlow

class editar_clientes : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val binding = ActivityEditarClientesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Botón retroceso
        binding.btnRetroceso.setOnClickListener {
            finish()
        }

        // Botón Buscar Cliente
        binding.btnBuscarCliente.setOnClickListener {
            val cedulaCliente: String = binding.etBuscarCliente.text.toString().trim()

            if (cedulaCliente != "") {
//                buscarCliente(binding, cedulaCliente)
            } else {
                Toast.makeText(this, "Por favor ingresa la cédula para buscar", Toast.LENGTH_SHORT).show()
            }
        }

    }


//    fun buscarCliente(binding: ActivityEditarClientesBinding, cedulaCliente: String) {
//
//        val clientes: MutableStateFlow<List<cliente>> = ConexionServiceTienda.obtenerClientes()
//
////        var encontrado = false
//
//        for (c in clientes.value) {
//            if (c.cedula == cedulaCliente) {
//                binding.etNombreCliente.setText(c.nombre)
//                binding.etCedulaCliente.setText(c.cedula)
//                binding.etTelefonoCliente.setText(c.celular)
//                binding.etValorCredito.setText(c.total?.toString())
//                binding.btnGuardarCliente.isEnabled = true
////                encontrado = true
//                break
//            }
//        }

//        if (!encontrado) {
//            Toast.makeText(this, "Cliente no encontrado", Toast.LENGTH_SHORT).show()
//        }

    }

