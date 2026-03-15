package com.example.micaja.Operaciones

import android.util.Log
import com.example.micaja.ConexionService.ConexionServiceTienda
import com.example.micaja.models.OperacionesInventario
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class OperacionProducto {
    var inicio = false
    var fase = 0

    var nombreExtraido = ""
    var presentacionExtraida = ""
    var cantidadExtraida = 0
    var valorVentaExtraido = 0

    private val conectores = listOf("de", "un", "una", "por", "la", "el", "con")

    private val unidades = listOf(
        "caja", "libra", "kilo", "bolsita", "unidad", "paquete",
        "bolsa", "paca", "litro", "pequeña", "grande", "sobre", "mediana"
    )


    private val mapaNumeros = mapOf(
        "uno" to 1, "una" to 1, "dos" to 2, "tres" to 3, "cuatro" to 4,
        "cinco" to 5, "seis" to 6, "siete" to 7, "ocho" to 8, "nueve" to 9,
        "diez" to 10, "doce" to 12, "quince" to 15, "veinte" to 20, "treinta" to 30
    )

    suspend fun procesarFlujo(mensaje: String, idTendero: String): String {
        val textoLimpio = mensaje.lowercase().trim()

        return when (fase) {
            0 -> {
                // PASO 1: Identificar Producto y Presentación
                extraerProductoYPresentacion(textoLimpio)
                if (nombreExtraido.isEmpty()) {
                    return "No pude reconocer el nombre del producto. Intenta decir algo como 'Arroz de libra'."
                }

                fase = 1
                "Entendido: $nombreExtraido ($presentacionExtraida). Ahora, di la cantidad del producto."
            }
            1 -> {
                val cantidad = extraerNumero(textoLimpio)
                if (cantidad != null && cantidad > 0) {
                    cantidadExtraida = cantidad
                    fase = 2
                    "¿Cuál es el valor de venta de $nombreExtraido?"
                } else {
                    "Dime una cantidad válida en números (ejemplo: '15')."
                }
            }
            2 -> {
                val precio = extraerNumero(textoLimpio)?.toInt()
                if (precio != null && precio > 0) {
                    valorVentaExtraido = precio
                    fase = 3
                    "¿Confirmas agregar $cantidadExtraida $nombreExtraido por $$valorVentaExtraido? (Si/No)"
                } else {
                    "Por favor, dime un precio de venta válido."
                }
            }
            3 -> {
                if (mensaje.lowercase().contains("si") || mensaje.lowercase().contains("sí")) {

                    try {

                        val respuesta = withContext(Dispatchers.IO) {
                            val service = ConexionServiceTienda.create()
                            val datos = OperacionesInventario(
                                idTendero, nombreExtraido, presentacionExtraida, cantidadExtraida, "agregar"
                            )
                            service.operacionesInventario(datos)
                        }

                        if (respuesta.isSuccessful) {
                            val nombreFinal = nombreExtraido
                            reiniciar()
                            inicio = false
                            "¡Listo! El producto $nombreFinal ha sido guardado exitosamente."
                        } else {
                            "El servidor respondió con un error inténtalo de nuevo."
                        }
                    } catch (e: Exception) {
                        Log.e("ErrorDelProducto", "Error: ${e.message}")
                        "Lo siento, hubo un error al guardar en el servidor inténtalo de nuevo."
                    }
                } else if (mensaje.lowercase().contains("no")) {
                    inicio = false
                    reiniciar()
                    "Registro cancelado."
                } else {
                    "Por favor responde 'Sí' o 'No'."
                }
            }
            else -> "Error."
        }
    }

    private fun extraerProductoYPresentacion(texto: String) {
        val palabras = texto.split(Regex("\\s+")).toMutableList()

        presentacionExtraida = palabras.find { unidades.contains(it) } ?: "unidad"

        val palabrasNombre = palabras.filter {
            it != presentacionExtraida && !conectores.contains(it)
        }

        nombreExtraido = palabrasNombre.joinToString(" ").trim().lowercase()
    }

    private fun extraerNumero(texto: String): Int? {
        val textoSinPuntos = texto.replace(".", "").replace(",", "")
        val soloDigitos = textoSinPuntos.filter { it.isDigit() }
        if (soloDigitos.isNotEmpty()) return soloDigitos.toIntOrNull()

        val palabras = texto.split(Regex("\\s+"))
        for (p in palabras) {
            if (mapaNumeros.containsKey(p)) return mapaNumeros[p]
        }
        return null
    }

    private fun reiniciar() {
        fase = 0
    }
}