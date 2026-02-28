package com.example.micaja

import android.content.ContentValues.TAG
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.micaja.Adapter.Adapter
import com.example.micaja.ConexionService.ConexionServiceTienda
import com.example.micaja.databinding.ActivityChatTiendaBinding
import com.example.micaja.models.ModeloBase
import com.example.micaja.models.crear_venta
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
import androidx.core.view.WindowInsetsControllerCompat
import com.example.micaja.utils.TimeUtils


var montoVentas = 0
var montoGastos = 0
var montoCostos = 0
var baseInicial = 0
var credito: Boolean = false
var montoCredito = 0

var hora = TimeUtils.horaActual()


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
    "agregar cliente" to listOf("agregar nombre", "añadir cliente", "cliente nuevo", "nuevo cliente")
)

class chat_Tienda : AppCompatActivity() {
    lateinit var mensaje: String
    private lateinit var binding: ActivityChatTiendaBinding
    private val RQ_SPEECH_REC = 102

    var dataset = mutableListOf<modelo>()
    var sistemaData = mutableListOf<modelo>()
    lateinit var Adapter : Adapter
    lateinit var estadoTienda: SharedPreferences
    lateinit var estadoBase: SharedPreferences



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
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val preferencia = getSharedPreferences("SesionTendero", MODE_PRIVATE)

        val cedula = preferencia.getString("cedula", null)
//        CoroutineScope(Dispatchers.IO).launch { ConexionServiceTienda.llamarInventario(cedula.toString()) }
        //Esto ↓ lo agrego julio(yo) para evitar el crasheo al ejecutar la app sin el backend, habiliten ↑ esta linea y comenten
     // esta ↓
        // Protege la llamada inicial para que no cierre la app si falla el servidor
        CoroutineScope(Dispatchers.IO).launch {
            try {
                ConexionServiceTienda.llamarInventario(cedula.toString())
            } catch (e: Exception) {
                Log.e("ErrorRed", "No se pudo conectar al inventario: ${e.message}")
                // Opcional: Avisar al usuario en el hilo principal
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@chat_Tienda, "Error de conexión con el servidor", Toast.LENGTH_SHORT).show()
                }
            }
        }



        estadoTienda =  getSharedPreferences("EstadoTienda",MODE_PRIVATE)
        estadoBase = getSharedPreferences("EstadoBase",MODE_PRIVATE)

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
        // PopupMenu reemplazado por MenuBottomSheet
        // Comandos y Cerrar sesión (mas Balance, Editar Cliente, Editar Producto)
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
            Toast.makeText(this, "Ha ocurrido un error durante el dictado", Toast.LENGTH_SHORT).show()
        } else {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PROMPT, "" +
                        "No se escuchó. Intentalo nuevamente")
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

        model.mensajesSistema.observe(this,Observer {newName ->
            Adapter.sistemaData = newName
            Adapter.notifyDataSetChanged()
            binding.recyclerMensajes.scrollToPosition(Adapter.itemCount - 1)
        })
    }

    fun evento() {
        binding.sendBtn.setOnClickListener {
            mensaje = binding.messageInput.text.toString()
            val tienda = estadoTienda.getBoolean("abierta",false)
            val base = estadoBase.getBoolean("base",false)
            binding.messageInput.setText("")

            // Ocultar el teclado al presionar enviar
            WindowInsetsControllerCompat(window, binding.messageInput)
                .hide(WindowInsetsCompat.Type.ime())

            //Llama a TimeUtils para obtener la hora — la lógica vive en TimeUtils.kt

            val texto = modelo(mensaje, hora)   //agrega hora al mensaje del usuario
            model.addMensaje(texto)


            if (tienda){
                if (base) {
                    procesarCompra(mensaje)
                    Log.d(TAG, "El valor de la venta es ${montoVentas}")
                    Log.d(TAG, "El valor de los gastos es ${montoGastos}")
                    Log.d(TAG, "El valor de los costos es ${montoCostos}")
                }
                else {
                    CoroutineScope(Dispatchers.IO).launch {
                        baseInicial(mensaje)
                        Log.d(TAG, "El valor de la base inicial es de ${baseInicial}")
                    }
                }
            }
            else {
                val estado = tienda(mensaje)
                if (estado){
                    Log.d(TAG, "La tienda está Abierta")
                    val mensaje = modelo ("Tienda Abierta ¿Cúal es la base del día de hoy?")
                    model.addMensajeSistema(mensaje)
                }
                else {
                    Log.d(TAG, "La tienda está Cerrada")
                    val mensajeSistema = modelo ("Tienda Cerrada. Reintente nuevamente.")
                    model.addMensajeSistema(mensajeSistema)
                }
            }
        }
    }


    private fun tienda(mensaje: String):Boolean {
        val palabras = mensaje.lowercase().split(Regex("""[\s,.:]+""") )
        var estado = ""
        for (palabra in palabras){

            if ((diccionario["abrir"]?.contains(palabra)?:false)){
                val editor = estadoTienda.edit()
                editor.putBoolean("abierta", true)
                editor.apply()
                estado = "abierto"
            }
        }

        if (estado == "abierto") {
            return true
        }

        return  false
    }
    val unidades = listOf("libra", "kilo", "unidad", "paquete", "bolsa", "paca", "litro", "pequeña", "grande", "mediana" )


    /* Aquí se encuentran las funciones para el reconocimiento de valores y palabras*/

    fun procesarCompra(texto: String) {
        val prefs = getSharedPreferences("SesionTendero", MODE_PRIVATE)
        val cedula = prefs.getString("cedula", null) ?: return

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

                    Log.d("DETECCION_VENTA", "REGISTRADO -> PROD: $nombre | PRES: $pres | PRECIO: $precio")


//                    CoroutineScope(Dispatchers.IO).launch {
//                        try {
//                            val service = ConexionServiceTienda.create()
//                            val op = crear_venta(cedula, "Venta", sumaTotalVenta, mensaje)
//                            service.addOperacion(op)
//                            withContext(Dispatchers.Main) {
//                                montoVentas += precio
//                            }
//                        } catch (e: Exception) {
//                            Log.e("API", "Error al guardar: ${e.message}")
//                        }
//                    }
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
        val ruido = listOf("vendi", "vendí", "vende", "venta", "un", "una", "de", "a", "por", "el", "la", "total", "precio", "también")
        var nombreLimpio = s
        for (r in ruido) {
            nombreLimpio = nombreLimpio.replace(Regex("\\b$r\\b", RegexOption.IGNORE_CASE), "")
        }

        nombreLimpio = nombreLimpio.trim().replace(Regex("\\s+"), " ")

        return if (nombreLimpio.isNotEmpty()) {
            Triple(nombreLimpio.replaceFirstChar { it.uppercase() }, unidadDetectada, precioFinal)
        } else null
    }

    private fun cerrarTienda() {
        val editor = estadoTienda.edit()
        editor.putBoolean("abierta", false)
        editor.apply()
        Log.d(TAG, "La tienda se acaba de cerrar")
        estadoBase.edit().putBoolean("base",false).apply()
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
            val mensajeSistema = modelo("La base no se ha podido identificar, por favor vuelve a intentarlo")
            withContext(Dispatchers.Main) {
                model.addMensajeSistema(mensajeSistema)
            }

        }

    }
}



fun tipoVenta(palabras: List<String>):String {
    for (palabra in palabras){
        if (diccionario["venta"]?.contains(palabra)?:false){
            return "venta"
        }
        else if (diccionario["gasto"]?.contains(palabra)?:false){
            return "gasto"
        }

        else if (diccionario["costo"]?.contains(palabra)?:false){
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