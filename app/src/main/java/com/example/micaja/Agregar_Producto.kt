package com.example.micaja

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
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
import com.example.micaja.models.BuscarProductos
import com.example.micaja.models.EditarProducto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class Agregar_Producto : AppCompatActivity() {

    /**
     * Mapa de unidades reconocidas por la app.
     * Cada clave es el nombre canónico de la unidad y su valor es una lista
     * de sinónimos/abreviaturas que el tendero puede escribir.
     * Se usa en [presentacionLista] para validar el campo presentación.
     */
    val unidades = mapOf(
        // Peso
        "gramo"      to listOf("gramo", "gramos", "g", "gr"),
        "libra"      to listOf("libra", "libras", "lb"),
        "kilogramo"  to listOf("kilo", "kilogramos", "kg", "kilos"),
        "onza"       to listOf("onza", "onzas"),

        // Líquidos
        "Litro"      to listOf("litro", "litron", "l", "litros"),
        "militro"    to listOf("ml", "mililitros"),
        "garrafa"    to listOf("garrafa", "gal"),

        // Empaques y Agrupaciones
        "bolsa"      to listOf("bolsa", "bolsita", "chuspa"),
        "caja"       to listOf("caja", "cajetilla"),
        "paquete"    to listOf("paquete", "paca", "sixpack", "sobre"),
        "envase"     to listOf("envase", "frasco", "botella", "tubo", "spray", "rollo"),
        "unidad"     to listOf("unidad", "unidades", "barra", "capsula", "tableta", "docena", "panal", "atado", "hojas", "carta"),
        "recipiente" to listOf("cubeta", "canasta", "cubo", "vaso", "vasito", "lata", "laton"),

        // Tamaños y Medidas
        "pequeño"    to listOf("pequeña", "pequeño", "mini"),
        "mediano"    to listOf("mediana", "mediano", "media"),
        "grande"     to listOf("grande", "jumbo")
    )

    /**
     * Mapa de categorías disponibles en el inventario.
     * La clave es el ID que se envía al servidor y el valor es el nombre visible.
     * Se usa en [categoriaLista] para cargar el AutoComplete y en [boton] para
     * validar que el tendero haya seleccionado una opción válida.
     */
    private val categorias = mapOf(
        1 to "abarrotes", 2 to "bebidas", 3 to "dulcería", 4 to "licores",
        5 to "fruver", 6 to "lácteos", 7 to "aseo personal", 8 to "aseo general",
        9 to "cárnicos", 10 to "papelería", 11 to "fármacos", 12 to "mascotas"
    )

    /** ID de la categoría que el tendero seleccionó del dropdown. -1 significa que aún no eligió. */
    private var idCategoriaSeleccionada: Int = -1

    private lateinit var binding: ActivityAgregarProductoBinding

    // ─────────────────────────────────────────────────────────────────────────
    // BLOQUE DE SUGERENCIAS
    // Agrupa toda la lógica para mostrar, ocultar y reaccionar a sugerencias
    // mientras el usuario escribe el nombre del producto que desea agregar.
    // ─────────────────────────────────────────────────────────────────────────

    private var debounceJob: Job? = null

    /**
     * TextWatcher que escucha cada cambio en el campo nombre del producto.
     * - Primero filtra caracteres no permitidos (solo letras y espacios).
     * - Si el texto queda vacío, cancela cualquier búsqueda pendiente y oculta sugerencias.
     * - Si hay texto válido, cancela la búsqueda anterior (debounce) y lanza una nueva en IO.
     */
    private val buscadorTextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

        override fun afterTextChanged(s: Editable?) {
            // Validación de caracteres: elimina todo lo que no sea letra o espacio
            val textoOriginal = s.toString()
            val textoFiltrado = textoOriginal.replace(Regex("[^a-zA-ZñÑ ]"), "")
            if (textoOriginal != textoFiltrado) {
                // Quitamos el watcher antes de corregir el texto para evitar un bucle infinito
                binding.etNombreProducto.removeTextChangedListener(this)
                binding.etNombreProducto.setText(textoFiltrado)
                binding.etNombreProducto.setSelection(textoFiltrado.length)
                binding.etNombreProducto.addTextChangedListener(this)
                return
            }

            val texto = textoFiltrado.trim()
            if (texto.isEmpty()) {
                ocultarSugerencias()
                debounceJob?.cancel()
                return
            }

            // Cancela la búsqueda previa y lanza una nueva (patrón debounce)
            debounceJob?.cancel()
            debounceJob = CoroutineScope(Dispatchers.IO).launch {
                buscarSugerencias(texto)
            }
        }
    }

    /**
     * Llama al servidor con el texto que el usuario está escribiendo.
     * Corre en un hilo IO y salta al hilo principal solo para tocar la UI.
     * Muestra máximo 3 sugerencias para no saturar la pantalla.
     *
     * @param query Texto actual del campo nombre (ya sin espacios extremos).
     */
    private suspend fun buscarSugerencias(query: String) {
        val cedula = getSharedPreferences("SesionTendero", MODE_PRIVATE)
            .getString("cedula", "") ?: return

        try {
            val conexion = ConexionServiceTienda.create()
            val response = conexion.buscarProductos(BuscarProductos(cedula, query))

            if (response.isSuccessful && response.body() != null) {
                val lista = response.body()!!
                withContext(Dispatchers.Main) { mostrarSugerencias(lista.take(3)) }
            } else {
                withContext(Dispatchers.Main) { ocultarSugerencias() }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Log.e("AgregarSugerencias", "Error: ${e.message}")
                ocultarSugerencias()
            }
        }
    }

    /**
     * Rellena los chips/TextViews de sugerencia con los productos recibidos
     * y los hace visibles. Los slots sobrantes (si la lista tiene menos de 3)
     * se ocultan para no dejar espacios vacíos.
     * Formato mostrado: "NombreProducto  •  presentacion"
     *
     * @param lista Hasta 3 productos que coinciden con lo que el usuario escribió.
     */
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
                // Al tocar una sugerencia se llena el nombre y la presentación del formulario
                chip.setOnClickListener { seleccionarSugerencia(item) }
            } else {
                chip.visibility = View.GONE
            }
        }
        binding.layoutSugerencias.visibility = View.VISIBLE
    }

    /**
     * Oculta el panel de sugerencias completo.
     * Se llama cuando: el campo queda vacío, se selecciona una sugerencia,
     * o se termina de escribir sin interactuar con ninguna opción.
     */
    private fun ocultarSugerencias() {
        binding.layoutSugerencias.visibility = View.GONE
    }

    /**
     * Rellena el nombre y la presentación con el producto sugerido que el usuario tocó.
     * Los campos de precio y cantidad se dejan en blanco a propósito, ya que el tendero
     * está *agregando* un producto nuevo y debe ingresar esos valores manualmente.
     *
     * @param sugerido Producto seleccionado desde las sugerencias.
     */
    private fun seleccionarSugerencia(sugerido: EditarProducto) {
        ocultarSugerencias()

        // Quitamos el watcher antes de setText para no disparar otra búsqueda
        binding.etNombreProducto.removeTextChangedListener(buscadorTextWatcher)
        binding.etNombreProducto.setText(sugerido.nombreProducto)
        binding.etNombreProducto.setSelection(sugerido.nombreProducto.length)
        binding.etDescripcionProducto.setText(sugerido.presentacion ?: "")
        binding.etNombreProducto.addTextChangedListener(buscadorTextWatcher)

        WindowInsetsControllerCompat(window, binding.etNombreProducto)
            .hide(WindowInsetsCompat.Type.ime())
    }

    // ─────────────────────────────────────────────────────────────────────────

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
        binding.btnRetroceso.setOnClickListener { finish()
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

        // Registra el watcher: a partir de aquí cada letra que escriba el usuario
        // dispara el flujo de sugerencias definido en el bloque de sugerencias
        binding.etNombreProducto.addTextChangedListener(buscadorTextWatcher)
    }

    /**
     * Configura los listeners del botón guardar y del campo de categoría.
     *
     * - El campo categoría oculta el teclado al tocarlo (ya que usa un dropdown, no escritura).
     * - El botón guardar valida que todos los campos estén llenos, que la categoría
     *   haya sido seleccionada del listado y que la presentación sea reconocida
     *   antes de llamar a [ejecutarGuardado].
     */
    private fun boton() {
        binding.etCatagoriaProducto.setOnClickListener {
            WindowInsetsControllerCompat(window, binding.etCatagoriaProducto)
                .hide(WindowInsetsCompat.Type.ime())
        }

        binding.btnGuardarProducto.setOnClickListener {
            Log.d("MI_CAJA_DEBUG", "Botón presionado")
            val nombre       = binding.etNombreProducto.text.toString().trim().lowercase()
            val categoriaTxt = binding.etCatagoriaProducto.text.toString().trim().lowercase()
            val presentacion = binding.etDescripcionProducto.text.toString().trim().lowercase()
            val pCompra      = binding.etPrecioProductoCompra.text.toString().trim().lowercase()
            val pVenta       = binding.etPrecioProducto.text.toString().trim().lowercase()
            val cantidad     = binding.etCantidadProducto.text.toString().trim().lowercase()

            if (nombre.isEmpty() || categoriaTxt.isEmpty() || presentacion.isEmpty() ||
                pCompra.isEmpty() || pVenta.isEmpty() || cantidad.isEmpty()) {
                Toast.makeText(this, "Por favor diligencie todos los campos.", Toast.LENGTH_SHORT).show()
            }
            if (idCategoriaSeleccionada == -1) {
                Toast.makeText(this, "Por favor seleccione una categoría de la lista.", Toast.LENGTH_SHORT).show()
            }
            if (presentacionLista(presentacion)) {
                ejecutarGuardado(nombre, presentacion, cantidad, pVenta, pCompra)
            } else {
                Toast.makeText(this, "La presentacion no es reconocida", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Envía el nuevo producto al servidor en un hilo IO.
     * Si la respuesta es exitosa, empaqueta un mensaje de confirmación en el
     * Intent de resultado para que la pantalla anterior pueda mostrarlo,
     * luego cierra esta Activity. Si falla, muestra un Toast con el código de error.
     *
     * @param nombre      Nombre del producto (ya en minúsculas).
     * @param presentacion Presentación validada (ej: "500g", "1 bolsa").
     * @param cantidad    Cantidad en inventario.
     * @param pVenta      Precio de venta.
     * @param pCompra     Precio de compra.
     */
    private fun ejecutarGuardado(
        nombre: String,
        presentacion: String,
        cantidad: String,
        pVenta: String,
        pCompra: String
    ) {
        val preferencia = getSharedPreferences("SesionTendero", MODE_PRIVATE)
        val cedula = preferencia.getString("cedula", null)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val conexion = ConexionServiceTienda.create()
                val modeloProd = AgregarProducto(
                    cedula!!, nombre, presentacion, cantidad.toInt(),
                    pVenta.toInt(), idCategoriaSeleccionada, pCompra.toInt()
                )
                val respuesta = conexion.addProducto(modeloProd)

                launch(Dispatchers.Main) {
                    if (respuesta.isSuccessful) {
                        // Devuelve un mensaje a la Activity anterior para que lo muestre al usuario
                        val data = Intent()
                        data.putExtra(
                            "mensaje_confirmacion",
                            "¡Producto registrado! Se añadió '$nombre' ($presentacion) con $cantidad unidades al inventario."
                        )
                        setResult(RESULT_OK, data)
                        Toast.makeText(this@Agregar_Producto, "Guardado exitoso", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this@Agregar_Producto, "Error al guardar: ${respuesta.code()}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                launch(Dispatchers.Main) {
                    Toast.makeText(this@Agregar_Producto, "Error de red: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * Valida si el texto ingresado en el campo presentación corresponde a una
     * unidad reconocida por la app.
     *
     * Extrae la última palabra alfabética del texto (ej: de "500g" extrae "g",
     * de "1 bolsa" extrae "bolsa") y la busca en los sinónimos de [unidades].
     *
     * @param texto Contenido del campo presentación, ya en minúsculas y sin espacios extremos.
     * @return `true` si la unidad es reconocida, `false` en caso contrario.
     */
    private fun presentacionLista(texto: String): Boolean {
        val soloTexto = Regex("[a-zA-ZñÑ]+").findAll(texto).lastOrNull()?.value ?: ""
        return unidades.values.any { listaSinonimos ->
            listaSinonimos.contains(soloTexto)
        }
    }

    /**
     * Carga la lista de categorías en el AutoCompleteTextView.
     * Al seleccionar un ítem, guarda su ID en [idCategoriaSeleccionada] para
     * que [boton] pueda validar que proviene del listado y no fue escrito a mano.
     */
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
            val nombreSeleccionado = parent.getItemAtPosition(position).toString()
            idCategoriaSeleccionada =
                categorias.entries.find { it.value == nombreSeleccionado }?.key ?: -1
            Log.d("CategoriaSeleccionada: $nombreSeleccionado", "ID: $idCategoriaSeleccionada")
        }
        autoComplete.setOnClickListener { autoComplete.showDropDown() }
    }

    /**
     * Registra los TextWatchers de validación en tiempo real para los campos del formulario.
     * NOTA: El campo [etNombreProducto] NO tiene watcher aquí; su filtro de caracteres
     * está integrado dentro de [buscadorTextWatcher] para que ambas lógicas
     * (filtrar + buscar sugerencias) convivan en un solo listener.
     *
     * - [etDescripcionProducto]: muestra un error inline si la unidad escrita no
     *   está en [unidades], y lo limpia en cuanto el texto es reconocido o el
     *   campo queda vacío.
     */
    private fun validaciones() {
        binding.etDescripcionProducto.addTextChangedListener { s ->
            val texto = s.toString().trim().lowercase()

            when {
                texto.isEmpty()          -> binding.etDescripcionProducto.error = null
                presentacionLista(texto) -> binding.etDescripcionProducto.error = null
                else                     -> binding.etDescripcionProducto.error = "Unidad no reconocida (ej: 500g, 1 bolsa)"
            }
        }
    }
}