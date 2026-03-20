package com.example.micaja.Operaciones

import android.util.Log
import com.example.micaja.ConexionService.ConexionServiceTienda
import com.example.micaja.models.OperacionesInventario
import com.example.micaja.models.cantidadIn
import com.example.micaja.models.ventaDetectada
import java.text.Normalizer

class OperacionVenta() {
    var inicio = false

    companion object {

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
//        val unidades = listOf(
//            "bolsa", "bolsita", "caja", "cajetilla", "canasta", "chuspa",
//            "barra", "capsula", "cubeta", "tableta",
//            "docena", "gramo", "libra", "libras", "kilo", "kilogramos", "litro", "litron", "onza",
//            "envase", "frasco", "plastico", "paquete",
//            "pequeña", "pequeño", "media", "mediana", "mediano", "grande",
//            "garrafa", "lata", "laton", "paca", "sixpack",
//            "panal", "sobre", "rollo", "tubo", "unidad", "vasito", "vaso", "botella",
//            "kg","l","ml","sobre","tubo","spray","carta","hojas","g","cubo","lata","jumbo","mini","atado","unidades"
//        )

        val limpiarN = listOf(
            "vendi", "vende", "venta", "un", "una", "de", "del",
            "por", "el", "la", "los", "las", "me", "compraron", "salio", "pago","s","S", "es"
        )

        val numerosMap = mapOf(
            "un" to "1", "uno" to "1", "dos" to "2", "tres" to "3",
            "cuatro" to "4", "cinco" to "5", "seis" to "6",
            "siete" to "7", "ocho" to "8", "nueve" to "9", "diez" to "10"
        )
    }

    private fun normalizarTexto(texto: String): String {
        val temp = Normalizer.normalize(texto.lowercase(), Normalizer.Form.NFD)
        val sinTildes = Regex("\\p{InCombiningDiacriticalMarks}+").replace(temp, "")
        var resultado = sinTildes

        numerosMap.forEach { (palabra, numero) ->
            resultado = resultado.replace(Regex("\\b$palabra\\b"), numero)
        }
        return resultado
    }

    suspend fun procesarListaProductos(texto: String, fin_credito: Boolean, idCliente: String = "", idTendero: String): String {
        val textoLimpio = normalizarTexto(texto)
        if (textoLimpio.contains("fin") || fin_credito) {
            this.inicio = false
            return if (idCliente.isNotEmpty() && idCliente != idTendero) {
                "¡Listo! El crédito se ha registrado con éxito para el cliente."
            } else { "Venta de contado finalizada correctamente." }
        }
        val datos = extraerDatosProducto(textoLimpio)

        if (datos != null) {
            val (nombre, pres, cant) = datos
            Log.i("datos_procesados", "Nombre: $nombre, Pres: $pres, Cant: $cant")

            try {
                val conexion = ConexionServiceTienda.create()
                val tipoVenta =
                    if (idCliente.isNotEmpty()) "credito" else "efectivo"

                if (tipoVenta == "efectivo") {
                    Log.i("cantidad suficiente", "hay cantidad")
                    val modeloOperacion =
                        OperacionesInventario(idTendero, nombre, pres, cant, "descontar")
                    val respuestaOperacion = conexion.operacionesInventario(modeloOperacion)

                    if (respuestaOperacion.isSuccessful) {
                        val modeloVenta = ventaDetectada(
                            idTendero,
                            idCliente,
                            texto,
                            "efectivo",
                            nombre,
                            cant,
                            pres
                        )
                        val respuestaVenta = conexion.ventaDetectada(modeloVenta)

                        if (respuestaVenta.isSuccessful){
                            return  "• $cant  $nombre de $pres registrado. ¿Algo más? (o di 'fin')"
                        }else{
                            return "La cantidad del producto ($nombre) es insuficiente"
                        }
                        Log.i("respuestaOperacion", respuestaVenta.toString())

                    } else {
                        return when(respuestaOperacion.code()){
                            400 ->  "La cantidad del producto $nombre es insuficiente"
                            else -> "Producto: $nombre, No encontrado en el inventario"

                        }
                    }

                }else if (tipoVenta == "credito") {
                    val cuerpo =
                        ventaDetectada(idTendero, idCliente, texto, "credito", nombre, cant, pres)
                    val respuesta = conexion.registrarCredito(cuerpo)

                    if (respuesta.isSuccessful) {
                        return respuesta.body()!!
                    } else {
                        return "Error de conexion."
                    }
                }
            }catch (e: Exception) { return "Fuera de conexion intentalo de nuevo" }
        }
        return "No entendí el producto, intenta decir algo como: '2 libras de arroz' o 'un aceite'"
    }

    private fun extraerDatosProducto(segmento: String): Triple<String, String, Int>? {
        var texto = segmento.trim()
        for (palabra in limpiarN) {
            texto = texto.replace(Regex("(?i)\\b$palabra\\b"), "").trim()
        }

        val cifraCabtidad = Regex("""\b\d+\b""").find(texto)
        val cantidad = cifraCabtidad?.value?.toIntOrNull() ?: 1

        if (cifraCabtidad != null) {
            texto = texto.replaceFirst(cifraCabtidad.value, "").trim()
        }

        var unidadDetectada = "unidad"
        var encontrado = false
        for ((keyOficial, sinonimos) in unidades) {
            // Buscamos los sinónimos más largos
            for (s in sinonimos.sortedByDescending { it.length }) {
                val regexPresentacion = Regex("""\b\d+\s*$s\b""")
                val coincidencia = regexPresentacion.find(texto)

                if (coincidencia != null) {
                    val numeroEncontrado = coincidencia.value.filter { it.isDigit() }
                    unidadDetectada = "$numeroEncontrado $keyOficial"
                    texto = texto.replaceFirst(coincidencia.value, "").trim()
                    encontrado = true
                    break
                }
            }
            if (encontrado) break
        }

        Log.i("presentacion detectada:", unidadDetectada)

        var nombreProducto = texto
            .replace(Regex("(?i)\\b[s]\\b"), "")
            .replace(Regex("""\s+"""), " ")
            .lowercase()
            .trim()

        nombreProducto = nombreProducto.removePrefix("de ").removeSuffix(" de").trim()
        return if (nombreProducto.isNotEmpty()) {
            Triple(nombreProducto, unidadDetectada, cantidad)
        } else { null }
    }
}