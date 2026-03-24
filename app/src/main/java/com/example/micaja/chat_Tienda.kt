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
import androidx.activity.result.contract.ActivityResultContracts
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
import com.example.micaja.Operaciones.OperacionVenta
import com.example.micaja.databinding.ActivityChatTiendaBinding
import com.example.micaja.models.Identificacion
import com.example.micaja.models.ModeloBase
import com.example.micaja.models.cliente1
import com.example.micaja.models.clienteNuevo
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

var baseInicial = 0
var cantidad: String = ""
var i: Int = 1
var procesoActivo = "ninguno"
var estado = "ninguno"
var cedulaCliente : String? = null
var cedulaGlobal = ""

val diccionario = mapOf(
    "abrir" to listOf("abrir", "comenzar", "arrancar", "empezar"),
    "cerrar" to listOf("cerrar", "cerrando", "cierre"),
    "venta" to listOf ("venta", "vendí", "vendieron", "vendió", "ventica"),
    "gasto" to listOf ("gasto", "gasté", "gastar", "gastico"),
    "compra" to listOf ("compra", "compré", "costo", "pagué"),
    "credito" to listOf ("credito", "crédito", "fiado", "fiar", "fié"),
    "abono" to listOf ("abono", "abonar", "cuota", "deuda", "saldo"),
    "agregar" to listOf ("agregar", "añadir", "producto"),
    "consultar" to listOf("busca", "buscar", "consulta", "filtrar", "pregunta"), // Por si lo va a usar ñiñin
    "cliente" to listOf("cliente", "nombre", "nuevo", "deudor"),
    "si" to listOf("si", "sí"),
    "efectivo" to listOf("efectivo", "plata", "paga", "contado", "dinero"),
)

class chat_Tienda : AppCompatActivity() {
    val abono = Abonos()
    private val agregarProductoLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val mensajeConfirmacion = result.data?.getStringExtra("mensaje_confirmacion")
                ?: "Producto guardado correctamente"

