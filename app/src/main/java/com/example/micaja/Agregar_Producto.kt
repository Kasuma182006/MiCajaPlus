package com.example.micaja

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.widget.addTextChangedListener
import com.example.micaja.ConexionService.ConexionServiceTienda
import com.example.micaja.databinding.ActivityAgregarProductoBinding
import com.example.micaja.models.AgregarProducto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class Agregar_Producto : AppCompatActivity() {

    val unidades = mapOf(
        // Peso
        "gramo" to listOf("gramo", "gramos", "g", "gr"),
        "libra" to listOf("libra", "libras", "lb"),
        "kilogramo" to listOf("kilo", "kilogramos", "kg", "kilos"),
        "onza" to listOf("onza", "onzas"),

        // Líquidos
        "Litro" to listOf("litro", "litron", "l", "litros"),
        "militro" to listOf("ml", "mililitros"),
        "garrafa" to listOf("garrafa", "gal"),

        // Empaques y Agrupaciones
        "bolsa" to listOf("bolsa", "bolsita", "chuspa"),
        "caja" to listOf("caja", "cajetilla"),
        "paquete" to listOf("paquete", "paca", "sixpack", "sobre"),
        "envase" to listOf("envase", "frasco", "botella", "tubo", "spray", "rollo"),
        "unidad" to listOf("unidad", "unidades", "unidad", "barra", "capsula", "tableta", "docena", "panal", "atado", "hojas", "carta"),
        "recipiente" to listOf("cubeta", "canasta", "cubo", "vaso", "vasito", "vasito", "lata", "laton"),

        // Tamaños y Medidas
        "pequeño" to listOf("pequeña", "pequeño", "mini"),
        "mediano" to listOf("mediana", "mediano", "media"),
        "grande" to listOf("grande", "jumbo")
    )

    private val categorias = mapOf(
        1 to "abarrotes", 2 to "bebidas", 3 to "dulcería", 4 to "licores",
        5 to "fruver", 6 to "lácteos", 7 to "aseo personal", 8 to "aseo general",
        9 to "cárnicos", 10 to "papelería", 11 to "fármacos", 12 to "mascotas"
    )
    private var idCategoriaSeleccionada: Int = -1

    private lateinit var binding: ActivityAgregarProductoBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        binding = ActivityAgregarProductoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupKeyboardBehavior(
            rootView = binding.main,
            viewToScroll = binding.main,
            viewToHide = null
        )

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        binding.btnRetroceso.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

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
            val nombre = binding.etNombreProducto.text.toString().trim().lowercase()
            val categoriaTxt = binding.etCatagoriaProducto.text.toString().trim().lowercase()
            val presentacion = binding.etDescripcionProducto.text.toString().trim().lowercase()
            val pCompra = binding.etPrecioProductoCompra.text.toString().trim().lowercase()
            val pVenta = binding.etPrecioProducto.text.toString().trim().lowercase()
            val cantidad = binding.etCantidadProducto.text.toString().trim().lowercase()

            if (nombre.isEmpty() || categoriaTxt.isEmpty() || presentacion.isEmpty() ||
                pCompra.isEmpty() || pVenta.isEmpty() || cantidad.isEmpty()) {

                Toast.makeText(this, "Por favor diligencie todos los campos.", Toast.LENGTH_SHORT).show()
            }
            if (idCategoriaSeleccionada == -1) {
                Toast.makeText(
                    this,
                    "Por favor seleccione una categoría de la lista.",
                    Toast.LENGTH_SHORT
                ).show()
            }
            if(presentacionLista(presentacion)) {
                ejecutarGuardado(nombre, presentacion, cantidad, pVenta, pCompra)
            }else{
                Toast.makeText(this, "La presentacion no es reconocida", Toast.LENGTH_SHORT).show()

            }
        }
    }

    private fun ejecutarGuardado(nombre: String, presentacion: String, cantidad: String, pVenta: String, pCompra: String) {
        val preferencia = getSharedPreferences("SesionTendero", MODE_PRIVATE)
        val cedula = preferencia.getString("cedula", null)

        binding.btnGuardarProducto.isEnabled = false

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val conexion = ConexionServiceTienda.create()
                val modelo = AgregarProducto(cedula!!, nombre, presentacion, cantidad.toInt(), pVenta.toInt(), idCategoriaSeleccionada, pCompra.toInt())
                val respuesta = conexion.addProducto(modelo)

                launch(Dispatchers.Main) {
                    binding.btnGuardarProducto.isEnabled = true

                    if (respuesta.isSuccessful) {
                        val data = Intent()
                        data.putExtra("mensaje_confirmacion", "¡$nombre guardado correctamente!")
                        setResult(RESULT_OK, data)

                        Toast.makeText(this@Agregar_Producto, "Producto agregado con éxito", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        val errorCuerpo = respuesta.errorBody()?.string() ?: "Error del sistema"
                        Log.e("API_ERROR", "Código: ${respuesta.code()} - $errorCuerpo")

                        Toast.makeText(this@Agregar_Producto, "Servidor: No se pudo guardar. Intenta de nuevo.", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                launch(Dispatchers.Main) {
                    binding.btnGuardarProducto.isEnabled = true

                    val mensajeError = when (e) {
                        is java.net.SocketTimeoutException -> "La conexión tardó demasiado. Revisa tu internet."
                        else -> "Error de red: ${e.localizedMessage}"
                    }

                    Log.e("NETWORK_ERROR", mensajeError)
                    Toast.makeText(this@Agregar_Producto, mensajeError, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun presentacionLista( texto: String): Boolean{
        var textoLimpio = texto

        val soloTexto = Regex("[a-zA-ZñÑ]+").findAll(textoLimpio).lastOrNull()?.value ?: ""

        return unidades.values.any{ listaSinonimos ->
            listaSinonimos.contains(soloTexto)
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
        autoComplete.setOnItemClickListener { parent, _, position, _ ->
            // Obtenemos el nombre que el usuario tocó
            val nombreSeleccionado = parent.getItemAtPosition(position).toString()

            // Buscamos la Key (ID) que corresponde a ese nombre
            idCategoriaSeleccionada = categorias.entries.find { it.value == nombreSeleccionado }?.key ?: -1
            Log.d("CategoriaSeleccionada: $nombreSeleccionado", "ID de la categoría seleccionada: $idCategoriaSeleccionada")
        }
        autoComplete.setOnClickListener { autoComplete.showDropDown() }
    }

    private fun validaciones() {
        binding.etNombreProducto.addTextChangedListener { s ->
            val textoOriginal = s.toString()
            val textoFiltrado = textoOriginal.replace(Regex("[^a-zA-ZñÑ ]"), "")

            if (textoOriginal != textoFiltrado) {
                binding.etNombreProducto.setText(textoFiltrado)
                binding.etNombreProducto.setSelection(textoFiltrado.length)
                return@addTextChangedListener // Salimos para evitar doble validación
            }
        }
        binding.etDescripcionProducto.addTextChangedListener { s ->
            val texto = s.toString().trim().lowercase()

            when {
                texto.isEmpty() -> {
                    binding.etDescripcionProducto.error = null
                }

                presentacionLista(texto) -> {
                    binding.etDescripcionProducto.error = null
                }

                else -> {
                    binding.etDescripcionProducto.error = "Unidad no reconocida (ej: 500g, 1 bolsa)"
                }
            }
        }
    }

}