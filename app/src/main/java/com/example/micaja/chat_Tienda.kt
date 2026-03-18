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
import com.example.micaja.Operaciones.OperacionCosto
import com.example.micaja.Operaciones.OperacionGasto
import com.example.micaja.Operaciones.OperacionProducto
import com.example.micaja.Operaciones.OperacionVenta
import com.example.micaja.databinding.ActivityChatTiendaBinding
import com.example.micaja.models.Identificacion
import com.example.micaja.models.ModeloBase
import com.example.micaja.models.OperacionesInventario
import com.example.micaja.models.cliente1
import com.example.micaja.models.clienteNuevo
import com.example.micaja.models.compra_Mercancia
import com.example.micaja.models.costoDetectado
import com.example.micaja.models.gastoDetectado
import com.example.micaja.models.modelo
import com.example.micaja.utils.SesionManager
import com.example.micaja.viewmodel.TenderoViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.util.Locale
import kotlin.apply
import kotlin.getValue
import kotlin.text.contains

var montoVentas = 0
var montoGastos = 0
var montoCostos = 0
var baseInicial = 0

var cantidad: String = ""

var i: Int = 1

var procesoActivo = "ninguno"
var estadoCredito = "ninguno"

var cliente = "ninguno"

var cedulaCliente : String? = null

var cedulaGlobal = ""

val diccionario = mapOf(
    "abrir" to listOf("abierto","iniciar","inicio","open","abrir","abriendo","comenzar","comienzo","arrancar","empezar","empezemos","dia","día"),
    "cerrar" to listOf("acabar", "cerrar","cerrando","cierre", "end", "final", "finalizar", "terminar"),
    "venta" to listOf ("ingreso", "ingresos" ,"venta","ventas", "vende", "vendí", "vendido", "vendiendo", "vendieron", "vendimos", "vendo", "vendió", "vendidos"),
    "compra" to listOf ("compra", "comprar", "compras", "compré", "costo", "costos", "pagamos", "pagué", "pedido"),
    "gasto" to listOf ("egreso", "egresos", "gastamos", "gastan", "gastando", "gastaron", "gasté", "gasto", "gastó", "gastos"),
    "credito" to listOf ("credito", "crédito", "créditos", "creditos", "fiado a", "fiado", "fiados", "fiar", "fié"),
    "efectivo" to listOf("efectivo","efectivos", "plata", "paga", "contado", "dinero", "efectivito"),
    "abono" to listOf ("abonar", "abono", "abonos", "cuota", "adelantar" ,"adelanto"),
    "agregar" to listOf ("agregar", "añadir", "identificar", "nuevo", "producto"),
    "cliente" to listOf("nombre", "cliente", "nuevo", "cliente"),
    "consultar" to listOf("busca", "buscar", "consulta", "filtrar", "pregunta"),
    "si" to listOf("si", "sí"),
    "cancelar" to listOf("cancela", "cancelalo", "cancelelo", "equivoqué", "equivocación", "salgase", "sálgase", "salir", "reiniciar")
)

class chat_Tienda : AppCompatActivity() {

    val abono=Abonos()

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
    var operacionGasto = OperacionGasto()
    var operacionCosto = OperacionCosto()
    var operacionProducto = OperacionProducto()




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

        val preferencia = getSharedPreferences("SesionTendero", MODE_PRIVATE)

        val cedula = preferencia.getString("cedula", null)

        cedulaGlobal = cedula.toString()


        estadoTienda = getSharedPreferences("EstadoTienda", MODE_PRIVATE)
        estadoBase = getSharedPreferences("EstadoBase", MODE_PRIVATE)

        Adapter = Adapter(dataset, sistemaData)
        binding.recyclerMensajes.layoutManager = LinearLayoutManager(this)
        binding.recyclerMensajes.adapter = Adapter

