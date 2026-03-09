package com.example.micaja.Operaciones

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

    fun procesarListaProductos(texto: String): String {
        val datos = extraerDatosProducto(texto)

        if (datos != null) {
            val (nombre, pres, cant) = datos
            return "• $cant $pres de $nombre registrado. ¿Algo más? (o di 'fin')"
        } else {
            return "No entendí el producto, intenta decir algo como: '2 libras de arroz' o 'un aceite'"
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