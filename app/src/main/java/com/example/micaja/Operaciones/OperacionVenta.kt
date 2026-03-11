package com.example.micaja.Operaciones

import android.util.Log
import com.example.micaja.ConexionService.ConexionServiceTienda
import com.example.micaja.models.OperacionesInventario
import com.example.micaja.models.ventaDetectada
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
            "garrafa", "lata", "latón", "paca", "sixpack", "six pack","sispa",
            "panal", "sobre", "rollo", "tubo", "unidad", "vasito", "vaso"
        )

        val limpiarN = listOf(
            "vendi", "vendí", "vende", "venta", "un", "una", "de", "del",
            "por", "el", "la", "los", "las", "me", "compraron", "salio","s","S","ventas","costos"
        )
    }

    suspend fun procesarListaProductos(texto: String, idTendero: String): String {
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
            val modelo = OperacionesInventario(idTendero, nombre, pres, cant, "descontar")
            Log.d("modelo" , "modelo de datos ${modelo}")

            val respuesta = conexion.operacionesInventario(modelo)

            if (respuesta.isSuccessful && respuesta.body() != null) {

                val cuerpo = respuesta.body()
                Log.d("respuesta", "respuesta del servidor ${cuerpo}")

                val modeloVenta = ventaDetectada(idTendero,texto,"Efectivo", nombre,cant)
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
            return Triple(nombre, unidadDetectada, cantidad)
        } else {
            return null
        }
    }

}