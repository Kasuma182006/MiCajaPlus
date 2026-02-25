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
import android.widget.PopupMenu
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
import com.example.micaja.models.modelo
import com.example.micaja.models.modeloOperaciones
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
var credito: Boolean = false
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
        binding.btnMenu.setOnClickListener { mostrarMenuDesplegable(it) }
    }

    private fun mostrarMenuDesplegable(view: View) {
        val popup = PopupMenu(this, view)
        popup.menuInflater.inflate(R.menu.menu_principal, popup.menu)

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_CerrarSeccion -> {
                    Toast.makeText(this, "Cerrando sesión...", Toast.LENGTH_SHORT).show()
                    SesionManager.cerrarSesion(this)
                    true
                }

                R.id.action_Comandos -> {
                    val dialogo = dialogo_comandos()
                    dialogo.show(supportFragmentManager, "DialogoComandos")
                    true
                }
                else -> false
            }
        }
        popup.show()

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
            val texto = modelo(mensaje)
            val tienda = estadoTienda.getBoolean("abierta",false)
            val base = estadoBase.getBoolean("base",false)
            binding.messageInput.setText("")
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

    /* Aquí se encuentran las funciones para el reconocimiento de valores y palabras*/


    fun procesarCompra(texto: String) {
        val prefs = getSharedPreferences("SesionTendero", MODE_PRIVATE)
        val cedula = prefs.getString("cedula", null)
        var cerrada = 0
        var monto = 0
        var tipoVenta = ""
        var respuestaSistema = ""

        val palabras = texto.lowercase().split(Regex("""[\s:]+""") )

        for (palabra in palabras){

            if (diccionario["venta"]?.contains(palabra)==true){
                Log.i("entrando", "Entro al primer if")
                val mensaje = modelo("¿Que productos vendío?.")
                for (p in palabras) {
                    Log.i("ciclo", p)
                    if(diccionario["credito"]?.contains(p)==true){
                        credito = true
                        model.addMensajeSistema(mensaje)
                        Log.i("entrando", "Entro a credito")
                    }else if (diccionario["efectivo"]?.contains(p)==true){
                        model.addMensajeSistema(mensaje)
                        Log.i("entrando", "Entro a efectivo")
                    }

                }

//                    if ((diccionario["credito"]?.contains(palabra)?:false)){
//                        tipoVenta = "credito"
//                        binding.messageInput.clearFocus()
//                        supportFragmentManager
//                            .beginTransaction()
//                            .replace(R.id.fragmento_opciones, Opciones())
//                            .addToBackStack(null)
//                            .commit()
//                    }
                }
            val precio = calcularMonto(palabra)

            if (precio !=  null && precio>=100){
                monto += precio
            }

            if ((diccionario["cerrar"]?.contains(palabra)?:false)){
                Log.i("cerrar", "La tienda cerro.")
                cerrarTienda()
                cerrada = 1

            }

        }
        //
        if (tipoVenta != "credito" && monto != 0) {
            tipoVenta = tipoVenta(palabras)

            when (tipoVenta) {
//                "venta" -> {montoVentas += monto
//                    CoroutineScope(Dispatchers.IO).launch{
//
//                        val service = ConexionServiceTienda.create()
//                        val venta = modeloOperaciones(tipoVenta, monto, cedula!!,mensaje)
//                        val respuesta = service.addOperacion(venta)
//                    }
//                }
                "gasto" -> {montoGastos += monto
                    CoroutineScope(Dispatchers.IO).launch{

                        val service = ConexionServiceTienda.create()
                        val gasto = modeloOperaciones(tipoVenta,monto, cedula!!, mensaje)
                        val respuesta = service.addOperacion(gasto)
                    }
                }
                "compra" -> {montoCostos += monto
                    CoroutineScope(Dispatchers.IO).launch{

                        val service = ConexionServiceTienda.create()
                        val costo = modeloOperaciones(tipoVenta,monto, cedula!!, mensaje)
                        val respuesta = service.addOperacion(costo)
                    }
                }
                "sin coincidencias"-> {Log.d(TAG, "No se han encontrado coincidencias")}
            }
        }

//        respuestaSistema = when (tipoVenta) {
//            "venta" -> "Se ha registrado una venta por $monto"
//            "gasto" -> "Se ha registrado un gasto por $monto"
//            "compra" -> "Se ha registrado un costo por $monto"
//            "credito"-> "Abriendo la interfaz de credito..."
//            else -> "No hay coincidencias, por favor revisa tu mensaje"
//        }
//
//        if (cerrada == 0){
//            val sistema = modelo(respuestaSistema)
//            model.addMensajeSistema(sistema)
//        }
//        else {
//            respuestaSistema = when (tipoVenta) {
//                "venta" -> "La última venta se ha hecho por $monto y ahora la tienda está cerrada"
//                "gasto" -> "El ultimo gasto se ha hecho por $monto y ahora la tienda está cerrada"
//                "compra" -> "La ultima compra se ha hecho por $monto y ahora la tienda está cerrada"
//
//                else -> "la tienda se ha cerrado correctamente"
//            }
//            val sistema = modelo(respuestaSistema)
//
//            cerrarTienda()
//        }
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