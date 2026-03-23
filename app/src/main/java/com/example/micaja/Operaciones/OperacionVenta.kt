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
        val unidades = listOf(
            "bolsa", "bolsita", "caja", "cajetilla", "canasta", "chuspa",
            "barra", "capsula", "cubeta", "tableta",
            "docena", "gramo", "libra", "libras", "kilo", "kilogramos", "litro", "litron", "onza",
            "envase", "frasco", "plastico", "paquete",
            "pequeña", "pequeño", "media", "mediana", "mediano", "grande",
            "garrafa", "lata", "laton", "paca", "sixpack",
            "panal", "sobre", "rollo", "tubo", "unidad", "vasito", "vaso", "botella",
            "kg","l","ml","sobre","tubo","spray","carta","hojas","g","cubo","lata","jumbo","mini","atado","unidades"
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
                val tipoVenta =
                    if (idCliente.isNotEmpty()) "credito" else "efectivo"
                Log.i("tipoCliente", tipoVenta)
                Log.i("idcliente", idCliente)

            if(tipoVenta == "efectivo") {
                val modeloCantidad = cantidadIn(idTendero, cant, pres, nombre)
                val respuestaCantidad = conexion.cantidadProducto(modeloCantidad)

                return if (respuestaCantidad.isSuccessful) {
                    Log.i("cantidad suficiente", "hay cantidad")
                    val modeloOperacion =
                        OperacionesInventario(idTendero, nombre, pres, cant, "descontar")
                    val respuestaOperacion = conexion.operacionesInventario(modeloOperacion)

                    return if (respuestaOperacion.isSuccessful) {
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
                        Log.i("respuestaOperacion", respuestaVenta.toString())

                        return if (respuestaVenta.isSuccessful) {
                            "• $cant  $nombre de $pres registrado. ¿Algo más? (o di 'fin')"

                        } else { "No pude registrar la venta" }

                    } else { "No se pudo descontar la cantidad" }

                } else { "No pude registrar la cantidad" }
            } else if (tipoVenta == "credito") {
                val cuerpo =
                    ventaDetectada(idTendero, idCliente, texto, "credito", nombre, cant, pres)
                val respuesta = conexion.registrarCredito(cuerpo)

                if (respuesta.isSuccessful) {
                    return respuesta.body()!!
                } else { return "Error de conexion." }
            }} catch (e: Exception) { return  "Fuera de conexion intentalo de nuevo" }
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
        for (u in unidades) {
            val  presentacion = Regex("""\b\d+\s*$u\b""")
            val concidencia = presentacion.find(texto)

            if (concidencia != null){
                unidadDetectada = concidencia.value.trim()
                texto = texto.replaceFirst(concidencia.value, "").trim()
                 break
            } else {
                val unidad = Regex("""\b$u\b""")
                val coincidenciaUnidad = unidad.find(texto)
                if (coincidenciaUnidad != null) {
                    unidadDetectada = coincidenciaUnidad.value.trim()
                    texto = texto.replace(coincidenciaUnidad.value, "").trim()
                    break
                }
            }
        }

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