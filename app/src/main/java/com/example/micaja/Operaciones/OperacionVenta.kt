package com.example.micaja.Operaciones

import android.util.Log
import com.example.micaja.ConexionService.ConexionServiceTienda
import com.example.micaja.models.OperacionesInventario
import com.example.micaja.models.Venta
import com.example.micaja.models.ventaDetectada
import java.text.Normalizer

class OperacionVenta() {
    var inicio = false

    companion object {
        // Lista de unidades/presentaciones sin tildes (ya que normalizamos el texto)
        val unidades = listOf(
            "bolsa", "bolsita", "caja", "cajetilla", "canasta", "chuspa",
            "barra", "capsula", "cubeta", "tableta",
            "docena", "gramo", "libra", "kilo", "kilogramos", "litro", "litron", "onza",
            "envase", "frasco", "plastico", "paquete", "vidrio",
            "pequena", "pequeno", "media", "mediana", "mediano", "grande",
            "garrafa", "lata", "laton", "paca", "sixpack", "six-pack",
            "panal", "sobre", "rollo", "tubo", "unidad", "vasito", "vaso"
        )

        // Palabras de acción que deben eliminarse para no ensuciar el nombre del producto
        val limpiarN = listOf(
            "vendi", "vende", "venta", "un", "una", "de", "del",
            "por", "el", "la", "los", "las", "me", "compraron", "salio", "pago","s","S"
        )

        // Mapa para convertir dictado de voz a números procesables
        val numerosMap = mapOf(
            "un" to "1", "uno" to "1", "dos" to "2", "tres" to "3",
            "cuatro" to "4", "cinco" to "5", "seis" to "6",
            "siete" to "7", "ocho" to "8", "nueve" to "9", "diez" to "10"
        )
    }

    /**
     * Normaliza el texto: quita tildes y convierte palabras numéricas a dígitos.
     */
    private fun normalizarTexto(texto: String): String {
        // 1. Quitar tildes y caracteres especiales
        val temp = Normalizer.normalize(texto.lowercase(), Normalizer.Form.NFD)
        val sinTildes = Regex("\\p{InCombiningDiacriticalMarks}+").replace(temp, "")

        // 2. Reemplazar palabras ("dos") por números ("2")
        var resultado = sinTildes
        numerosMap.forEach { (palabra, numero) ->
            resultado = resultado.replace(Regex("\\b$palabra\\b"), numero)
        }
        return resultado
    }

    suspend fun procesarListaProductos(texto: String, cedula: String, fin_credito: Boolean, idCliente: String = "", idTendero: String): String {
        // Paso fundamental: Limpiar la entrada de voz antes de procesar
        val textoLimpio = normalizarTexto(texto)
        if (textoLimpio.contains("fin") || fin_credito) {
            this.inicio = false

            return if (idCliente.isNotEmpty() && idCliente != idTendero) {
                "¡Listo! El crédito se ha registrado con éxito para el cliente."
            } else {
                "Venta de contado finalizada correctamente."
            }
        }


        val datos = extraerDatosProducto(textoLimpio)

        if (datos != null) {
            val (nombre, pres, cant) = datos
            val conexion = ConexionServiceTienda.create()

            Log.i("datos_procesados", "Nombre: $nombre, Pres: $pres, Cant: $cant")


            val producto = Venta(nombre, pres, cant, cedula,  idCliente)
            val respuesta = conexion.registrarVenta(producto)

            return try {
                val conexion = ConexionServiceTienda.create()
                val modelo = OperacionesInventario(idTendero, nombre, pres, cant, "descontar")
                Log.d("modelo" , "modelo de datos ${modelo}")

                val respuesta = conexion.operacionesInventario(modelo)

                if (respuesta.isSuccessful && respuesta.body() != null) {

                    val cuerpo = respuesta.body()
                    Log.d("respuesta", "respuesta del servidor ${cuerpo}")


                    val tipoVenta = if (idCliente.isNotEmpty() && idCliente != idTendero) "credito" else "efectivo"

                    val modeloVenta = ventaDetectada(idTendero,texto,tipoVenta, nombre,cant)
                    val respuestaVenta = conexion.ventaDetectada(modeloVenta)
                    if (respuestaVenta.isSuccessful && respuestaVenta.body() != null){
                        Log.d("venta", "venta registrada compita")
                    }
                    "• $cant $pres de $nombre registrado. ¿Algo más? (o di 'fin')"
                } else {
                    "El producto '$nombre' no existe en tu inventario. Verifica el nombre."
                }
            } catch (e: Exception) {
                "Fuera de conexion intentalo de nuevo"
            }
        }

        return "No entendí el producto, intenta decir algo como: '2 libras de arroz' o 'un aceite'"
    }

    private fun extraerDatosProducto(segmento: String): Triple<String, String, Int>? {
        var texto = segmento.trim()

        // 1. LIMPIEZA DE ACCIONES: Quitamos "vendi", "venta", etc., al principio
        for (palabra in limpiarN) {
            texto = texto.replace(Regex("(?i)\\b$palabra\\b"), "").trim()
        }

        // 2. EXTRAER CANTIDAD: Buscamos el primer número que aparezca
        val cifra = Regex("""\b\d+\b""").find(texto)
        val cantidad = cifra?.value?.toIntOrNull() ?: 1

        // Removemos la cifra del texto para que no sea parte del nombre
        if (cifra != null) {
            texto = texto.replaceFirst(cifra.value, "").trim()
        }

        // 3. DETECTAR UNIDAD/PRESENTACIÓN
        var unidadDetectada = "unidad"
        for (u in unidades) {
            if (texto.contains(u)) {
                unidadDetectada = u
                texto = texto.replace(u, "").trim()
                break
            }
        }
        val nombreSinS = texto.replace(Regex("(?i)\\b[s]\\b"), "").trim()

        // 4. LIMPIEZA FINAL DEL NOMBRE: Quitar espacios dobles y basura
        val nombreFinal = nombreSinS.replace(Regex("""\s+"""), " ")

        return if (nombreFinal.isNotEmpty()) {
            Triple(nombreFinal, unidadDetectada, cantidad)
        } else {
            null
        }
    }
}