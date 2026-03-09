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
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.micaja.Adapter.Adapter
import com.example.micaja.ConexionService.ConexionServiceTienda
import com.example.micaja.Operaciones.OperacionVenta
import com.example.micaja.databinding.ActivityChatTiendaBinding
import com.example.micaja.models.Identificacion
import com.example.micaja.models.ModeloBase
import com.example.micaja.models.cliente1
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
import kotlin.apply
import kotlin.getValue

var montoVentas = 0
var montoGastos = 0
var montoCostos = 0
var baseInicial = 0

var cantidad: String = ""

var i: Int = 1

var procesoActivo = "NINGUNO"
var estadoCredito = "NINGUNO"

var montoCredito = 0

val diccionario = mapOf(
    "abrir" to listOf("abierto","iniciar","inicio","open","abrir","abriendo","comenzar","comienzo","arrancar","empezar","empezemos","dia","día"),
    "cerrar" to listOf("acabar", "cerrar","cerrando","cierre", "end", "final", "finalizar", "terminar"),
    "venta" to listOf ("ingreso", "ingresos" ,"venta","ventas", "vende", "vendí", "vendido", "vendiendo", "vendieron", "vendimos", "vendo", "vendió", "vendidos"),
    "compra" to listOf ("compra de mercancía", "comprar", "compras", "compré", "costo", "costos", "pagamos", "pago de", "pague" ,"pagué", "pedido"),
    "gasto" to listOf ("egreso", "egresos", "gastamos", "gastan", "gastando", "gastaron", "gasté", "gasto", "gastó", "gastos"),
    "credito" to listOf ("credito", "crédito", "créditos", "creditos", "fiado a", "fiado", "fiados", "fiar", "fié"),
    "efectivo" to listOf("efectivo","efectivos", "plata", "paga", "a la mano", "contado", "dinero", "efectivito"),
    "abono" to listOf ("abonar", "abono", "abonos", "cuota", "adelantar" ,"adelanto"),
    "agregar producto" to listOf ("agregar producto", "añadir producto" ,"nuevo producto", "producto nuevo"),
    "agregar cliente" to listOf("agregar nombre", "añadir cliente", "cliente nuevo", "nuevo cliente"),
    "si" to listOf("SI", "Si", "si", "sI", "Yes", "YES", "YeS", "yES", "yEs", "yeS"),
    "no" to listOf("NO","No","no","nO" )
)

class chat_Tienda : AppCompatActivity() {
    lateinit var mensaje: String
    private lateinit var binding: ActivityChatTiendaBinding
    private val RQ_SPEECH_REC = 102

    var indicador: Boolean? = false
    var dataset = mutableListOf<modelo>()
    var sistemaData = mutableListOf<modelo>()
    lateinit var Adapter: Adapter
    lateinit var estadoTienda: SharedPreferences
    lateinit var estadoBase: SharedPreferences

    var operacionVenta = OperacionVenta()




