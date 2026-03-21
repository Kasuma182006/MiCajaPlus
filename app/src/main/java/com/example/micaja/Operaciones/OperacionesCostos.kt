package com.example.micaja.Operaciones

import com.example.micaja.ConexionService.ConexionServiceTienda
import com.example.micaja.models.compra_Mercancia
import com.example.micaja.models.modelo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OperacionCosto {
    private enum class EstadoCosto { INACTIVO, ESPERANDO_NOMBRE, ESPERANDO_CANTIDAD, ESPERANDO_PRECIO, ESPERANDO_CONFIRMACION, ESPERANDO_PROVEEDOR }

    private var estadoActual = EstadoCosto.INACTIVO
    private var nombreProducto = ""
    private var cantidadProducto = 1
    private var precioCompraTotal = 0
    private var presentacionProducto = "presentación"
    private var proveedor = ""

    companion object {
        val unidades = listOf(
            "arroba", "bolsa", "bolsita", "bulto", "bultos",
            "caja", "cajetilla", "canasta", "chuspa", "barra",
            "capsula", "cubeta", "tableta", "docena", "gramo",
            "lb", "libra", "kilo", "kg", "kilogramos", "litro", "lt",
            "litron", "mililitro", "mililitros", "ml",
            "envase", "frasco", "onza", "plastico", "paquete", "vidrio",
            "media", "mediana", "mediano", "grande", "pequeña", "pequeño",
            "pet", "personal", "canasta", "retornable", "retornables",
            "garrafa", "lata", "laton", "paca", "sixpack", "six-pack",
            "panal", "sobre", "rollo", "tubo", "unidad", "vasito", "vaso"
        )

        val limpiarN = listOf(
            "compra de mercancía", "comprar", "compras", "compré",
            "costo", "costos", "pagamos", "pagado",
            "pague" ,"pagué", "pedido", "factura", "de", "del",
            "por", "el", "la", "los", "las", "me"
        )

        val numerosMap = mapOf(
            "un" to "1", "uno" to "1", "dos" to "2", "tres" to "3",
            "cuatro" to "4", "cinco" to "5", "seis" to "6",
            "siete" to "7", "ocho" to "8", "nueve" to "9", "diez" to "10"
        )
    }

    fun iniciarFlujoCosto(enviarMensajeSistema: (modelo) -> Unit) {
        estadoActual = EstadoCosto.ESPERANDO_NOMBRE
        enviarMensajeSistema(modelo("He detectado una compra de mercancía. ¿Qué producto compraste?"))
    }

    suspend fun procesarRespuesta(
        texto: String,
        enviarMensajeSistema: (modelo) -> Unit,
        idTendero: String
    ): Boolean {
        if (estadoActual == EstadoCosto.INACTIVO) return false
        val textoLimpio = texto.trim()

        when (estadoActual) {
            EstadoCosto.ESPERANDO_NOMBRE -> {
                var nombreLimpio = textoLimpio.lowercase()
                limpiarN.forEach { ruido ->
                    nombreLimpio = nombreLimpio.replace(Regex("\\b$ruido\\b"), "").trim()
                }
                nombreProducto = nombreLimpio.replace(Regex("""\s+"""), " ").replaceFirstChar { it.uppercase() } ?: ""

                if (nombreProducto.isBlank() || nombreProducto.length < 2) {
                    enviarMensajeSistema(modelo("No pude entender el nombre del producto. ¿Qué producto compraste?"))
                    return true
                }
                estadoActual = EstadoCosto.ESPERANDO_CANTIDAD
                enviarMensajeSistema(modelo("¿Qué cantidad y presentación de '$nombreProducto' compraste? (Ej: 10 libras, 2 litros)"))
            }

            EstadoCosto.ESPERANDO_CANTIDAD -> {
                var procesado = textoLimpio.lowercase()
                numerosMap.forEach { (palabra, numero) ->
                    procesado = procesado.replace(Regex("\\b$palabra\\b"), numero)
                }

                presentacionProducto = "Unidad"
                for (u in unidades) {
                    if (procesado.contains(u)) {
                        presentacionProducto = u
                        break
                    }
                }

                val numero = Regex("""\b\d+\b""").find(procesado)?.value?.toIntOrNull()
                // VALIDACIÓN: Exigir un número. Si no hay número, pero dictó una presentación válida (ej: "compré cajas"), asumimos 1.
                if (numero != null && numero > 0) {
                    cantidadProducto = numero
                } else if (presentacionProducto != "Unidad") {
                    cantidadProducto = 1
                } else {
                    enviarMensajeSistema(modelo("No pude identificar la cantidad. Por favor, dime cuántas unidades o paquetes compraste (Ej: 5 cajas)."))
                    return true
                }

                estadoActual = EstadoCosto.ESPERANDO_PRECIO
                enviarMensajeSistema(modelo("¿Cuál fue el precio total de la compra por $cantidadProducto $presentacionProducto(s)?"))
            }

            EstadoCosto.ESPERANDO_PRECIO -> {
                val precio = textoLimpio.replace(Regex("[^0-9]"), "").toIntOrNull()
                if (precio != null && precio >= 50) {
                    precioCompraTotal = precio
                    estadoActual = EstadoCosto.ESPERANDO_PROVEEDOR
                    enviarMensajeSistema(modelo("¿A qué proveedor le compraste la mercancía?"))
                } else {
                    enviarMensajeSistema(modelo("El precio dictado no parece válido. Por favor dicte nuevamente el valor total de la factura en números."))
                }
            }

            EstadoCosto.ESPERANDO_PROVEEDOR -> {
                var nombreProveedor = textoLimpio.lowercase()
                limpiarN.forEach { ruido ->
                    nombreProveedor = nombreProveedor.replace(Regex("\\b$ruido\\b"), "").trim()
                }
                proveedor = nombreProveedor.replace(Regex("""\s+"""), " ").replaceFirstChar { it.uppercase() } ?: ""
                if (proveedor.isBlank() || proveedor.length < 2) {
                    enviarMensajeSistema(modelo("No pude registrar el proveedor correctamente. ¿A quién le compraste la mercancía?"))
                    return true
                }
                estadoActual = EstadoCosto.ESPERANDO_CONFIRMACION
                enviarMensajeSistema(modelo("¿Confirma la compra de $cantidadProducto $presentacionProducto(s) de $nombreProducto por $$precioCompraTotal al proveedor $proveedor? (Sí/No)"))
            }
            EstadoCosto.ESPERANDO_CONFIRMACION -> {
                validarConfirmacion(textoLimpio.lowercase(), enviarMensajeSistema, idTendero)
            }
            else -> {}
        }
        return true
    }

    private suspend fun validarConfirmacion(
        texto: String,
        enviarMensajeSistema: (modelo) -> Unit,
        idTendero: String
    ) {
        val afirmativo = listOf("si", "sí", "confirmar", "confirmo", "claro", "aceptar", "vale", "correcto", "ajá", "aja", "epa", "ok", "de acuerdo", "hágale", "hagale")
        val negativo = listOf("no", "fin", "finalizar", "cancelar", "terminar", "parar", "ninguno", "incorrecto")
        val esAfirmativo = afirmativo.any { Regex("(?i)\\b$it\\b").containsMatchIn(texto) }
        val esNegativo = negativo.any { Regex("(?i)\\b$it\\b").containsMatchIn(texto) }

        when {
            esAfirmativo -> {
                try {
                    val factura = compra_Mercancia(
                        idTendero = idTendero,
                        cantidadStock = cantidadProducto,
                        presentacion = presentacionProducto.replaceFirstChar { it.uppercase() },
                        nombre = nombreProducto,
                        precioCompra = precioCompraTotal,
                        proveedor = proveedor
                    )

                    val respuesta = withContext(Dispatchers.IO) {
                        ConexionServiceTienda.create().compra_Mercancia(factura)
                    }

                    if (respuesta.isSuccessful) {
                        enviarMensajeSistema(modelo("¡Compra registrada! Se han sumado $cantidadProducto $presentacionProducto(s) de $nombreProducto al inventario."))
                        finalizarFlujo()
                    } else {
                        enviarMensajeSistema(modelo("Error al guardar en el servidor. ¿Deseas intentar registrarlo de nuevo? (Sí/No)"))
                    }
                } catch (e: Exception) {
                    enviarMensajeSistema(modelo("Error de conexión. No pude registrar la compra de tu mercancía\n. ¿Deseas intentar enviarlo de nuevo? (Sí/No)"))
                    // Omitimos finalizarFlujo() para permitir el reintento de red
                }
            }

            esNegativo -> {
                enviarMensajeSistema(modelo("Operación cancelada. No se realizó ningún registro."))
                finalizarFlujo()
            }

            else -> {
                enviarMensajeSistema(modelo("No te entendí. Di 'Sí' para confirmar la compra de $nombreProducto o 'No' para cancelar."))
            }
        }
    }

    private fun finalizarFlujo() {
        estadoActual = EstadoCosto.INACTIVO
        nombreProducto = ""
        cantidadProducto = 1
        precioCompraTotal = 0
    }

    fun cancelarFlujo() {
        estadoActual = EstadoCosto.INACTIVO
        nombreProducto = ""
        cantidadProducto = 1
        precioCompraTotal = 0
        proveedor = ""
    }
}