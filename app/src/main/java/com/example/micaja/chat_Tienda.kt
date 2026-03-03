package com.example.micaja

import android.content.ContentValues.TAG
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.micaja.Adapter.Adapter
import com.example.micaja.ConexionService.ConexionServiceTienda
import com.example.micaja.databinding.ActivityChatTiendaBinding
import com.example.micaja.models.ModeloBase
import com.example.micaja.models.compra_Mercancia
import com.example.micaja.models.gastoDetectado
import com.example.micaja.models.modelo
import com.example.micaja.utils.SesionManager
import com.example.micaja.viewmodel.TenderoViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.String
import kotlin.apply
import kotlin.getValue

var montoVentas = 0
var montoGastos = 0
var montoCostos = 0
var baseInicial = 0
var credito: Boolean = false
var montoCredito = 0

val diccionario = mapOf(
    "abrir" to listOf("abierto","iniciar","inicio","open","abrir","abriendo","comenzar","comienzo","arrancar","empezar","empezemos","dia","día"),
    "cerrar" to listOf("acabar", "acabé", "cerrar","cerrando","cierre", "cierrame", "end", "final", "finalizar", "terminar"),
    "venta" to listOf ("ingreso", "ingresos" ,"venta","ventas", "vende", "vendí", "vendido", "vendiendo", "vendieron", "vendimos", "vendo", "vendió", "vendidos"),
    "compra" to listOf ("compra", "comprar", "compras", "compré", "costo", "costos", "mercancia", "pagamos", "pago", "pague" ,"pagué", "pedido", "proveedor", "provedor"),
    "gasto" to listOf ("egreso", "egresos", "gastamos", "gastan", "gastando", "gastaron", "gasté", "gasto", "gastó", "gastos", "salida", "salidas"),
    "credito" to listOf ("credito", "crédito", "créditos", "creditos", "fia", "fias", "fiao", "fiaos", "fiado", "fiados", "fiar", "fié"),
    "efectivo" to listOf("a la mano", "billete", "billetes", "cash", "contado", "dinero", "efectivo", "efectivos", "moneda", "monedas", "pagan", "plata"),
    "abono" to listOf ("abonar", "abono", "abonos", "abonó", "cuota", "cuotas", "adelantar" ,"adelanto", "adelantos", "deuda"),
    "agregar producto" to listOf ("agregar producto", "añadir producto" ,"nuevo producto", "producto nuevo"),
    "agregar cliente" to listOf("agregar nombre", "añadir cliente", "cliente nuevo", "nuevo cliente")
)

class chat_Tienda : AppCompatActivity() {
    lateinit var mensaje: String
    private lateinit var binding: ActivityChatTiendaBinding
    private val RQ_SPEECH_REC = 102

    var dataset = mutableListOf<modelo>()
    var sistemaData = mutableListOf<modelo>()
    lateinit var Adapter: Adapter
    lateinit var estadoTienda: SharedPreferences
    lateinit var estadoBase: SharedPreferences
    var gastoPendienteMensaje: String? = null
    var esperandoPrecioGasto: Boolean = false
    var costoPendienteMensaje: String? = null
    var esperandoPrecioCosto: Boolean = false

    private val model: TenderoViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {

        try {
            super.onCreate(savedInstanceState)
        } catch (e: Exception) {
            Log.e("ErrorApp", "Error[202]: ${e.message}", e)
        }
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, true)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        binding = ActivityChatTiendaBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        estadoTienda = getSharedPreferences("EstadoTienda", MODE_PRIVATE)
        estadoBase = getSharedPreferences("EstadoBase", MODE_PRIVATE)

        Adapter = Adapter(dataset, sistemaData)
        binding.recyclerMensajes.layoutManager = LinearLayoutManager(this)
        binding.recyclerMensajes.adapter = Adapter

        binding.microphoneBtn.setOnClickListener {
            ConfTrans()
        }

        binding.btnRetroceder.setOnClickListener {
            intent = Intent(this, Principal::class.java)
            startActivity(intent)
        }