    private val model: TenderoViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {

        try {
            super.onCreate(savedInstanceState)
        } catch (e: Exception) {
            Log.e("ErrorApp", "Error inesperado [202]: ${e.message}", e)
        }

        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, true)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        binding = ActivityChatTiendaBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime()) // Detecta el teclado

            // Si el teclado está abierto (ime.bottom > 0), usamos ese valor.
            // Si está cerrado, usamos el valor de la barra del sistema.
            val bottomPadding = if (ime.bottom > 0) ime.bottom else systemBars.bottom

            v.setPadding(systemBars.left, systemBars.top, systemBars.right, bottomPadding)

            // Devolvemos los insets consumidos para que no se apliquen doble
            WindowInsetsCompat.CONSUMED
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
                            "No se escuchó. Intentalo nuevamente"
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
    fun filtarPalabras(msj: String): Boolean {
        val textoLimpio = msj.replace(Regex("""(\d)[.,](\d{3})\b"""), "$1$2").lowercase()
        val palabras = textoLimpio.split(Regex("""[\s,.:]+"""))

        for (l in diccionario.values) {
            for (p in palabras) {
                if (l.contains(p)) {
                    return true
                }
            }
        }
        if (operacionVenta.inicio){
            return true
        }
        return false
    }

    fun evento() {
        binding.sendBtn.setOnClickListener {
            mensaje = binding.messageInput.text.toString()
            val texto = modelo(mensaje)
            val tienda = estadoTienda.getBoolean("abierta", false)
            val base = estadoBase.getBoolean("base", false)
            binding.messageInput.setText("")
            model.addMensaje(texto)

            // Ocultar el teclado al presionar enviar
            WindowInsetsControllerCompat(window, binding.messageInput)
                .hide(WindowInsetsCompat.Type.ime())

            val textoLimpio = mensaje.replace(Regex("""(\d)[.,](\d{3})\b"""), "$1$2").lowercase()
            val palabras = textoLimpio.split(Regex("""[\s,.:]+"""))

            if (tienda) {
                if (base) {
                    if (operacionVenta.inicio){
                        if (palabras.contains("fin")){
                            operacionVenta.inicio = false
                            model.addMensajeSistema(modelo("Venta finalizada"))
                        }
                    }else{
                        val respuesta= operacionVenta.procesarListaProductos(textoLimpio)
                        model.addMensajeSistema(modelo(respuesta))
                    }
                    lifecycleScope.launch(Dispatchers.IO) {

                        withContext(Dispatchers.Main) {
                            procesarGasto(mensaje)
                        }
                        ////////ABONOS
                        withContext(Dispatchers.Main){
                            procesarAbonos(mensaje)
                        }
                    }
                    Log.d(TAG, "El valor de la venta es ${montoVentas}")
                    Log.d(TAG, "El valor de los gastos es ${montoGastos}")
                    Log.d(TAG, "El valor de los costos es ${montoCostos}")
                } else {
                    CoroutineScope(Dispatchers.IO).launch {
                        baseInicial(mensaje)
                        Log.d(TAG, "El valor de la base inicial es de ${baseInicial}")
                    }
                    return@setOnClickListener
                }
                val operaciones = filtarPalabras(mensaje)
                if (operaciones){
                    val esVenta = diccionario["venta"]?.any { palabras.contains(it) } == true

                    if (esVenta) {
                        operacionVenta.inicio = true
                        model.addMensajeSistema(modelo("¡Venta iniciada! Dicta los productos uno a uno o di 'fin'."))
                    }else{
                        procesarGasto(mensaje)
                        procesarCosto(mensaje)
                        Log.d(TAG, "El valor de la venta es ${montoVentas}")
                        Log.d(TAG, "El valor de los gastos es ${montoGastos}")
                        Log.d(TAG, "El valor de los costos es ${montoCostos}")
                    }
                }else{
                    val errorMsj = modelo("No se pudo detectar  la operacion, por favor vuelve a intentarlo")
                    model.addMensajeSistema(errorMsj)
                }
            } else {
                val estado = tienda(mensaje)
                if (estado) {
                    Log.d(TAG, "La tienda está Abierta")
                    val mensaje = modelo("Tienda Abierta ¿Cúal es la base del día de hoy?")
                    model.addMensajeSistema(mensaje)
                } else {
                    Log.d(TAG, "La tienda está Cerrada")
                    val mensajeSistema = modelo("Tienda Cerrada. Reintente nuevamente.")
                    model.addMensajeSistema(mensajeSistema)
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
        "caja",
        "libra",
        "kilo",
        "bolsita",
        "unidad",
        "paquete",
        "bolsa",
        "paca",
        "litro",
        "pequeña",
        "grande",
        "sobre",
        "mediana",
        "vidrio"
    )


    //////////////////////////////ABONOS
    val abono=Abonos()
    fun procesarAbonos(texto: String) {
        val preferencia = getSharedPreferences("SesionTendero", MODE_PRIVATE)
        val idTendero = preferencia.getString("cedula", "") ?: ""
        lifecycleScope.launch {
            val fueAbono = abono.procesarRespuesta(texto, { msg ->
                model.addMensajeSistema(msg)
            }, idTendero)

            if (fueAbono)
                return@launch

            val textoLimpio = texto.replace(Regex("""(\d)[.,](\d{3})\b"""), "$1$2")
            val textoMinuscula = textoLimpio.lowercase()
            val palabras = textoMinuscula.split(Regex("""[\s:]+"""))

            val esAbono = palabras.any { diccionario["abono"]?.contains(it) == true }

            if (esAbono) {
                abono.iniciarFlujoAbono(model::addMensajeSistema)
            }
        }
    }


    suspend fun procesarCompra(texto: String) {
        val prefs = getSharedPreferences("SesionTendero", MODE_PRIVATE)
        val cedula_tendero = prefs.getString("cedula", null) ?: return

        // limpieza elimina  puntos y comas
        val textoLimpio = texto.replace(Regex("""(\d)[.,](\d{3})\b"""), "$1$2")
        val textoMinuscula = textoLimpio.lowercase()

        // Detecta (Venta, Cierre, Crédito)
        val palabras = textoMinuscula.split(Regex("""[\s:]+"""))
        Log.i("palabra", palabras.toString())
        val esVenta = palabras.any { diccionario["venta"]?.contains(it) == true }
        val esCredito = palabras.any{diccionario["credito"]?.contains(it)==true}

        if (esVenta && esCredito) {
            procesoActivo = "credito"
            estadoCredito = "esperando_cedula"
            indicador = true

            model.addMensajeSistema(
                modelo("He detectado venta a crédito. Dicte el número de cédula del cliente.")
            )
            return
        }
        if (esVenta && indicador == false){
            indicador = false
            // Separamos por conectores para manejar múltiples productos
            val segmentos = textoMinuscula.split(Regex(",|\\by\\b|;|\\."))
                .map { it.trim() }
                .filter { it.length > 3 }

            Log.i("segmentos", segmentos.toString())

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
            // respuesta sistema
            if (listaResumen.isNotEmpty()) {
                val saludo = "¡Entendido! He registrado lo siguiente:\n"
                val cuerpo = listaResumen.joinToString("\n")
                val total = "\n\nTotal esta operación: $$sumaTotalVenta"

                model.addMensajeSistema(modelo(saludo + cuerpo + total))
            } else {
                model.addMensajeSistema(modelo("Detecté una venta, pero no logré identificar el producto o el precio. Intenta algo como: Arroz libra 3500"))
            }
            return
        }
        if (procesoActivo == "credito" && indicador == true) {
                if (estadoCredito == "esperando_cedula") {
                    val cedula = textoMinuscula.replace(Regex("[^0-9]"), "")
                    Log.i("verificacion", "Llega hasta aca" )

                    if ((cedula.length > 10) || (cedula.length < 6)) {
                        model.addMensajeSistema(modelo("No pude reconocer la cédula. Por favor dictala nuevamente. Debe tener entre 6 y 10 dígitos."))
                    } else {
                        val conexion = ConexionServiceTienda.create()
                        Log.i("cedula tendero", cedula_tendero)
                        val respuesta =
                            conexion.busca_cliente(Identificacion(cedula, cedula_tendero))
                        if (respuesta.body()!!.nombre != null && respuesta.body()!!.saldo != null) { // Si usas Response<>
                            Log.i("hay respuesta", respuesta.body()?.saldo.toString())
                            estadoCredito = "pedir_cantidad"
                            model.addMensajeSistema(modelo("Cliente encontrado: ${respuesta.body()?.nombre}.\nSaldo total: ${respuesta.body()?.saldo}\n¿Cuántos productos compró fiados?"))
                        } else {
                            model.addMensajeSistema(modelo("El cliente con la cedula $cedula no se encuentra registrado. ¿Le gustaría registrarlo?"))
                            estadoCredito = "registrar_cliente"
                        }
                    }
                }

                if (estadoCredito == "registrar_cliente"){
                    val si = palabras.any { diccionario["si"]?.contains(it) == true}
                    val no = palabras.any { diccionario["no"]?.contains(it) == true}
                    if (si){
                        model.addMensajeSistema(modelo("¿Cuál es el nombre del cliente?"))
                    }else if (no){
                        model.addMensajeSistema(modelo("Saliendo de credito"))
                        indicador = false
                    }
                    else{
                        model.addMensajeSistema(modelo("Lo siento, no pude entender la respuesta. Dictela de nuevo"))
                    }
                }
                if (estadoCredito == "pedir_cantidad") {
                    cantidad = textoMinuscula.replace(Regex("[^0-9]"), "")
                    Log.i("cantidad", cantidad.toString())
                    if ((cantidad.toInt() > 0) and (cantidad.toInt() < 100)) {
                        model.addMensajeSistema(modelo("Dicte el primer producto."))
                        estadoCredito = "pedir_productos"
                    }
                }
                if (estadoCredito == "pedir_productos") {
                    Log.i("Valor de i", i.toString())
                    if (cantidad.toInt() > i) {
                        i++
                        model.addMensajeSistema(modelo("Dicte el siguiente producto."))

                    }

                }


            }

        if (palabras.any { diccionario["cerrar"]?.contains(it) == true }) {
            cerrarTienda()
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
            if (resultadoMonto != null && resultadoMonto >= 100) {
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

        // limpieza de piskini
        val ruido = listOf(
            "vendi",
            "vendí",
            "vende",
            "venta",
            "un",
            "una",
            "de",
            "a",
            "por",
            "el",
            "la",
            "total",
            "precio",
            "también"
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
            // 2. Separar por conectores (por si el usuario dice: "Gasto bolsas 5000 y gaseosa 2000")
            val segmentos = textoMinuscula.split(Regex(",|\\by\\b|;|\\."))
                .map { it.trim() }
                .filter { it.length > 3 }

            val resumenGasto = mutableListOf<String>()
            var sumaTotalGasto = 0

            for (segmento in segmentos) {
                // REUTILIZAMOS la función extraerDatosProducto que ya funciona bien
                val datos = extraerDatosProducto(segmento)

                if (datos != null) {
                    val (nombre, pres, precio) = datos
                    resumenGasto.add("• $nombre ($pres) por $$precio")
                    sumaTotalGasto += precio

                    // Actualizamos la variable global de gastos
                    montoGastos += precio
                }
            }

            // 3. Respuesta al usuario
            if (resumenGasto.isNotEmpty()) {
                val saludo = "¡Gasto registrado con éxito!\n"
                val cuerpo = resumenGasto.joinToString("\n")
                val total = "\n\nTotal de este egreso: $$sumaTotalGasto"

                model.addMensajeSistema(modelo(saludo + cuerpo + total))
            } else {
                model.addMensajeSistema(modelo("Detecté un gasto, pero no entendí el producto o el valor. Intenta: 'Gasto en bolsas 5000'"))

                Log.d("DETECCION_GASTO", "REGISTRADO -> PROD: $palabras")

                if (resumenGasto.isNotEmpty()) {
                    val saludo = "¡Entendido! He registrado lo siguiente:\n"
                    val cuerpo = resumenGasto.joinToString("\n")
                    val total = "\n\nTotal esta operación: $$sumaTotalGasto"

                    model.addMensajeSistema(modelo(saludo + cuerpo + total))
                } else {
                    model.addMensajeSistema(modelo("Detecté un gasto pero no pude identificar el precio. ¿Cuál es el precio?"))
                }
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

        // Limpieza de ruido específica para gastos
        val ruido = listOf("gasté", "gasto", "gastar", "gastando", "pagué", "pago", "un", "una", "de", "por", "total")
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
            .filter { it.length > 3 }

        val resumenCostos = mutableListOf<String>()
        var sumaTotalCostos = 0

        for (segmento in sgmnts) {
            val costoDetectado = registrarCosto(segmento)

            if (costoDetectado != null) {

                resumenCostos.add("• ${costoDetectado.mensaje} por $${costoDetectado.monto}")
                sumaTotalCostos += costoDetectado.monto
                montoCostos += costoDetectado.monto

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val service = ConexionServiceTienda.create()
                        service.compra_Mercancia(costoDetectado)
                    } catch (e: Exception) {
                        Log.e("ERROR_COSTO", e.message ?: "Error desconocido")
                    }
                }
            }
        }

        if (resumenCostos.isNotEmpty()) {
            val mensajeSistema = "¡Entendido! Registré esta compra de mercancía:\n" +
                    resumenCostos.joinToString("\n") +
                    "\n\nTotal facturado: $$sumaTotalCostos"
            model.addMensajeSistema(modelo(mensajeSistema))
        } else {
            model.addMensajeSistema(modelo("Detecté una compra, pero no identifiqué el valor. Ejemplo: 'Compré 2 pacas de arroz por 60000'"))
        }
    }

    fun registrarCosto(segmento: String): compra_Mercancia? {
        var s = segmento.trim()
        var precioFinal: Int? = null
        var textoPrecioEncontrado = ""

        val fragmentos = s.split(" ")
        for (f in fragmentos) {
            val resultadoMonto = calcularMonto(f)
            if (resultadoMonto != null && resultadoMonto >= 100) {
                precioFinal = resultadoMonto
                textoPrecioEncontrado = f
                break
            }
        }

        if (precioFinal == null) return null

        s = s.replace(textoPrecioEncontrado, "").replace("$", "").trim()

        val ruido = listOf(
            "compré", "compra", "pagué", "pago", "pagaron", "mercancía",
            "de", "por", "total", "precio", "también", "además", "pedido"
        )

        var nombreLimpio = s
        for (r in ruido) {
            nombreLimpio = nombreLimpio.replace(Regex("\\b$r\\b", RegexOption.IGNORE_CASE), "")
        }
        nombreLimpio = nombreLimpio.trim().replace(Regex("\\s+"), " ")

        return if (nombreLimpio.isNotEmpty()) {
            compra_Mercancia(
                idTendero = "",
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


fun tipoVenta(palabras: List<String>): String {
    for (palabra in palabras) {
        if (diccionario["venta"]?.contains(palabra) ?: false) {
            return "venta"
        } else if (diccionario["gasto"]?.contains(palabra) ?: false) {
            return "gasto"
        } else if (diccionario["costo"]?.contains(palabra) ?: false) {
            return "costo"
        }
    }
    return "sin coincidencias"
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