            model.addMensajeSistema(modelo(mensajeConfirmacion))
            binding.recyclerMensajes.post { binding.recyclerMensajes.smoothScrollToPosition(Adapter.itemCount - 1) }

        } else {
            val mensajeCierre = result.data?.getStringExtra("mensaje_cierre") ?: "Registro de producto cancelado"
            model.addMensajeSistema(modelo(mensajeCierre))
            binding.recyclerMensajes.post { binding.recyclerMensajes.smoothScrollToPosition(Adapter.itemCount - 1) }
        }
    }
    lateinit var mensaje: String
    private lateinit var binding: ActivityChatTiendaBinding
    private val RQ_SPEECH_REC = 102
    var dataset = mutableListOf<modelo>()
    var sistemaData = mutableListOf<modelo>()
    lateinit var Adapter: Adapter
    lateinit var estadoTienda: SharedPreferences
    lateinit var estadoBase: SharedPreferences
    var operacionVenta = OperacionVenta()
    var operacionGasto = OperacionGasto()
    var operacionCosto = OperacionCosto()
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
            val bottomPadding = if (ime.bottom > 0) ime.bottom else systemBars.bottom
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, bottomPadding)
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
        binding.microphoneBtn.setOnClickListener { ConfTrans() }
        evento()
        observer()
        configuracionMenu()
    }

    override fun onResume() {
        super.onResume()
        if (!SesionManager.esSesionValida(this)) {
            SesionManager.cerrarSesion(this)
        }
    }

    private fun configuracionMenu() {
        binding.btnMenu.setOnClickListener {
            val sheet = MenuBottomSheet(
                onComandos = { dialogo_comandos().show(supportFragmentManager, "DialogoComandos") },
                onCerrarSesion = {
                    SesionManager.cerrarSesion(this, "Has cerrado sesión exitosamente.")
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
                putExtra(RecognizerIntent.EXTRA_PROMPT, "" + "No se escuchó. Intentalo nuevamente")
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

            if (Adapter.itemCount > 0) {
                binding.recyclerMensajes.scrollToPosition(Adapter.itemCount - 1)
            }
        })
        model.mensajesSistema.observe(this, Observer { newName ->
            Adapter.sistemaData = newName
            Adapter.notifyDataSetChanged()

            if (Adapter.itemCount > 0) {
                binding.recyclerMensajes.scrollToPosition(Adapter.itemCount - 1)
            }
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
        if (operacionVenta.inicio) { return true }

        return false
    }

    private fun tiendaTendero(mensaje: String): Boolean {
        val palabras = mensaje.lowercase().split(Regex("""[\s,.:]+"""))
        val tiendaYaAbierta = estadoTienda.getBoolean("abierta", false)

        val quiereAbrir =
            diccionario["abrir"]?.any { palabra -> palabras.contains(palabra) } ?: false

        if (quiereAbrir) {
            if (tiendaYaAbierta) {
                model.addMensajeSistema(modelo("La tienda ya está abierta. ¿En qué puedo ayudarte?"))
                return false
            } else {
                val editor = estadoTienda.edit()
                editor.putBoolean("abierta", true)
                editor.apply()
                return true
            }
        }
        return false
    }

    fun evento() {
        binding.sendBtn.setOnClickListener {
            mensaje = binding.messageInput.text.toString().trim()
            if (mensaje.isEmpty()) { return@setOnClickListener }

            val tienda = estadoTienda.getBoolean("abierta", false)
            val base = estadoBase.getBoolean("base", false)
            binding.messageInput.setText("")
            model.addMensaje(modelo(mensaje))
            WindowInsetsControllerCompat(window, binding.messageInput)
                .hide(WindowInsetsCompat.Type.ime())

            val textoLimpio = mensaje.replace(Regex("""(\d)[.,](\d{3})\b"""), "$1$2").lowercase()
            val palabras = textoLimpio.split(Regex("""[\s,.:]+"""))
            val esCliente = diccionario["cliente"]?.any { palabras.contains(it) } == true
            val consultarCliente = diccionario["consultar"]?.any { palabras.contains(it) } == true
            val esCredito = diccionario["credito"]?.any { palabras.contains(it) } == true

            if (consultarCliente) { procesoActivo = "consultar" }
            else if (esCliente) { procesoActivo = "nuevo_cliente" }
            if (esCredito) { procesoActivo = "credito" }

            if (!SesionManager.esSesionValida(this@chat_Tienda)) {
                SesionManager.cerrarSesion(this@chat_Tienda)
                return@setOnClickListener
            }

            val intentoAbrir = diccionario["abrir"]?.any { palabras.contains(it) } == true
            if (intentoAbrir) {
                val seAbrioAhora = tiendaTendero(mensaje)
                if (seAbrioAhora) {
                    model.addMensajeSistema(modelo("¡Tienda Abierta! ¿Cuál es la base del día de hoy?"))
                }
                return@setOnClickListener
            }
            val CerrarTienda = diccionario["cerrar"]?.any { palabras.contains(it) } == true

            if (CerrarTienda && mensaje.contains("tienda")) {
                val editor = estadoTienda.edit()
                editor.putBoolean("abierta", false)
                editor.apply()
                estadoBase.edit().clear().apply()
                model.addMensajeSistema(modelo("Tienda cerrada correctamente. ¡Buen descanso!"))
                return@setOnClickListener
            }

            if (tienda) {
                if (base) {
                    val quiereCancelar = diccionario["cancelar"]?.any { palabras.contains(it) } == true

                    if (quiereCancelar) {
                        operacionVenta.inicio = false
                        procesoActivo = "ninguno"
                        estado = "ninguno"
                        cedulaCliente = null
                        operacionCosto.cancelarFlujo()
                        abono.cancelarFlujo()
                        model.addMensajeSistema(modelo("Operación cancelada."))
                        return@setOnClickListener
                    }

                    if (procesoActivo == "credito") {
                        lifecycleScope.launch {
                            procesarCredito(mensaje)
                            return@launch
                        }
                        if (textoLimpio.contains("fin") || textoLimpio.contains("finalizar")) {
                            procesoActivo = "ninguno"
                            estado = "ninguno"
                        }
                        return@setOnClickListener
                    }
                    if (operacionVenta.inicio) {
                        lifecycleScope.launch(Dispatchers.IO) {
                            val clienteID =
                                if (procesoActivo == "credito") (cedulaCliente ?: "") else ""
                            val respuestaChat =
                                operacionVenta.procesarListaProductos(
                                    textoLimpio,
                                    false,
                                    clienteID,
                                    cedulaGlobal
                                )
                            withContext(Dispatchers.Main) { model.addMensajeSistema(modelo(respuestaChat)) }
                        }
                        return@setOnClickListener
                    }

                    lifecycleScope.launch {
                        val ocupadoConAbono = abono.procesarRespuesta(mensaje, { msg ->
                            model.addMensajeSistema(msg)
                        }, cedulaGlobal)

                        if (ocupadoConAbono) return@launch
                        if (procesoActivo == "nuevo_cliente") {
                            cliente(mensaje, true)
                            return@launch
                        } else if (procesoActivo == "consultar") {
                            cliente(mensaje, false)
                            return@launch
                        }

                        val ocupadoConGasto = operacionGasto.procesarRespuesta(mensaje, { msg ->
                            model.addMensajeSistema(msg)
                        }, cedulaGlobal)
                        if (ocupadoConGasto) return@launch

                        val ocupadoConCosto = operacionCosto.procesarRespuesta(textoLimpio, { msg ->
                            model.addMensajeSistema(msg)
                        }, cedulaGlobal)
                        if (ocupadoConCosto) return@launch

                        val operaciones = filtarPalabras(mensaje)
                        if (operaciones) {
                            val esVenta =
                                diccionario["venta"]?.any { palabras.contains(it) } == true
                            val esGasto =
                                diccionario["gasto"]?.any { palabras.contains(it) } == true
                            val esCompra =
                                diccionario["compra"]?.any { palabras.contains(it) } == true
                            val esAbono =
                                diccionario["abono"]?.any { palabras.contains(it) } == true
                            val esAgregar =
                                diccionario["agregar"]?.any { palabras.contains(it) } == true

                            if (esAbono) {
                                abono.iniciarFlujoAbono(model::addMensajeSistema)
                            } else if (esGasto) {
                                operacionGasto.iniciarFlujoGasto { msg ->
                                    model.addMensajeSistema(msg)
                                }
                            } else if (esCompra) {
                                operacionCosto.iniciarFlujoCosto { msg ->
                                    model.addMensajeSistema(msg)
                                }
                            } else if (esVenta) {
                                operacionVenta.inicio = true
                                model.addMensajeSistema(modelo("¡Venta iniciada! Dicta los productos uno a uno o di 'fin'."))
                            } else if (esAgregar && mensaje.contains("producto")) {
                                val intent = Intent(this@chat_Tienda, Agregar_Producto::class.java)
                                agregarProductoLauncher.launch(intent)
                            }
                        } else {
                            model.addMensajeSistema(modelo("No se pudo detectar la operación, por favor vuelve a intentarlo"))
                        }
                    }
                } else {
                    CoroutineScope(Dispatchers.IO).launch { baseInicial(mensaje) }
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
        } else {
            model.addMensajeSistema(modelo("Lo siento, ocurrió un error,intentelo de nuevo."))
            return false
        }
    }

    private suspend fun cliente(texto: String, nuevoCliente: Boolean) {
        val prefs = getSharedPreferences("SesionTendero", MODE_PRIVATE)
        val cedula_tendero = prefs.getString("cedula", null) ?: return
        val textoLimpio = texto.replace(Regex("""(\d)[.,](\d{3})\b"""), "$1$2")
        val textoMinuscula = textoLimpio.lowercase()
        val palabras = textoMinuscula.split(Regex("""[\s:]+"""))
        if (palabras.contains("fin")) {
            estado = "ninguno"
            procesoActivo = "niguno"
            if (nuevoCliente) {
                model.addMensajeSistema(modelo("El proceso para registrar un nuevo cliente ha finalizado correctamente."))
            } else {
                model.addMensajeSistema(modelo("El proceso para buscar un nuevo cliente ha finalizado correctamente."))
            }
            return
        }

        if (estado == "ninguno") {
            model.addMensajeSistema(modelo("Dicte el número de cédula del cliente por favor."))
            estado = "cliente"
            return
        }

        if (estado == "cliente") {
            val textoSinEspacios = textoMinuscula.replace(" ", "")
            val cedulaRegex = Regex("\\d{6,}")
            val cedula = cedulaRegex.find(textoSinEspacios)?.value
            Log.i("cedula", cedula.toString())

            if (cedula != null) {
                if (cedula.length > 10 || cedula.length < 6) {
                    Log.i("primer if", "entre a estado cedula")
                    model.addMensajeSistema(modelo("El número de cédula no puede tener mas de 10 digitos, ni menos de 6. Por favor dicte de nuevo."))
                    return
                }
                if (cedula == cedula_tendero) {
                    Log.i("segundo if", "entre a estado cedula")
                    model.addMensajeSistema(modelo("La cédula del cliente no puede ser igual a la del tendero. Por favor dicte de nuevo."))
                    return
                }

                val respuesta = buscarCliente(cedula, cedula_tendero)
                if (respuesta.body()?.nombre == null) {
                    cedulaCliente = cedula
                    if (nuevoCliente == true) {
                        estado = "registro_cliente"
                        model.addMensajeSistema(modelo("Dicte el nombre del cliente y su número télefonico."))
                        return
                    } else {
                        estado = "decision"
                        model.addMensajeSistema(modelo("El cliente no está registrado ¿Le gustaría registrarlo?"))
                        return
                    }
                } else {
                    model.addMensajeSistema(modelo("Cliente encontrado: ${respuesta.body()?.nombre}.\nSaldo total: $${respuesta.body()?.saldo}"))
                    estado = "ninguno"
                    procesoActivo = "ninguno"
                    return
                }
            } else {
                model.addMensajeSistema(modelo("Lo siento no pude entender la cédula del cliente. Dictela de nuevo."))
                return
            }
        }

        if (estado == "decision") {
            val si = palabras.any { diccionario["si"]?.contains(it) == true }

            Log.i("si", si.toString())
            if (si) {
                model.addMensajeSistema(modelo("Dicte el nombre del cliente y su número télefonico."))
                estado = "registro_cliente"
                return
            }
            if (textoMinuscula == "no") {
                procesoActivo = "ninguno"
                estado = "ninguno"
                model.addMensajeSistema(modelo("Ok, saliendo..."))
            } else {
                model.addMensajeSistema(modelo("Lo siento, no pude entenderte, por favor dicta de nuevo"))
            }
        }

        if (estado == "registro_cliente") {
            val regexTelefono = Regex("3[\\d\\s]{9,}")
            val telefono = regexTelefono.find(textoMinuscula)

            if (telefono != null) {
                val nombreFinal = textoMinuscula.substring(0, telefono.range.first).trim()
                val telefonoLimpio = telefono.value.replace(" ", "").trim()

                if (nombreFinal.length >= 3 && nombreFinal.length <= 40) {
                    val nombreFormateado = nombreFinal.split(" ")
                        .filter { it.isNotBlank() }
                        .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }

                    if (nuevo_cliente(
                            cedulaCliente,
                            cedula_tendero,
                            nombreFormateado,
                            telefonoLimpio
                        )
                    ) {
                        model.addMensajeSistema(modelo("El cliente $nombreFormateado con la cédula $cedulaCliente y número $telefonoLimpio registrado con éxito."))
                        procesoActivo = "ninguno"
                        estado = "ninguno"
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

    private suspend fun buscarCliente(
        cedula: String?,
        cedula_tendero: String?
    ): Response<cliente1> {
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
        if (estado == "abierto") {
            return true
        }
        return false
    }

    suspend fun procesarCredito(texto: String) {
        val prefs = getSharedPreferences("SesionTendero", MODE_PRIVATE)
        val cedula_tendero = prefs.getString("cedula", null) ?: return
        val textoLimpio = texto.replace(Regex("""(\d)[.,](\d{3})\b"""), "$1$2")
        val textoMinuscula = textoLimpio.lowercase()
        val palabras = textoMinuscula.split(Regex("""[\s:]+"""))

        if (palabras.contains("fin")) {
            estado = "ninguno"
            procesoActivo = "niguno"
            model.addMensajeSistema(modelo("El crédito ha finalizado correctamente."))
            return
        }

        if (estado == "ninguno") {
            model.addMensajeSistema(modelo("Por favor dicte el número de cédula del cliente."))
            estado = "cedula"
            return
        }

        //---------------CREDITO---------------------------------------------------------------------------------
        if (estado == "cedula") {

            val textoSinEspacios = textoMinuscula.replace(" ", "")
            val cedulaRegex = Regex("\\d{6,}")
            val cedula = cedulaRegex.find(textoSinEspacios)?.value
            Log.i("cedula", cedulaCliente.toString())

            if (cedula != null) {
                if (cedula.length > 10 || cedula.length < 6) {
                    Log.i("primer if", "entre a estado cedula")
                    model.addMensajeSistema(modelo("El número de cédula no puede tener mas de 10 digitos, ni menos de 6. Por favor dicte de nuevo."))
                    return
                }

                if (cedula == cedula_tendero) {
                    Log.i("segundo if", "entre a estado cedula")
                    model.addMensajeSistema(modelo("La cédula del cliente no puede ser igual a la del tendero. Por favor dicte de nuevo."))
                    return
                }

                val respuesta = buscarCliente(cedula, cedula_tendero)
                if (respuesta.body()?.nombre == null) {
                    Log.i("tercer if", "entre a estado cedula")
                    Log.i("Entre1", "No encontre cedula y devuelvo null")
                    model.addMensajeSistema(modelo("El cliente no está registrado. ¿Le gustaría registrarlo?."))
                    estado = "decision"
                    cedulaCliente = cedula

                } else {
                    Log.i("entre", "Encontre cedula y devuelvo mensajes")
                    model.addMensajeSistema(modelo("Cliente encontrado: ${respuesta.body()?.nombre}.\nSaldo total: $${respuesta.body()?.saldo}.\nDicte el producto que vendío a crédito."))
                    estado = "pedir_productos"
                    cedulaCliente = cedula
                }
                return
            } else {
                model.addMensajeSistema(modelo("No pude entender. Por favor dicte de nuevo"))
                return
            }
        }

        if (estado == "decision") {
            val si = palabras.any { diccionario["si"]?.contains(it) == true }

            Log.i("si", si.toString())
            if (si) {
                model.addMensajeSistema(modelo("Dicte el nombre del cliente y el número télefonico."))
                estado = "cliente_nuevo"
                return
            }

            if (textoMinuscula == "no") {
                procesoActivo = "ninguno"
                estado = "ninguno"
                model.addMensajeSistema(modelo("Ok, saliendo de crédito"))
            } else {
                model.addMensajeSistema(modelo("Lo siento, no pude entenderte, por favor dicta de nuevo"))
            }
        }

        if (estado == "cliente_nuevo") {
            val regexTelefono = Regex("3[\\d\\s]{9,}")
            val telefono = regexTelefono.find(textoMinuscula)

            if (telefono != null) {
                val nombreFinal = textoMinuscula.substring(0, telefono.range.first).trim()
                val telefonoLimpio = telefono.value.replace(" ", "").trim()

                if (nombreFinal.length >= 3 && nombreFinal.length <= 40) {
                    val nombreFormateado = nombreFinal.split(" ")
                        .filter { it.isNotBlank() }
                        .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }

                    if (nuevo_cliente(
                            cedulaCliente,
                            cedula_tendero,
                            nombreFormateado,
                            telefonoLimpio
                        )
                    ) {
                        model.addMensajeSistema(modelo("El cliente $nombreFormateado con la cédula $cedulaCliente y número $telefonoLimpio ha sido registrado con éxito. \nDicte el producto que vendío a crédito."))
                        estado = "pedir_productos"
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

        if (estado == "pedir_productos") {
            var operacionVenta = OperacionVenta()
            val fin_credito = diccionario["fin credito"]?.any { frase ->
                textoMinuscula.contains(frase)
            }
            if (fin_credito == true) {
                estado = "ninguno"
                procesoActivo = "ninguno"
                model.addMensajeSistema(
                    modelo(
                        operacionVenta.procesarListaProductos(textoLimpio, true, cedulaCliente!!, cedulaGlobal,)))
            } else {
                val textoLimpio =
                    mensaje.replace(Regex("""(\d)[.,](\d{3})\b"""), "$1$2").lowercase()
                model.addMensajeSistema(
                    modelo(
                        operacionVenta.procesarListaProductos(textoLimpio, false, cedulaCliente!!, cedulaGlobal)))
            }
        }
        if (palabras.any { diccionario["cerrar"]?.contains(it) == true }) {
            cerrarTienda()
        }
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
        val palabras = mensaje.lowercase().split(Regex("""[\s:]+"""))

        for (palabra in palabras) {
            val montoDetectado = calcularMonto(palabra)

            if (montoDetectado != null) {
                try {
                    val service = ConexionServiceTienda.create()
                    val envioBase = ModeloBase(cedula ?: "", montoDetectado)
                    val respuesta = withContext(Dispatchers.IO) { service.addBase(envioBase) }

                    if (respuesta.isSuccessful) {
                        estadoBase.edit().putBoolean("base", true).apply()

                        withContext(Dispatchers.Main) {
                            model.addMensajeSistema(modelo("Se ha registrado una Base inicial de $montoDetectado"))
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            model.addMensajeSistema(modelo("Parece que ha ocurrido un error, por favor vuelve a intentarlo"))
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        model.addMensajeSistema(modelo("No se pudo conectar al servidor. Verifica tu red."))
                    }
                }
                break
            }
        }
    }

    private fun calcularMonto(palabra: String): Int? {
        return when {
            palabra.matches(Regex("""\$?\d+k""", RegexOption.IGNORE_CASE)) -> {
                val baseStr = palabra.lowercase().removePrefix("$").removeSuffix("k")
                baseStr.toIntOrNull()?.times(1000)
            }

            palabra.matches(Regex("""\$?\d{1,3}([.,]\d{3})+""")) -> {
                palabra.replace("$", "").replace(".", "").replace(",", "").toIntOrNull()
            }

            palabra.matches(Regex("""\$?\d+""")) -> { palabra.removePrefix("$").toIntOrNull() }
            else -> null
        }
    }
}