package com.example.micaja.Operaciones

import com.example.micaja.calcularMonto
import com.example.micaja.models.compra_Mercancia

class OperacionesCostos {

    var inicio = false

    companion object {

        val limpiarN = listOf(
            "compra", "comprar", "compras", "compré",
            "costo", "costos",
            "factura",
            "mercancia", "mercancía",
            "pagar", "pagamos",
            "pague", "pagué",
            "pedido"
        )
    }

    fun procesarListadoDeCostos(texto: String): String {

        if (texto.contains("fin", ignoreCase = true)) {
            inicio = false
            return "Se ha guardado el listado de compras de mercancía."
        }

        val compra = de_Compras(texto)

        if (compra == null) {
            return "No entendí lo que compraste. Di algo como: 'Compré 180000 en productos Postobón'."
        }

        val nombreProducto = compra.nombre
        val presentacion = compra.presentacion
        val proveedor = compra.proveedor
        val facturaDeCompras = compra.precioCompra
        val cantidadDisponible = compra.cantidadStock

        return """  • Se ha registrado una compra de $cantidadDisponible $presentacion de $nombreProducto
             por un total de $$facturaDeCompras al proveedor $proveedor. ¿Algo más? (o di "fin" para finalizar la operación.)""".trimIndent()
    }
}

private fun de_Compras(segmentoScrum: String): compra_Mercancia? {

    var texto = segmentoScrum.trim()
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

    var msjCostos = texto

    for (palabra in OperacionesCostos.limpiarN) {

        msjCostos = msjCostos.replace(
            Regex("\\b$palabra\\b", RegexOption.IGNORE_CASE),
            ""
        )
    }

    msjCostos = msjCostos
        .trim()
        .replace(Regex("\\s+"), " ")

    if (msjCostos.isEmpty()) return null

    return compra_Mercancia(
        nombre = msjCostos.replaceFirstChar { it.uppercase() },
        presentacion = "unidad",
        cantidadStock = 1,
        precioCompra = precioFinal,
        proveedor = "Desconocido"
    )
}
