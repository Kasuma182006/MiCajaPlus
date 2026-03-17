package com.example.micaja

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
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
import com.example.micaja.databinding.ActivityFragmentEditarProductoBinding
import com.example.micaja.models.BuscarProductos
import com.example.micaja.models.EditarProducto
import com.example.micaja.ui.dialogs.BuscarProductoDialog
import kotlinx.coroutines.*

class fragment_editar_producto : AppCompatActivity() {

    private lateinit var binding: ActivityFragmentEditarProductoBinding
    private var debounceJob: Job? = null
    private var ultimaLista: List<EditarProducto> = emptyList()

    // 1. Definimos el TextWatcher como variable para poder quitarlo y ponerlo
    private val buscadorTextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

        override fun afterTextChanged(s: Editable?) {
            val texto = s?.toString()?.trim() ?: ""

            if (texto.isEmpty()) {
                ocultarSugerencias()
                debounceJob?.cancel()
                return
            }

            // Cancelamos la búsqueda anterior (ahora sí cancelará la petición de red)
            debounceJob?.cancel()

            // Lanzamos la búsqueda de inmediato en un hilo secundario
            debounceJob = CoroutineScope(Dispatchers.IO).launch {
                buscarSugerencias(texto)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // NO llames enableEdgeToEdge() si quieres que el teclado empuje el layout
        WindowCompat.setDecorFitsSystemWindows(window, true)

        binding = ActivityFragmentEditarProductoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Aplica el comportamiento del teclado
        // binding.main     → el NestedScrollView raíz
        // binding.logoApp  → el logo que se oculta para dar espacio
        setupKeyboardBehavior(
            rootView = binding.main,
            viewToScroll = binding.main,
            viewToHide = null
        )

        // Insets normales para status bar
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupListeners()
    }

    private fun setupListeners() {
        binding.btnRetroceso.setOnClickListener { finish() }

        binding.btnBuscarProducto.setOnClickListener {
            buscarProducto()
            WindowInsetsControllerCompat(window, binding.btnBuscarProducto)
                .hide(WindowInsetsCompat.Type.ime())
        }

        binding.textBuscarProducto.setEndIconOnClickListener {
            inicializarEditarProducto()
        }

        // Asignamos el listener del buscador
        binding.etBuscarProducto.addTextChangedListener(buscadorTextWatcher)
    }

    /**
     * Llama al API de forma suspendida.
     * Al ser parte del debounceJob, si este se cancela, la petición se aborta.
     */
    private suspend fun buscarSugerencias(query: String) {
        val preference = getSharedPreferences("SesionTendero", MODE_PRIVATE)
        val cedula = preference.getString("cedula", "") ?: return

        try {
            val conexion = ConexionServiceTienda.create()
            val response = conexion.buscarProductos(BuscarProductos(cedula, query))

            if (response.isSuccessful && response.body() != null) {
                val lista = response.body()!!
                ultimaLista = lista

                withContext(Dispatchers.Main) {
                    mostrarSugerencias(lista.take(3))
                }
            } else {
                withContext(Dispatchers.Main) { ocultarSugerencias() }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Log.e("SearchError", "Error: ${e.message}")
                ocultarSugerencias()
            }
        }
    }

    private fun mostrarSugerencias(lista: List<EditarProducto>) {
        val slots = listOf(binding.suger1, binding.suger2, binding.suger3)

        slots.forEachIndexed { i, chip ->
            if (i < lista.size) {
                val item = lista[i]
                chip.text = buildString {
                    append(item.nombreProducto.replaceFirstChar { it.uppercase() })
                    if (!item.presentacion.isNullOrEmpty()) append("  •  ${item.presentacion}")
                }
                chip.visibility = View.VISIBLE
                chip.setOnClickListener { seleccionarProducto(item) }
            } else {
                chip.visibility = View.GONE
            }
        }
        binding.layoutSugerencias.visibility = View.VISIBLE
    }

    private fun ocultarSugerencias() {
        binding.layoutSugerencias.visibility = View.GONE
    }

    private fun seleccionarProducto(seleccionado: EditarProducto) {
        ocultarSugerencias()

        // Evitamos que el cambio de texto dispare otra búsqueda
        binding.etBuscarProducto.removeTextChangedListener(buscadorTextWatcher)

        binding.etBuscarProducto.setText(seleccionado.nombreProducto)
        binding.etNombreProducto.setText(seleccionado.nombreProducto)
        binding.etDescripcionProducto.setText(seleccionado.presentacion)
        val puntuacionCantidad = seleccionado.cantidad.toString().replace(Regex("""(\d)(?=(\d{3})+(?!\d))"""), "$1.")
        val puntuacionPrecioProducto = seleccionado.valorVenta.toString().replace(Regex("""(\d)(?=(\d{3})+(?!\d))"""), "$1.")
        val puntuacionPrecioCompra = seleccionado.valorCompra.toString().replace(Regex("""(\d)(?=(\d{3})+(?!\d))"""), "$1.")
        binding.etCantidadProducto.setText(puntuacionCantidad)
        binding.etPrecioProducto.setText(puntuacionPrecioProducto)
        binding.etPrecioCompra.setText(puntuacionPrecioCompra)
        binding.etNombreProducto.isEnabled = true
        binding.etDescripcionProducto.isEnabled = true
        binding.etCantidadProducto.isEnabled = true
        binding.etPrecioProducto.isEnabled = true
        binding.etPrecioCompra.isEnabled = true

        // Restauramos el listener
        binding.etBuscarProducto.addTextChangedListener(buscadorTextWatcher)

        binding.btnGuardarProducto.isEnabled = true
        binding.cvFormulario.alpha = 1f
        binding.btnGuardarProducto.setOnClickListener {
            editarProducto(seleccionado)
        }

        WindowInsetsControllerCompat(window, binding.etBuscarProducto)
            .hide(WindowInsetsCompat.Type.ime())
    }

    fun buscarProducto() {
        val producto = binding.etBuscarProducto.text.toString().lowercase().trimEnd()
        if (producto.isEmpty()) return

        val cedula = getSharedPreferences("SesionTendero", MODE_PRIVATE)
            .getString("cedula", "") ?: return

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val conexion = ConexionServiceTienda.create()
                val response = conexion.buscarProductos(BuscarProductos(cedula, producto))

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        ocultarSugerencias()
                        val dialog = BuscarProductoDialog(producto, response.body()!!) { seleccionado ->
                            seleccionarProducto(seleccionado)
                        }
                        dialog.show(supportFragmentManager, "buscar_producto")
                    } else {
                        Toast.makeText(this@fragment_editar_producto, "No se encontraron productos", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@fragment_editar_producto, "Error de conexión", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun editarProducto(producto: EditarProducto) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val conexion = ConexionServiceTienda.create()

                fun String.toCleanInt(): Int {
                    return this.replace(".", "").replace(",", "").toIntOrNull() ?: 0
                }
                val editado = EditarProducto(
                    cantidad = binding.etCantidadProducto.text.toString().toCleanInt(),
                    idTendero = producto.idTendero,
                    idInventario = producto.idInventario,
                    nombreProducto = binding.etNombreProducto.text.toString().lowercase(),
                    presentacion = binding.etDescripcionProducto.text.toString(),
                    valorVenta = binding.etPrecioProducto.text.toString().toCleanInt(),
                    valorCompra = binding.etPrecioCompra.text.toString().toCleanInt()                )
                val response = conexion.editarProducto(editado)

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@fragment_editar_producto, "Producto editado con éxito", Toast.LENGTH_SHORT).show()
                        inicializarEditarProducto()
                    }
                    else{
                        when (response.code()){
                            409 -> Toast.makeText(this@fragment_editar_producto, "Ya existe un producto con ese nombre y presentación", Toast.LENGTH_SHORT).show()
                            500 -> Toast.makeText(this@fragment_editar_producto, "Error en el servidor", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("ErrorEditar", e.message ?: "")
            }
        }
    }

    fun inicializarEditarProducto() {
        ocultarSugerencias()
        binding.etNombreProducto.setText("")
        binding.etPrecioProducto.setText("")
        binding.etDescripcionProducto.setText("")
        binding.etCantidadProducto.setText("")
        binding.etPrecioCompra.setText("")

        binding.etBuscarProducto.removeTextChangedListener(buscadorTextWatcher)
        binding.etBuscarProducto.setText("")
        binding.etBuscarProducto.addTextChangedListener(buscadorTextWatcher)

        binding.btnGuardarProducto.isEnabled = false
        binding.cvFormulario.alpha = 0.5f
    }
}