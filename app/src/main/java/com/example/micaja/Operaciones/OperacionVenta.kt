package com.example.micaja.Operaciones

import com.example.micaja.ConexionService.ConexionServiceTienda
import com.example.micaja.models.consultarIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class OperacionVenta {

    var inicio = false

    companion object {
        val unidades = listOf(
            "bolsa", "bolsita", "caja", "cajetilla", "canasta", "chuspa",
            "barra", "capsula", "cápsula", "cubeta", "tableta",
            "docena", "gramo", "libra", "kilo", "kilogramos", "litro", "litrón", "onza",
            "envase", "frasco", "plastico", "plástico", "paquete", "vidrio",
            "pequeña", "pequeño", "media", "mediana", "mediano", "grande",
            "garrafa", "lata", "latón", "paca", "sixpack", "six-pack",
            "panal", "sobre", "rollo", "tubo", "unidad", "vasito", "vaso"
        )

        val limpiarN = listOf(
            "vendi", "vendí", "vende", "venta", "un", "una", "de", "del",
            "por", "el", "la", "los", "las", "me", "compraron", "salio"
        )
    }

    suspend fun procesarListaProductos(texto: String): String {
        if (texto.contains("fin")) {
            inicio = false
            return "Venta finalizada"
        }
        val datos = extraerDatosProducto(texto)

        if (datos == null) {
            return "No entendí el producto, intenta decir algo como: '2 libras de arroz' o 'un aceite'"
        }

        val (nombre, pres, cant) = datos


        return try {
            val conexion = ConexionServiceTienda.create()
            val modelo = consultarIn(nombre)


            val respuesta = conexion.consultarInv(modelo)

            if (respuesta.isSuccessful && respuesta.body() != null) {
                "• $cant $pres de $nombre registrado. ¿Algo más? (o di 'fin')"

            } else {
                "El producto '$nombre' no existe en tu inventario. Verifica el nombre."
            }
        } catch (e: Exception) {
            "No se pudo conectar con el servidor: ${e.message}"
        }
    }
    private fun extraerDatosProducto(segmento: String): Triple<String, String, Int>? {
        var texto = segmento.trim()

        val cifra = Regex("""\b\d+\b""").find(texto)
        val cantidad = cifra?.value?.toIntOrNull() ?: 1

        if (cifra != null) texto = texto.replaceFirst(cifra.value, "").trim()


        var unidadDetectada = "unidad"
        for (u in unidades) {
            if (texto.contains(u)) {
                unidadDetectada = u
                texto = texto.replace(u, "").trim()
                break
            }
        }


        var nombre = texto
        for (palabra in limpiarN) {
            nombre = nombre.replace(Regex("""\b$palabra\b"""), "")
        }

        nombre = nombre.trim().replace(Regex("""\s+"""), " ")

        if (nombre.isNotEmpty()) {
            return Triple(nombre.replaceFirstChar { it.uppercase() }, unidadDetectada, cantidad)
        } else {
            return null
        }
    }
}