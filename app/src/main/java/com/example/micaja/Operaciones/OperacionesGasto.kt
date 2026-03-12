package com.example.micaja.Operaciones

import com.example.micaja.Operaciones.OperacionesGastos.Companion.limpiarN
import com.example.micaja.calcularMonto
import com.example.micaja.cedulaGlobal
import com.example.micaja.models.gastoDetectado

class OperacionesGastos {
    var inicio = false

    companion object {

        val limpiarN = listOf(
            "egreso","egresos","gastamos","gastan","gastando",
            "gastaron","gasté","gasto","gastó","gastos",
            "pagué","pago","pagamos", "pagaron",
            "en","un","una","de","del","por", "el","la","los","las"
        )
    }

    fun procesarListaGastos(texto: String): String {

        if (texto.contains("fin")) {
            inicio = false
            return "Se ha guardado un listado de gastos."
        }

        val dtGasto = justificaGasto(texto)

        if (dtGasto == null) {
            return "No entendí el gasto. Ejemplo: 'Me gasté 18000 en un almuerzo'"
        }

        val justificacion = dtGasto.mensaje
        val precio = dtGasto.precio

        return "• Se ha registrado un gasto $justificacion por $$precio. ¿Algo más? (o di 'fin' para finalizar la operación.)"
    }
}

private fun justificaGasto(segmen: String): gastoDetectado? {

    var texto = segmen.trim()
    var precioFinal: Int? = null
    var textoPrecioEncontrado = ""

    val palabras = texto.split(" ")
    for (p in palabras) {
        val monto = calcularMonto(p)

        if (monto != null) {
            precioFinal = monto
            textoPrecioEncontrado = p
            break
        }
    }

    if (precioFinal == null) return null
    texto = texto.replace(textoPrecioEncontrado, "").trim()
    var msjGasto = texto

    for (palabra in limpiarN) {
        msjGasto = msjGasto.replace(Regex("\\b$palabra\\b", RegexOption.IGNORE_CASE), "")
    }

    msjGasto = msjGasto.trim().replace(Regex("\\s+"), " ")

    return if (msjGasto.isNotEmpty()) {
        gastoDetectado(
            idTendero = cedulaGlobal,
            mensaje = msjGasto.replaceFirstChar { it.uppercase() },
            precio = precioFinal
        )
    } else null
}