package com.example.micaja.Operaciones

import android.util.Log
import com.example.micaja.ConexionService.ConexionServiceTienda
import com.example.micaja.models.OperacionesInventario
import com.example.micaja.models.ventaDetectada
import java.text.Normalizer

class OperacionVenta() {
    var inicio = false

    companion object {
        val unidades = mapOf(
            "gramo" to listOf("gramo", "gramos", "g", "gr"),
            "libra" to listOf("libra", "libras", "lb"),
            "kilogramo" to listOf("kilo", "kilogramos", "kg", "kilos"),
            "onza" to listOf("onza", "onzas"),
            "Litro" to listOf("litro", "litron", "l", "litros"),
            "militro" to listOf("ml", "mililitros"),
            "garrafa" to listOf("garrafa", "gal"),
            "bolsa" to listOf("bolsa", "bolsita", "chuspa"),
            "caja" to listOf("caja", "cajetilla"),
            "paquete" to listOf("paquete", "paca", "sixpack", "sobre"),
            "envase" to listOf("envase", "frasco", "botella", "tubo", "spray", "rollo"),
            "unidad" to listOf("unidad", "unidades", "barra", "capsula", "tableta", "docena", "panal", "atado", "hojas", "carta"),
            "recipiente" to listOf("cubeta", "canasta", "cubo", "vaso", "vasito", "lata", "laton"),
            "pequeño" to listOf("pequeña", "pequeño", "mini"),
            "mediano" to listOf("mediana", "mediano", "media"),
            "grande" to listOf("grande", "jumbo")
        )

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
            return if (idCliente.isNotEmpty() ) {
                "¡Listo! El crédito ha finalizado correctamente."
            } else { "Venta de contado finalizada correctamente." }
        }

        val datos = extraerDatosProducto(textoLimpio)

        if (datos != null) {
            val (nombre, pres, cant) = datos
            Log.i("datos_procesados", "Nombre: $nombre, Pres: $pres, Cant: $cant")

            try {
                val conexion = ConexionServiceTienda.create()
                val tipoVenta = if (idCliente.isNotEmpty()) "credito" else "efectivo"

                if (tipoVenta == "efectivo") {
                    val modeloOperacion = OperacionesInventario(idTendero, nombre, pres, cant, "descontar")
                    val respuestaOperacion = conexion.operacionesInventario(modeloOperacion)

                    if (respuestaOperacion.isSuccessful) {
                        val modeloVenta = ventaDetectada(idTendero, idCliente, texto, "efectivo", nombre, cant, pres)
                        val respuestaVenta = conexion.ventaDetectada(modeloVenta)

                        if (respuestaVenta.isSuccessful){
                            return  "• $cant de  $nombre registrado. ¿Algo más? (o di 'fin')"
                        } else {
                            return "La cantidad del producto ($nombre) es insuficiente"
                        }
                    } else {
                        return when(respuestaOperacion.code()){
                            400 ->  "La cantidad del producto $nombre es insuficiente"
                            else -> "Producto: $nombre, No encontrado en el inventario"
                        }
                    }

                } else if (tipoVenta == "credito") {
                    val cuerpo = ventaDetectada(idTendero, idCliente, texto, "credito", nombre, cant, pres)
                    val respuesta = conexion.registrarCredito(cuerpo)
                    return if (respuesta.isSuccessful) respuesta.body()!! else "Error de conexion."
                }
            } catch (e: Exception) { return "Fuera de conexion intentalo de nuevo" }
        }

        return "Estructura incompleta. Debe contener: [cantidad] [nombre] [presentación]. Ejemplo: '2 arroz diana 2 libras'"
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


        var unidadDetectada: String? = null
        var encontrado = false

        for ((keyOficial, sinonimos) in unidades) {
            for (s in sinonimos.sortedByDescending { it.length }) {
                val regexUnidad = Regex("""\b(\d+)?\s*$s\b""")
                val coincidencia = regexUnidad.find(texto)

                if (coincidencia != null) {
                    val numeroEncontrado = coincidencia.groups[1]?.value
                    unidadDetectada = if (numeroEncontrado != null) "$numeroEncontrado $keyOficial" else keyOficial
                    texto = texto.replaceFirst(coincidencia.value, "").trim()
                    encontrado = true
                    break
                }
            }
            if (encontrado) break
        }

        if (!encontrado) return null

        var nombreProducto = texto
            .replace(Regex("(?i)\\b[s]\\b"), "")
            .replace(Regex("""\s+"""), " ")
            .lowercase()
            .trim()

        nombreProducto = nombreProducto.removePrefix("de ").removeSuffix(" de").trim()

        return if (nombreProducto.isNotEmpty()) {
            Triple(nombreProducto, unidadDetectada!!, cantidad)
        } else { null }
    }
}