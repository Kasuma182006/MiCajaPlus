package com.example.micaja.Operaciones


import android.util.Log
import com.example.micaja.ConexionService.ConexionServiceTienda
import com.example.micaja.models.gastoDetectado
import java.text.Normalizer

class OperacionGasto {

    companion object {
        val limpiarRuido = listOf(
            "gaste", "gasto", "pagamos", "pague", "pago", "compre", "compro",
            "un", "una", "el", "la", "de", "del", "por", "total"
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

    suspend fun procesarGasto(texto: String, idTendero: String): String {
        val textoLimpio = normalizarTexto(texto)
        val datos = extraerDatosGasto(textoLimpio)

        if (datos != null) {
            val (justificacion, precio) = datos

            return try {
                val conexion = ConexionServiceTienda.create()
                val modeloGasto = gastoDetectado(idTendero, justificacion, precio)
                val respuesta = conexion.gastosDetectadoss(modeloGasto)

                if (respuesta.isSuccessful) {
                    "¡Gasto registrado! $${precio} en $justificacion."
                } else {
                    "No pude guardar el gasto. Error en el servidor."
                }
            } catch (e: Exception) {
                Log.e("ERROR_GASTO", "Error: ${e.message}")
                "Sin conexión. Inténtalo de nuevo."
            }
        }
        return "No entendí el gasto. Intenta algo como: 'Gasto en bolsas 5000' o 'pagué transporte 10000'"
    }

    private fun extraerDatosGasto(segmento: String): Pair<String, Int>? {
        var texto = segmento.trim()
        val cifras = Regex("""\b\d+\b""").findAll(texto).map { it.value }.toList()
        if (cifras.isEmpty()) return null

        var precioFinal: Int? = null
        var textoPrecioDetectado = ""

        val regexDinero = Regex("""(?:por|de|\$)\s*(\d+)|(\d+)\s*(?:pesos|lucas)""")
        val matchDinero = regexDinero.find(texto)

        if (matchDinero != null) {
            textoPrecioDetectado = matchDinero.groupValues[1].ifEmpty { matchDinero.groupValues[2] }
            precioFinal = textoPrecioDetectado.toIntOrNull()
        }

        if (precioFinal == null) {
            val numeros = cifras.mapNotNull { it.toIntOrNull() }
            val posiblesPrecios = numeros.filter { it >= 100 }

            precioFinal = if (posiblesPrecios.isNotEmpty()) {
                posiblesPrecios.maxOrNull()
            } else {
                numeros.maxOrNull()
            }
            textoPrecioDetectado = precioFinal.toString()
        }

        if (precioFinal == null) return null
        texto = texto.replaceFirst(textoPrecioDetectado, "").replace("$", "").replace("pesos", "")
            .replace("lucas", "").trim()

        for (ruido in limpiarRuido) {
            texto = texto.replace(Regex("(?i)\\b$ruido\\b"), " ")
        }

        val justificacionFinal = texto.replace(Regex("""\s+"""), " ").trim()
        return if (justificacionFinal.isNotEmpty()) {
            Pair(justificacionFinal.replaceFirstChar { it.uppercase() }, precioFinal)
        } else {
            Pair("Gasto general", precioFinal)
        }
    }
}