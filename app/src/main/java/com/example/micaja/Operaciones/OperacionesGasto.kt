package com.example.micaja.Operaciones

import android.util.Log
import com.example.micaja.ConexionService.ConexionServiceTienda
import com.example.micaja.models.gastoDetectado
import com.example.micaja.models.modelo
import java.text.Normalizer

class OperacionGasto {
    var inicio = false

    companion object {
        val limpiarN = listOf(
            "gaste", "gasto", "pague", "pago", "compre", "compra",
            "un", "una", "de", "del", "por", "el", "la", "los", "las", "en", "me", "salio"
        )

        val numerosMap = mapOf(
            "un" to "1", "uno" to "1", "dos" to "2", "tres" to "3",
            "cuatro" to "4", "cinco" to "5", "seis" to "6", "sixpack" to "6",
            "siete" to "7", "ocho" to "8", "nueve" to "9",
            "diez" to "10", "docena" to "12", "panal" to "30"
        )
    }

    private fun normalizarTexto(texto: String): String {
        val DictarGasto = Normalizer.normalize(texto.lowercase(), Normalizer.Form.NFD)
        val sinTildes = Regex("\\p{InCombiningDiacriticalMarks}+").replace(DictarGasto, "")
        var resultado = sinTildes

        numerosMap.forEach { (palabra, numero) ->
            resultado = resultado.replace(Regex("\\b$palabra\\b"), numero)
        }
        return resultado
    }

    fun iniciarFlujoGasto(onMensaje: (modelo) -> Unit) {
        this.inicio = true
        onMensaje(modelo("¡Gasto registrado! Dicta en lo que has gastado o di 'fin'."))
    }

    suspend fun procesarRespuesta(
        texto: String,
        onMensaje: (modelo) -> Unit,
        idTendero: String
    ): Boolean {
        if (!inicio) return false // Si no estamos en un flujo de gasto, lo ignoramos

        val textoLimpio = normalizarTexto(texto)

        if (textoLimpio.contains("fin")) {
            this.inicio = false
            onMensaje(modelo("Gasto finalizado correctamente."))
            return true // Retornamos true porque sí procesamos el mensaje
        }

        // Si no es "fin", procesamos el gasto normalmente
        val respuesta = procesarGasto(textoLimpio, idTendero)
        onMensaje(modelo(respuesta))
        return true
    }

    suspend fun procesarGasto(texto: String, idTendero: String): String {
        val textoLimpio = normalizarTexto(texto)

        if (textoLimpio.contains("fin")) {
            this.inicio = false
            return "Gasto finalizado correctamente."
        }
        val datos = extraerDatosGasto(textoLimpio)

        if (datos != null) {
            val (justificacion, precio) = datos
            Log.i("datos_procesados", "Justificación: $justificacion, Precio: $precio")

            try {
                val conexion = ConexionServiceTienda.create()
                val modeloGasto = gastoDetectado(idTendero, justificacion, precio)
                val respuestaGasto = conexion.gastoDetectadoss(modeloGasto)

                return if (respuestaGasto.isSuccessful) {
                    "• Gasto de $precio en '$justificacion' registrado. ¿Algún otro gasto por registrar? (o di 'fin')"
                } else {
                    "No se pudo registrar el gasto, por favor reintente nuevamente."
                }

            } catch (e: Exception) {
                return "Fuera de conexión, inténtalo de nuevo."
            }
        }
        return "No entendí el gasto, intenta decir algo como: '75.000 en el recibo de la luz' o 'pagué $20000 en pasajes'"
    }

    private fun extraerDatosGasto(segmento: String): Pair<String, Int>? {
        var texto = segmento.trim()

        // 1. Limpiamos las palabras clave (stopwords)
        for (palabra in limpiarN) {
            texto = texto.replace(Regex("(?i)\\b$palabra\\b"), "").trim()
        }

        // 2. Normalizamos los formatos de moneda
        // Elimina el símbolo '$' y los puntos/comas que actúan como separadores de miles (ej: 180.000 -> 180000)
        texto = texto.replace("$", "")
            .replace(Regex("(?<=\\d)[.,](?=\\d{3})"), "")

        // 3. Extraemos la lista de números (solo de 3 o más dígitos para no tomar cantidades como "1" repuesto)
        val cifrasEncontradas = Regex("""\d{3,}""").findAll(texto).map { it.value }.toList()

        // 4. Buscamos el valor numérico más alto para el precio
        val precio = cifrasEncontradas.mapNotNull { it.toIntOrNull() }.maxOrNull()

        if (precio != null) {
            // Buscamos el string exacto del precio encontrado y lo quitamos del texto
            val precioString = precio.toString()
            texto = texto.replaceFirst(precioString, "").trim()
        } else {
            return null
        }

        // 5. Limpiamos espacios dobles residuales
        val justificacion = texto
            .replace(Regex("""\s+"""), " ")
            .trim()

        return if (justificacion.isNotEmpty()) {
            Pair(justificacion, precio)
        } else {
            null
        }
    }
}