        evento()
        observer()
        configuracionMenu()
    }

    private fun configuracionMenu() {
        binding.btnMenu.setOnClickListener {
            val sheet = MenuBottomSheet(
                onComandos = {
                    dialogo_comandos().show(supportFragmentManager, "DialogoComandos")
                },
                onCerrarSesion = {
                    Toast.makeText(this, "Cerrando sesión...", Toast.LENGTH_SHORT).show()
                    SesionManager.cerrarSesion(this)
                }
            )
            sheet.show(supportFragmentManager, "MenuBottomSheet")
        }
    }

    private fun ConfTrans() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "Ha ocurrido un error durante el dictado", Toast.LENGTH_SHORT)
                .show()
        } else {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(
                    RecognizerIntent.EXTRA_PROMPT, "" +
                            "Ocurrió un error durante el dictado. Por favor, intente nuevamente."
                )
            }
            startActivityForResult(intent, RQ_SPEECH_REC)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RQ_SPEECH_REC && resultCode == RESULT_OK) {
            val result = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val nuevoTexto = result?.firstOrNull().orEmpty()
            binding.messageInput.append("\n$nuevoTexto")//Mantiene el mensaje que se ha dictado
        }
    }

    private fun observer() {
        model.mensajes.observe(this, Observer { newName ->
            Adapter.dataset = newName
            Adapter.notifyDataSetChanged()
        })

        model.mensajesSistema.observe(this, Observer { newName ->
            Adapter.sistemaData = newName
            Adapter.notifyDataSetChanged()
            binding.recyclerMensajes.scrollToPosition(Adapter.itemCount - 1)
        })
    }

    fun evento() { binding.sendBtn.setOnClickListener {

        mensaje = binding.messageInput.text.toString()
        val texto = modelo(mensaje)
        binding.messageInput.setText("")
        model.addMensaje(texto)

        //Si no encuentra precio en el gasto
            if (esperandoPrecioGasto) {

                val palabras = mensaje.split(" ")
                for (p in palabras) {
                    val monto = calcularMonto(p)

                    if (monto != null) {
                        val gastoFinal = gastoDetectado(
                            mensaje = gastoPendienteMensaje!!.replaceFirstChar { it.uppercase() },
                            precio = monto //Si detectó gasto y precio en un solo mensaje
                        )
                        montoGastos += monto

                        model.addMensajeSistema(
                            modelo("¡Perfecto! Gasto registrado ${gastoFinal.mensaje} por $$monto"))

                        esperandoPrecioGasto = false
                        gastoPendienteMensaje = null

                        return@setOnClickListener //Se sale solo del if, luego de pulsar el btn de envíar msj, no coloco return solo pq se sale de la fun x completo.
                    }
                }
                model.addMensajeSistema(
                    modelo("Registra un precio para esta operación. Ejemplo: 'Gasté $250.000 en arreglo de goteras.'"))
                return@setOnClickListener
            }

        // Lo mismo con los costos
        if (esperandoPrecioCosto) {
            val palabras = mensaje.split(" ")

            for (p in palabras) {
                val monto = calcularMonto(p)
                if (monto != null) {

                    val costoFinal = compra_Mercancia(
                        mensaje = costoPendienteMensaje!!.replaceFirstChar { it.uppercase() },
                        monto = monto,
                        categoria = "General",
                        proveedor = "Anonimo"
                    )
                    montoCostos += monto

                    model.addMensajeSistema(
                        modelo("Costo registrado ${costoFinal.mensaje} por $$monto"))

                    esperandoPrecioCosto = false
                    costoPendienteMensaje = null
                    return@setOnClickListener
                }
            }
            model.addMensajeSistema(
                modelo("No logré identificar el valor del costo. Intenta nuevamente."))

            return@setOnClickListener
        }

            val tienda = estadoTienda.getBoolean("abierta", false)
            val base = estadoBase.getBoolean("base", false)

            if (tienda) {
                if (base) {
                    procesarCompra(mensaje)
                    procesarGasto(mensaje)
                    procesarCosto(mensaje)
                } else {
                    CoroutineScope(Dispatchers.IO).launch {
                        baseInicial(mensaje)
                    }
                }
            } else {
                val estado = tienda(mensaje)
                if (estado) {
                    model.addMensajeSistema(
                        modelo("Tienda Abierta ¿Cúal es la base del día de hoy?"))
                } else {
                    model.addMensajeSistema(
                        modelo("Tienda Cerrada. Debes abrir tienda para empezar a registrar operaciones."))
                }
            }
        }
    }

    private fun tienda(mensaje: String): Boolean {
        val palabras = mensaje.lowercase().split(Regex("""[\s,.:]+"""))
        var estado = ""
        for (palabra in palabras) {

            if ((diccionario["abrir"]?.contains(palabra) ?: false)) {
                val editor = estadoTienda.edit()
                editor.putBoolean("abierta", true)
                editor.apply()
                estado = "abierto"
            }
        }
        if (estado == "abierto") {
            return true
        }
        return false
    }

    val unidades = listOf(
        "bolsa", "bolsita", "caja", "cajetilla", "canasta", "chuspa",
        "barra", "capsula", "cápsula", "cubeta", "tableta",
        "docena", "gramo", "libra", "kilo", "kilogramos", "litro", "litrón", "onza",
        "envase", "frasco", "plastico", "plástico", "paquete", "vidrio",
        "pequeña", "pequeño", "media", "mediana", "mediano", "grande",
        "garrafa", "lata", "latón", "paca", "sixpack", "six-pack",
        "panal", "sobre", "rollo", "tubo", "unidad", "vasito", "vaso"
    )

    fun procesarCompra(texto: String) {
        // limpieza elimina  puntos y comas
        val textoLimpio = texto.replace(Regex("""(\d)[.,](\d{3})\b"""), "$1$2")
        val textoMinuscula = textoLimpio.lowercase()

        // Detecta (Venta, Cierre, Crédito)
        val palabras = textoMinuscula.split(Regex("""[\s:]+"""))
        val esVenta = palabras.any { diccionario["venta"]?.contains(it) == true }

        if (esVenta) {
            // Separamos por conectores para manejar múltiples productos
            val segmentos = textoMinuscula.split(Regex(",|\\by\\b|;|\\."))
                .map { it.trim() }
                .filter { it.length > 3 }

            val listaResumen = mutableListOf<String>()
            var sumaTotalVenta = 0

            for (segmento in segmentos) {
                val datos = extraerDatosProducto(segmento)

                if (datos != null) {
                    val (nombre, pres, precio) = datos

                    // Acumulamos para el mensaje final y el total
                    listaResumen.add("• $nombre ($pres) por $$precio")
                    sumaTotalVenta += precio

                    Log.d(
                        "DETECCION_VENTA",
                        "REGISTRADO -> PROD: $nombre | PRES: $pres | PRECIO: $precio"
                    )
                }
            }

            // respuesta del sistema
            if (listaResumen.isNotEmpty()) {
                val saludo = "¡Entendido! He registrado lo siguiente:\n"
                val cuerpo = listaResumen.joinToString("\n")
                val total = "\n\nTotal esta operación: $$sumaTotalVenta"

                model.addMensajeSistema(modelo(saludo + cuerpo + total))
            } else {
                model.addMensajeSistema(modelo("Detecté una venta, pero no logré identificar el producto o el precio. Intenta algo como: Arroz libra 3500"))
            }
        }

        if (palabras.any { diccionario["cerrar"]?.contains(it) == true }) {
            cerrarTienda()
        }

        if (palabras.any { diccionario["credito"]?.contains(it) == true }) {
            credito = true
            model.addMensajeSistema(modelo("Iniciando registro de crédito..."))
        }
    }

    private fun extraerDatosProducto(segmento: String): Triple<String, String, Int>? {
        var s = segmento.trim()
        //Extraer Monto
        var precioFinal: Int? = null
        var textoPrecioEncontrado = ""

        val fragmentos = s.split(" ")
        for (f in fragmentos) {
            val resultadoMonto = calcularMonto(f) // 20k, 20.000, etc.
            if (resultadoMonto != null && resultadoMonto >= 50) {
                precioFinal = resultadoMonto
                textoPrecioEncontrado = f
                break
            }
        }

        if (precioFinal == null) return null

        // LIMPIEZA DE PRECIO Y SÍMBOLOS
        s = s.replace(textoPrecioEncontrado, "").replace("$", "").trim()

        // identifica presentacion Usando tu lista de 'unidades'
        var unidadDetectada = "unidad"
        for (u in unidades) {
            if (s.contains(u)) {
                unidadDetectada = u
                s = s.replace(u, "").trim()
                break
            }
        }

        val ruido = listOf(
            "vendi", "vendí", "vende", "venta",
            "un", "una", "de", "a", "por",
            "el", "la", "total", "precio", "valor",
            "además", "ademas", "también"
        )
        var nombreLimpio = s
        for (r in ruido) {
            nombreLimpio = nombreLimpio.replace(Regex("\\b$r\\b", RegexOption.IGNORE_CASE), "")
        }

        nombreLimpio = nombreLimpio.trim().replace(Regex("\\s+"), " ")

        return if (nombreLimpio.isNotEmpty()) {
            Triple(nombreLimpio.replaceFirstChar { it.uppercase() }, unidadDetectada, precioFinal)
        } else null
    }

    fun procesarGasto(texto: String) {
        val textoLimpio = texto.replace(Regex("""(\d)[.,](\d{3})\b"""), "$1$2")
        val textoMinuscula = textoLimpio.lowercase()
        val palabras = textoMinuscula.split(Regex("""[\s:]+"""))

        val esGasto = palabras.any { diccionario["gasto"]?.contains(it) == true }

        if (esGasto) {
            val segmentos = textoMinuscula.split(Regex(",|\\by\\b|;|\\."))
                .map { it.trim() }
                .filter { it.length > 6 }

            val resumenGasto = mutableListOf<String>()
            var sumaTotalGasto = 0

            for (segmento in segmentos) {
                val datos = registrarGasto(segmento)

                if (datos != null) {
                    val (justificacion, precio) = datos
                    resumenGasto.add("• $justificacion por $$precio")
                    sumaTotalGasto += precio
                    montoGastos += precio
                }
            }

            if (resumenGasto.isNotEmpty()) {
                val respuesta = "¡Entendido! He registrado un gasto por un total de $$sumaTotalGasto\n"
                val cuerpo = resumenGasto.joinToString("\n")

                model.addMensajeSistema(modelo(respuesta + cuerpo))
            } else {
                model.addMensajeSistema(modelo("Detecté un gasto, pero no el total."))
                //Si identifica un gasto, pero no el total, el tendero dicta precio y se anida el mensaje con el precio.
                Log.d("DETECCION_GASTO", "REGISTRADO -> $palabras")
            }
        }
    }

    fun registrarGasto(sgmnt: String): gastoDetectado? {
        var s = sgmnt.trim()
        var precioFinal: Int? = null
        var textoPrecioEncontrado = ""

        val fragmentos = s.split(" ")
        for (f in fragmentos) {
            val resultadoMonto = calcularMonto(f)
            if (resultadoMonto != null && resultadoMonto >= 50) {
                precioFinal = resultadoMonto
                textoPrecioEncontrado = f
                break
            }
        }

        if (precioFinal == null) return null
        s = s.replace(textoPrecioEncontrado, "").replace("$", "").trim()

        val ruido = listOf("desembolsé", "desembolso", "egreso",
            "gastar", "gasté", "gasto", "salida", "salidas",
            "de", "el", "un", "una", "unas",
            "por", "total", "valor"
        )

        var nombreLimpio = s
        for (r in ruido) {
            nombreLimpio = nombreLimpio.replace(Regex("\\b$r\\b", RegexOption.IGNORE_CASE), "")
        }

        nombreLimpio = nombreLimpio.trim().replace(Regex("\\s+"), " ")

        return if (nombreLimpio.isNotEmpty()) {

            gastoDetectado(
                mensaje = nombreLimpio.replaceFirstChar { it.uppercase() },
                precio = precioFinal
            )
        } else null
    }

    fun procesarCosto(texto: String) {

        val textoLimpio = texto.replace(Regex("""(\d)[.,](\d{3})\b"""), "$1$2")
        val textoMinuscula = textoLimpio.lowercase()
        val palabras = textoMinuscula.split(Regex("""[\s:]+"""))
        val esUnCosto = palabras.any { diccionario["compra"]?.contains(it) == true }

        if (!esUnCosto) return

        val sgmnts = textoMinuscula
            .split(Regex(",|\\by\\b|;|\\."))
            .map { it.trim() }
            .filter { it.length > 6 }

        val resumenCostos = mutableListOf<String>()
        var sumaTotalCostos = 0
        var detectoAlguno = false

        for (segmento in sgmnts) {
            val costoDetectado = registrarCosto(segmento)

            if (costoDetectado != null) {

                detectoAlguno = true

                resumenCostos.add("• ${costoDetectado.mensaje} por $${costoDetectado.monto}")
                sumaTotalCostos += costoDetectado.monto
                montoCostos += costoDetectado.monto

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val service = ConexionServiceTienda.create()
                        service.compra_Mercancia(costoDetectado)
                    } catch (e: Exception) {
                        Log.e("ERROR_COSTO", e.message ?: "Error")
                    }
                }
            }
        }

        // 🔥 SI detectó compra pero NO detectó precio
        if (!detectoAlguno) {

            esperandoPrecioCosto = true
            costoPendienteMensaje = texto

            model.addMensajeSistema(
                modelo("Detecté una compra, pero no el valor. ¿Cuál fue el precio?")
            )

            return
        }

        if (resumenCostos.isNotEmpty()) {
            val mensajeSistema = "¡Entendido! Registré esta compra de mercancía:\n" +
                    resumenCostos.joinToString("\n") +
                    "\n\nTotal facturado: $$sumaTotalCostos"

            model.addMensajeSistema(modelo(mensajeSistema))
        }
    }

    fun registrarCosto(segmento: String): compra_Mercancia? {
        var s = segmento.trim()
        var precioFinal: Int? = null
        var textoPrecioEncontrado = ""

        val fragmentos = s.split(" ")
        for (f in fragmentos) {
            val resultadoMonto = calcularMonto(f)
            if (resultadoMonto != null && resultadoMonto >= 50) {
                precioFinal = resultadoMonto
                textoPrecioEncontrado = f
                break
            }
        }

        if (precioFinal == null) return null

        s = s.replace(textoPrecioEncontrado, "").replace("$", "").trim()

        val ruido = listOf(
            "compra", "compras", "compré", "costo", "costó",
            "mercancía", "pagaron", "pagué", "pago", "pagó",
            "pedido", "proveedores", "de", "por",
            "además", "también", "total", "valor"
        )

        var nombreLimpio = s
        for (r in ruido) {
            nombreLimpio = nombreLimpio.replace(Regex("\\b$r\\b", RegexOption.IGNORE_CASE), "")
        }
        nombreLimpio = nombreLimpio.trim().replace(Regex("\\s+"), " ")

        return if (nombreLimpio.isNotEmpty()) {
            compra_Mercancia(
                mensaje = nombreLimpio.replaceFirstChar { it.uppercase() },
                monto = precioFinal,
                categoria = "General",
                proveedor = "Desconocido"
            )
        } else null
    }

    fun cerrarTienda() {
        val editor = estadoTienda.edit()
        editor.putBoolean("abierta", false)
        editor.apply()
        Log.d(TAG, "La tienda se acaba de cerrar")
        estadoBase.edit().putBoolean("base", false).apply()
        baseInicial = 0
        model.addMensajeSistema(modelo("la tienda se ha cerrado correctamente"))

    }

    suspend fun baseInicial(mensaje: String) {
        val prefs = getSharedPreferences("SesionTendero", MODE_PRIVATE)
        val cedula = prefs.getString("cedula", null)

        var error = 0
        val palabras = mensaje.lowercase().split(Regex("""[\s:]+"""))
        for (palabra in palabras) {
            val base = calcularMonto(palabra)
            if (base != null) {
                try {
                    val service = ConexionServiceTienda.create()
                    val envioBase = ModeloBase(cedula!!, base)
                    val respuesta = withContext(Dispatchers.IO) { service.addBase(envioBase) }
                    if (respuesta.isSuccessful) {
                        baseInicial = base
                        estadoBase.edit().putBoolean("base", true).apply()
                        val mensajeSistema =
                            modelo("Se ha registrado una Base inicial de ${base}")
                        withContext(Dispatchers.Main) {
                            model.addMensajeSistema(mensajeSistema)
                        }

                    } else {
                        val mensajeSistema =
                            modelo("Parece que ha ocurrido un error, por favor vuelve a intentarlo")
                        withContext(Dispatchers.Main) {
                            model.addMensajeSistema(mensajeSistema)
                        }
                    }

                } catch (e: Exception) {
                    error = 1
                    withContext(Dispatchers.Main) {
                        model.addMensajeSistema(modelo("No se pudo conectar al servidor. Verifica tu red."))
                    }
                }
                break
            }
        }

        if (baseInicial == 0 && error == 0) {
            val mensajeSistema =
                modelo("La base no se ha podido identificar, por favor vuelve a intentarlo")
            withContext(Dispatchers.Main) {
                model.addMensajeSistema(mensajeSistema)
            }
        }
    }
}

fun calcularMonto(palabra: String): Int? {
    return when {
        // "$20k" o "20k" → 20000
        palabra.matches(Regex("""\$?\d+k""", RegexOption.IGNORE_CASE)) -> {
            val base = palabra.removePrefix("$").removeSuffix("k").toIntOrNull()
            base?.times(1000)
        }

        // "$20.000" o "$1,000.000" → 20000 o 1000000
        palabra.matches(Regex("""\$?\d{1,3}([.,]\d{3})+""")) -> {
            palabra.replace("$", "").replace(".", "").replace(",", "").toIntOrNull()
        }

        // "$20000" o "20000" → 20000
        palabra.matches(Regex("""\$?\d+""")) -> {
            palabra.removePrefix("$").toIntOrNull()
        }

        else -> null
    }
}