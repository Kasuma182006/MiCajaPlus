package com.example.micaja

import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.widget.addTextChangedListener
import com.example.micaja.ConexionService.ConexionServiceTienda
import com.example.micaja.databinding.ActivityAgregarProductoBinding
import com.example.micaja.databinding.RegistroBinding
import com.example.micaja.models.AgregarProducto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class Agregar_Producto : AppCompatActivity() {

    private val categorias = mapOf(
        1 to "abarrotes",
        2 to "bebidas",
        3 to "dulcería",
        4 to "licores",
        5 to "fruver",
        6 to "lácteos",
        7 to "aseo personal",
        8 to "aseo general",
        9 to "cárnicos",
        10 to "papelería",
        11 to "fármacos",
        12 to "mascotas"

    )


    private var idCategoriaSeleccionada: Int = -1


    private lateinit var binding: ActivityAgregarProductoBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // NO llames enableEdgeToEdge() si quieres que el teclado empuje el layout
        WindowCompat.setDecorFitsSystemWindows(window, true)
        @Suppress("DEPRECATION")
        window.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN
        )
        // Forzar ajuste del layout cuando aparece el teclado
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        binding = ActivityAgregarProductoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnRetroceso.setOnClickListener { finish() }

        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        validaciones()
        categoriaLista()
        boton()
    }



    private fun boton() {

        binding.etCatagoriaProducto.setOnClickListener {
            WindowInsetsControllerCompat(window, binding.etCatagoriaProducto)
                .hide(WindowInsetsCompat.Type.ime())
        }

        binding.btnGuardarProducto.setOnClickListener {
            Log.d("MI_CAJA_DEBUG", "Botón presionado")
            val nombre = binding.etNombreProducto.text.toString().trim()
            val categoriaTxt = binding.etCatagoriaProducto.text.toString().trim()
            val presentacion = binding.etDescripcionProducto.text.toString().trim()
            val pCompra = binding.etPrecioProductoCompra.text.toString().trim()
            val pVenta = binding.etPrecioProducto.text.toString().trim()
            val cantidad = binding.etCantidadProducto.text.toString().trim()

            if (nombre.isEmpty() || categoriaTxt.isEmpty() || presentacion.isEmpty() ||
                pCompra.isEmpty() || pVenta.isEmpty() || cantidad.isEmpty()) {

                Toast.makeText(this, "Por favor diligencie todos los campos.", Toast.LENGTH_SHORT).show()
            } else if (idCategoriaSeleccionada == -1) {
                // Validación extra para asegurar que seleccionó algo de la lista
                Toast.makeText(
                    this,
                    "Por favor seleccione una categoría de la lista.",
                    Toast.LENGTH_SHORT
                ).show()
            }else {
                ejecutarGuardado(nombre, presentacion, cantidad, pVenta, pCompra)
            }
        }
    }

    private fun ejecutarGuardado(nombre: String, presentacion: String, cantidad: String, pVenta: String, pCompra: String) {

        val preferencia = getSharedPreferences("SesionTendero", MODE_PRIVATE)
        val cedula = preferencia.getString("cedula", null)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val conexion = ConexionServiceTienda.create()
                val modelo = AgregarProducto(cedula!!,nombre,presentacion,cantidad.toInt(),pVenta.toInt(),idCategoriaSeleccionada,pCompra.toInt())
                val respuesta = conexion.addProducto(modelo)

                launch(Dispatchers.Main) {
                    if (respuesta.isSuccessful) {
                        Toast.makeText(this@Agregar_Producto, "Producto agregado con éxito", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this@Agregar_Producto, "Error al guardar en el servidor", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                launch(Dispatchers.Main) {
                    Toast.makeText(this@Agregar_Producto, "Error de red: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun categoriaLista() {
        val nombresCategorias = categorias.values.toList()

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            nombresCategorias
        )

        val autoComplete = binding.etCatagoriaProducto
        autoComplete.setAdapter(adapter)

        // CAPTURAR LA LLAVE (KEY) AL SELECCIONAR
        autoComplete.setOnItemClickListener { parent, _, position, _ ->
            // Obtenemos el nombre que el usuario tocó
            val nombreSeleccionado = parent.getItemAtPosition(position).toString()

            // Buscamos la Key (ID) que corresponde a ese nombre
            idCategoriaSeleccionada = categorias.entries.find { it.value == nombreSeleccionado }?.key ?: -1

            Log.d("CategoriaSeleccionada: $nombreSeleccionado", "ID de la categoría seleccionada: $idCategoriaSeleccionada")

        }

        autoComplete.setOnClickListener {
            autoComplete.showDropDown()
        }
    }


    private fun validaciones() {
        binding.etNombreProducto.addTextChangedListener { s ->
            val textoOriginal = s.toString()

            val textoFiltrado = textoOriginal.replace(Regex("[^a-zA-ZñÑ ]"), "")

            if (textoOriginal != textoFiltrado) {
                binding.etNombreProducto.setText(textoFiltrado)
                binding.etNombreProducto.setSelection(textoFiltrado.length) // Mantiene el cursor al final
                return@addTextChangedListener // Salimos para evitar doble validación
            }
        }
    }
}