        binding.microphoneBtn.setOnClickListener {
            ConfTrans()
        }

//        binding.btnRetroceder.setOnClickListener {
//            intent = Intent(this, Principal::class.java)
//            startActivity(intent)
//        }

        evento()
        observer()
        configuracionMenu()
    }

    override fun onResume() {
        super.onResume()
        if (!SesionManager.esSesionValida(this)) { SesionManager.cerrarSesion(this) }
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
            Adapter.notifyDataSetChanged() // 1. Le avisamos al adapter que repinte

            if (Adapter.itemCount > 0) { binding.recyclerMensajes.scrollToPosition(Adapter.itemCount - 1) }
        })
        model.mensajesSistema.observe(this, Observer { newName ->
            Adapter.sistemaData = newName
            Adapter.notifyDataSetChanged()

            if (Adapter.itemCount > 0) { binding.recyclerMensajes.scrollToPosition(Adapter.itemCount - 1) }
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
        if (operacionVenta.inicio){ return true }
        return false
    }

    fun evento() {
        binding.sendBtn.setOnClickListener {
            mensaje = binding.messageInput.text.toString().trim()
            if (mensaje.isEmpty()) {
                return@setOnClickListener
            }

            val tienda = estadoTienda.getBoolean("abierta", false)
            val base = estadoBase.getBoolean("base", false)
            binding.messageInput.setText("")

            model.addMensaje(modelo(mensaje))
            WindowInsetsControllerCompat(window, binding.messageInput)
                .hide(WindowInsetsCompat.Type.ime())

            val textoLimpio = mensaje.replace(Regex("""(\d)[.,](\d{3})\b"""), "$1$2").lowercase()
            val palabras = textoLimpio.split(Regex("""[\s,.:]+"""))
            val esCliente = diccionario["cliente"]?.any { palabras.contains(it) } == true

            if (!SesionManager.esSesionValida(this@chat_Tienda)) {
                SesionManager.cerrarSesion(this@chat_Tienda)
                return@setOnClickListener // Cortamos la ejecución aquí mismo
            }

            val losQueSomos = listOf("venta", "compra", "gasto", "abono", "credito")

            val quiereCambiarFlujo = losQueSomos.any { comando ->
                diccionario[comando]?.any { palabras.contains(it) } == true
            }

            if (quiereCambiarFlujo) {
                operacionCosto.cancelarFlujo()
                abono.cancelarFlujo()
                operacionVenta.inicio = false
                procesoActivo = "ninguno"
                estadoCredito = "ninguno"
                cedulaCliente = null
            }

            if (tienda) {
                if (base) {

                    val quiereCancelar = diccionario["cancelar"]?.any { palabras.contains(it) } == true

                    if (quiereCancelar) {
                        operacionVenta.inicio = false
                        procesoActivo = "ninguno"
                        estadoCredito = "ninguno"
                        cedulaCliente = null
                        operacionCosto.cancelarFlujo()
                        abono.cancelarFlujo()

                        model.addMensajeSistema(modelo("Operación cancelada."))
                        return@setOnClickListener
                    }

                    if (operacionVenta.inicio) {
                        lifecycleScope.launch(Dispatchers.IO) {
                            val clienteID = if (procesoActivo == "credito") (cedulaCliente ?: "") else ""
                            val respuestaChat =
                                operacionVenta.procesarListaProductos(textoLimpio, false, clienteID,cedulaGlobal)
                            withContext(Dispatchers.Main) {
                                model.addMensajeSistema(modelo(respuestaChat))
                            }
                        }
                        return@setOnClickListener
                    }
                    if (procesoActivo == "credito") {
                        lifecycleScope.launch {
                            procesarCompra(mensaje)
                        }
                        if (textoLimpio.contains("fin") || textoLimpio.contains("finalizar")) {

                            procesoActivo = "ninguno"
                            estadoCredito = "ninguno"
                        }
                        return@setOnClickListener
                    }




                    lifecycleScope.launch {
                        val ocupadoConAbono = abono.procesarRespuesta(mensaje, { msg ->
                            model.addMensajeSistema(msg)
                        },cedulaGlobal)

                        if (ocupadoConAbono) return@launch

                        if (esCliente || procesoActivo == "cliente_nuevo" || procesoActivo == "registro_cliente") {
                            nuevoCliente(mensaje)
                            return@launch
                        }
                        val ocupadoConCosto = operacionCosto.procesarRespuesta(mensaje, { msg ->
                            model.addMensajeSistema(msg)
                        }, cedulaGlobal)
                        if (ocupadoConCosto) return@launch

                        val operaciones = filtarPalabras(mensaje)
                        if (operaciones) {
                            val esVenta = diccionario["venta"]?.any { palabras.contains(it) } == true
                            val esGasto = diccionario["gasto"]?.any { palabras.contains(it) } == true
                            val esCompra = diccionario["compra"]?.any { palabras.contains(it) } == true
                            val esAbono = diccionario["abono"]?.any { palabras.contains(it) } == true
                            val esCredito = diccionario["credito"]?.any { palabras.contains(it) } == true
                            val esAgregar = diccionario["agregar"]?.any { palabras.contains(it) } == true

                            if (esCredito){
                                procesarCompra(mensaje)
                            } else if (esAbono) {
                                abono.iniciarFlujoAbono(model::addMensajeSistema)
                            } else if(esGasto){
                                val respuestaGasto = operacionGasto.procesarGasto(mensaje, cedulaGlobal)
                                model.addMensajeSistema(modelo(respuestaGasto))
                            } else if(esCompra) {
                                operacionCosto.iniciarFlujoCosto { msg -> model.addMensajeSistema(msg) }
                            } else if (esVenta) {
                                operacionVenta.inicio = true
                                model.addMensajeSistema(modelo("¡Venta iniciada! Dicta los productos uno a uno o di 'fin'."))
                            } else if(esAgregar && mensaje.contains("producto")){
                                val intent = Intent(this@chat_Tienda, Agregar_Producto::class.java)
                                startActivity(intent)
                            }
                        } else {
                            model.addMensajeSistema(modelo("No se pudo detectar la operación, por favor vuelve a intentarlo"))
                        }
                    }

                } else {
                    CoroutineScope(Dispatchers.IO).launch {
                        baseInicial(mensaje)
                    }
                    return@setOnClickListener
                }
            } else {
                if (tienda(mensaje)) {
                    model.addMensajeSistema(modelo("Tienda Abierta ¿Cuál es la base del día de hoy?"))
                } else {
                    model.addMensajeSistema(modelo("Tienda Cerrada. Reintente nuevamente."))
                }
            }
        }
    }
    private suspend fun nuevo_cliente(
        cedulaCliente: String?,
        cedula_tendero: String?,
        nombre: String?,
        telefono: String?
    ): Boolean {
        val conexion = ConexionServiceTienda.create()
        val respuesta = conexion.insertar_cliente(
            clienteNuevo(
                cedulaCliente!!,
                cedula_tendero!!,
                nombre!!,
                telefono!!
            )
        )
        Log.i("respuesta", respuesta.body().toString())
        if (respuesta.body() == true) {
            return true
        } else{
            model.addMensajeSistema(modelo("Lo siento, ocurrió un error,intentelo de nuevo."))
            return false
        }
    }

    private suspend fun nuevoCliente(texto: String) {
        val prefs = getSharedPreferences("SesionTendero", MODE_PRIVATE)
        val cedula_tendero = prefs.getString("cedula", null) ?: return
        val textoLimpio = texto.replace(Regex("""(\d)[.,](\d{3})\b"""), "$1$2")
        val textoMinuscula = textoLimpio.lowercase()
        if (procesoActivo == "ninguno") {
            model.addMensajeSistema(modelo("Dicte el número de cédula del cliente por favor."))
            procesoActivo = "cliente_nuevo"
            return
        }

        if (procesoActivo == "cliente_nuevo") {
            val textoSinEspacios = textoMinuscula.replace(" ", "")
            val cedulaRegex = Regex("\\d{6,10}")
            val cedula = cedulaRegex.find(textoSinEspacios)?.value
            Log.i("cedula", cedula.toString() )
            if (cedula != null) {
                val respuesta = buscarCliente(cedula, cedula_tendero)
                if (respuesta.body()?.nombre == null) {
                    cedulaCliente = cedula
                    procesoActivo = "registro_cliente"
                    model.addMensajeSistema(modelo("Dicte el nombre del cliente y su número télefonico."))
                    return
                } else {
                    model.addMensajeSistema(modelo("Cliente encontrado: ${respuesta.body()?.nombre}.\nSaldo total: ${respuesta.body()?.saldo}\n"))
                    procesoActivo = "ninguno"
                    return
                }
            }else {
                model.addMensajeSistema(modelo("Lo siento no pude entender la cédula del cliente. Dictela de nuevo."))
                return
            }
        }
        if (procesoActivo == "registro_cliente") {
            val textoSinEspacios = textoMinuscula.replace(" ", "")
            val regexTelefono = Regex("3[\\d\\s]{9,}")
            val telefono = regexTelefono.find(textoSinEspacios)

            // 2. Obtenemos el texto que está antes del número
            if (telefono != null) {
                // 2. CORTAMOS EL NOMBRE (Índices exactos)
                // Cortamos desde el inicio hasta donde empezó el primer '3'
                val nombreFinal = textoMinuscula.substring(0, telefono.range.first).trim()

                // 3. LIMPIAMOS EL TELÉFONO
                // Tomamos desde el '3' hasta el final y le quitamos los espacios
                val telefonoLimpio = telefono.value.replace(" ", "").trim()

                // 4. VALIDACIÓN Y REGISTRO
                if (nombreFinal.length >= 3 && nombreFinal.length <= 40) {
                    val nombreFormateado = nombreFinal.split(" ")
                        .filter { it.isNotBlank() }
                        .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }

                    if (nuevo_cliente(cedulaCliente, cedula_tendero, nombreFormateado, telefonoLimpio)) {
                        model.addMensajeSistema(modelo("El cliente $nombreFormateado con la cédula $cedulaCliente y número $telefonoLimpio registrado con éxito. \nDicte el producto que vendío a crédito."))
                        estadoCredito = "pedir_productos"
                        return
                    }
                } else {
                    model.addMensajeSistema(modelo("Lo siento, no pude entenderte, por favor dicta de nuevo"))
                }
            } else {
                model.addMensajeSistema(modelo("Lo siento, no pude entenderte, por favor dicta de nuevo"))
            }


        }
    }






    private suspend fun buscarCliente(cedula: String?, cedula_tendero: String?): Response<cliente1> {
        val conexion = ConexionServiceTienda.create()
        val respuesta = conexion.busca_cliente(Identificacion(cedula!!, cedula_tendero!!))
        if (respuesta.body()!!.nombre != null && respuesta.body()!!.saldo != null) {
            return respuesta
        }
        return respuesta
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
        if (estado == "abierto") { return true }
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






    suspend fun procesarCompra(texto: String) {
        val prefs = getSharedPreferences("SesionTendero", MODE_PRIVATE)
        val cedula_tendero = prefs.getString("cedula", null) ?: return

        // limpieza elimina  puntos y comas
        val textoLimpio = texto.replace(Regex("""(\d)[.,](\d{3})\b"""), "$1$2")
        val textoMinuscula = textoLimpio.lowercase()

        // Detecta (Venta, Cierre, Crédito)
        val palabras = textoMinuscula.split(Regex("""[\s:]+"""))
        Log.i("palabra12122", palabras.toString())

        val esCredito = palabras.any { diccionario["credito"]?.contains(it) == true }
//
        if (esCredito) {
            procesoActivo = "credito"
            Log.i("entro a credito", procesoActivo.toString())
            Log.i("entro a credito", estadoCredito.toString())

        }


        //---------------CREDITO---------------------------------------------------------------------------------
        if (procesoActivo == "credito" && estadoCredito == "ninguno") {

            val textoSinEspacios = textoMinuscula.replace(" ", "")
            val cedulaRegex = Regex("\\d{6,10}")
            val cedula = cedulaRegex.find(textoSinEspacios)?.value
            Log.i("cedula", cedula.toString())

            if (cedula != null) {
                val respuesta = buscarCliente(cedula, cedula_tendero)
                Log.i("respuesta de cedula", respuesta.toString())
                if (respuesta.body()?.nombre == null) {
                    estadoCredito = "decision"
                    cedulaCliente = cedula
                    model.addMensajeSistema(modelo("El cliente no está registrado. ¿Le gustaría registrarlo?"))
                }else{
                    model.addMensajeSistema(modelo("Cliente encontrado: ${respuesta.body()?.nombre}.\nSaldo total: ${respuesta.body()?.saldo}.\nDicte el producto que vendío a crédito."))
                    estadoCredito = "pedir_productos"
                    cedulaCliente = cedula
                }
                return
            } else {
                model.addMensajeSistema(modelo("Hubo un problema con la operación. Por favor dicte de nuevo la operación."))
                return
            }
        }
        if (estadoCredito == "decision") {

            val si = palabras.any { diccionario["si"]?.contains(it) == true }

            Log.i("si", si.toString())
            if (si) {
                model.addMensajeSistema(modelo("Dicte el nombre del cliente y el número télefonico."))
                estadoCredito = "cliente_nuevo"
                return
            }
            if (textoMinuscula == "no") {
                procesoActivo = "ninguno"
                estadoCredito = "ninguno"
                model.addMensajeSistema(modelo("Ok, saliendo de crédito"))
            } else {
                model.addMensajeSistema(modelo("Lo siento, no pude entenderte, por favor dicta de nuevo"))
            }

        }
        if (estadoCredito == "cliente_nuevo") {
            val textoSinEspacios = textoMinuscula.replace(" ", "")
            val regexTelefono = Regex("3[\\d\\s]{9,}")
            val telefono = regexTelefono.find(textoSinEspacios)

            // 2. Obtenemos el texto que está antes del número
            if (telefono != null) {
                // 2. CORTAMOS EL NOMBRE (Índices exactos)
                // Cortamos desde el inicio hasta donde empezó el primer '3'
                val nombreFinal = textoMinuscula.substring(0, telefono.range.first).trim()

                // 3. LIMPIAMOS EL TELÉFONO
                // Tomamos desde el '3' hasta el final y le quitamos los espacios
                val telefonoLimpio = telefono.value.replace(" ", "").trim()

                // 4. VALIDACIÓN Y REGISTRO
                if (nombreFinal.length >= 3 && nombreFinal.length <= 40) {
                    val nombreFormateado = nombreFinal.split(" ")
                        .filter { it.isNotBlank() }
                        .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }

                    if (nuevo_cliente(cedulaCliente, cedula_tendero, nombreFormateado, telefonoLimpio)) {
                        model.addMensajeSistema(modelo("El cliente $nombreFormateado con la cédula $cedulaCliente y número $telefonoLimpio registrado con éxito. \nDicte el producto que vendío a crédito."))
                        estadoCredito = "pedir_productos"
                        return
                    }
                } else {
                    model.addMensajeSistema(modelo("Lo siento, no pude entenderte, por favor dicta de nuevo"))
                    return
                }
            } else {
                model.addMensajeSistema(modelo("Lo siento, no pude entenderte, por favor dicta de nuevo"))
                return
            }

        }

        if (estadoCredito == "pedir_productos"){
            var operacionVenta = OperacionVenta()
            val fin_credito = diccionario["fin credito"]?.any { frase ->
                textoMinuscula.contains(frase) }
            if (fin_credito == true ){
                estadoCredito = "ninguno"
                procesoActivo = "ninguno"
                model.addMensajeSistema(
                    modelo(
                        operacionVenta.procesarListaProductos(
                            textoLimpio, true,cedulaCliente!!,cedulaGlobal,

                        )
                    ))
            }else {
                val textoLimpio =
                    mensaje.replace(Regex("""(\d)[.,](\d{3})\b"""), "$1$2").lowercase()
                model.addMensajeSistema(
                    modelo(
                        operacionVenta.procesarListaProductos(
                            textoLimpio, false,cedulaCliente!!,cedulaGlobal
                        )
                    )
                )

            }
        }

        if (palabras.any { diccionario["cerrar"]?.contains(it) == true }) {
            cerrarTienda()
        }
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
                val gastoDetectado = registrarGasto(segmento)

                if (gastoDetectado != null) {

                    resumenGasto.add("• ${gastoDetectado.mensaje} por $${gastoDetectado.precio}")
                    sumaTotalGasto += gastoDetectado.precio
                    montoGastos += gastoDetectado.precio

                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val service = ConexionServiceTienda.create()
                            val modelo = gastoDetectado(cedulaGlobal, gastoDetectado.mensaje,
                                gastoDetectado.precio)
                            val respuesta = service.gastosDetectadoss(modelo)
                            Log.i("respuesta", respuesta.toString())
                        } catch (e: Exception) {
                            Log.e("ERROR_GASTOS", e.message ?: "Error desconocido")
                        }
                    }
                }
            }

            if (resumenGasto.isNotEmpty()) {
                val saludo = "¡Gasto registrado con éxito por $$sumaTotalGasto\n"
                val cuerpo = resumenGasto.joinToString("\n")

                model.addMensajeSistema(modelo(saludo + cuerpo))
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

        val ruido = listOf("gasté", "gasto", "gastar", "gastando", "pagué", "pago", "un", "una", "de", "por", "total")
        var nombreLimpio = s
        for (r in ruido) {
            nombreLimpio = nombreLimpio.replace(Regex("\\b$r\\b", RegexOption.IGNORE_CASE), "")
        }

        nombreLimpio = nombreLimpio.trim().replace(Regex("\\s+"), " ")

        return if (nombreLimpio.isNotEmpty()) {

            gastoDetectado(
                idTendero = cedulaGlobal,
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

        for (segmento in sgmnts) {
            val costoDetectado = registrarCosto(segmento)

            if (costoDetectado != null) {

                resumenCostos.add("• ${costoDetectado.cantidadStock} ${costoDetectado.presentacion} por $${costoDetectado.precioCompra}")
                sumaTotalCostos += costoDetectado.precioCompra
                montoCostos += costoDetectado.precioCompra

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val service = ConexionServiceTienda.create()
                        val modelo = OperacionesInventario(cedulaGlobal, costoDetectado.nombre, costoDetectado.presentacion, costoDetectado.cantidadStock, "agregar")
                        val respuesta = service.operacionesInventario(modelo)
                        CoroutineScope(Dispatchers.Main).launch {
                            val datos = costoDetectado(
                                cedulaGlobal, segmento, costoDetectado.precioCompra, costoDetectado.proveedor
                            )
                            val resultado = service.compra_Mercancia(datos)
                            Log.i("respuesta", resultado.toString())
                        }
                        Log.i("respuesta", respuesta.toString())
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

        val cifra = Regex("""\b\d+\b""").find(segmento)
        val cantidad = cifra?.value?.toIntOrNull() ?: 1

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
                cantidadStock = cantidad,
                presentacion = "",
                nombre = nombreLimpio.replaceFirstChar { it.uppercase() },
                precioCompra = precioFinal,
                proveedor = ""
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