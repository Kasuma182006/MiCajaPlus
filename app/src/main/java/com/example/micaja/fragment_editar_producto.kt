package com.example.micaja

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.micaja.ConexionService.ConexionServiceTienda
import com.example.micaja.databinding.ActivityFragmentEditarProductoBinding
import com.example.micaja.models.inventario
import kotlinx.coroutines.flow.MutableStateFlow

class fragment_editar_producto : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // View Binding reemplaza setContentView(R.layout.activity_fragment_editar_producto)
        // Si aún no usas binding, deja el setContentView original y borra estas líneas
        val binding = ActivityFragmentEditarProductoBinding.inflate(layoutInflater)
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

        // Botón Buscar Producto — reemplaza el listener anterior
        binding.btnBuscarProducto.setOnClickListener {
            val nombreProducto = binding.etBuscarProducto.text.toString().trim()

            if (nombreProducto.isNotEmpty()) {
                val dialog = BuscarProductoDialog(
                    query = nombreProducto,
                    onProductoSeleccionado = { producto ->
                        // Al seleccionar un producto, rellena el formulario
                        binding.etNombreProducto.setText(producto.nombre)
                        binding.etDescripcionProducto.setText(producto.presentacion)
                        binding.etPrecioProducto.setText(producto.valorCompra.toString())
                        binding.etCantidadProducto.setText(producto.cantidad.toString())

                        // Habilita el formulario (quita el alpha de 0.5)
                        binding.cvFormulario.alpha = 1f
                    }
                )
                dialog.show(supportFragmentManager, "BuscarProductoDialog")
            } else {
                Toast.makeText(this, "Escribe un nombre para buscar", Toast.LENGTH_SHORT).show()
            }
        }

    }




}