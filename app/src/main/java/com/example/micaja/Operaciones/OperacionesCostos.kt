package com.example.micaja.Operaciones



import com.example.micaja.ConexionService.ConexionServiceTienda
import com.example.micaja.models.compra_Mercancia
import com.example.micaja.models.modelo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OperacionCosto {
    private enum class EstadoCosto {
        INACTIVO, ESPERANDO_NOMBRE, ESPERANDO_CANTIDAD, ESPERANDO_PRECIO, ESPERANDO_CONFIRMACION, ESPERANDO_PROVEEDOR
    }

    private var estadoActual = EstadoCosto.INACTIVO
    private var nombreProducto = ""
    private var cantidadProducto = 1
    private var precioCompraTotal = 0
    private var presentacionProducto = "presentación"
    private var proveedor = ""

    companion object {
        val unidades = listOf(
            "bolsa", "bolsita", "bulto", "bultos",
            "caja", "cajetilla", "canasta", "chuspa", "barra",
            "capsula", "cubeta", "tableta", "docena", "gramo",
            "libra", "kilo", "kilogramos", "litro", "litron", "onza",
            "envase", "frasco", "plastico", "paquete", "vidrio",
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

                nombreProducto = nombreLimpio.replaceFirstChar { it.uppercase() }

                if (nombreProducto.isEmpty()) nombreProducto = textoLimpio // Backup por si queda vacío

                estadoActual = EstadoCosto.ESPERANDO_CANTIDAD
                enviarMensajeSistema(modelo("¿Qué cantidad de '$nombreProducto' compraste? (Ej: 10, 2 pacas, 1 caja)"))
            }

            EstadoCosto.ESPERANDO_PROVEEDOR -> {
                var nombreProveedor = textoLimpio.lowercase()
                limpiarN.forEach { ruido ->
                    nombreProveedor = nombreProveedor.replace(Regex("\\b$ruido\\b"), "").trim()
                }

                proveedor = nombreProveedor.replaceFirstChar { it.uppercase() }

                if (proveedor.isEmpty()) nombreProveedor = textoLimpio

                estadoActual = EstadoCosto.ESPERANDO_CANTIDAD
                enviarMensajeSistema(modelo("Perfecto he registrado a '$nombreProveedor' como el proveedor de este producto."))
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
                cantidadProducto = numero ?: 1

                estadoActual = EstadoCosto.ESPERANDO_PRECIO

                enviarMensajeSistema(modelo("¿Cuál fue el precio total de la compra por $cantidadProducto $presentacionProducto(s)?"))
            }

            EstadoCosto.ESPERANDO_PRECIO -> {
                val precio = textoLimpio.replace(Regex("[^0-9]"), "").toIntOrNull()
                if (precio != null && precio > 0) {
                    precioCompraTotal = precio
                    estadoActual = EstadoCosto.ESPERANDO_CONFIRMACION
                    enviarMensajeSistema(modelo("¿Confirma la compra de $cantidadProducto $nombreProducto por $$precioCompraTotal? (Sí/No)"))
                } else {
                    enviarMensajeSistema(modelo("No entendí el precio. Por favor dicte nuevamente el valor total de la factura."))
                }
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
        val afirmativo = listOf("si", "sí", "confirmar", "confirmo", "claro", "aceptar", "vale", "correcto", "ajá", "aja", "epa", "ok", "de acuerdo")
        val negativo = listOf("no", "fin", "finalizar", "cancelar", "terminar", "parar", "ninguno")

        when {
            afirmativo.any { texto.contains(it) } -> {
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
                        enviarMensajeSistema(modelo("¡Compra registrada! Se han sumado $cantidadProducto unidades de $nombreProducto al inventario."))
                    } else {
                        enviarMensajeSistema(modelo("Error al guardar en el servidor. Inténtalo de nuevo."))
                    }
                    finalizarFlujo()
                } catch (e: Exception) {
                    enviarMensajeSistema(modelo("Error de conexión. No se pudo registrar la compra."))
                    finalizarFlujo()
                }
            }

            negativo.any { texto.contains(it) } -> {
                enviarMensajeSistema(modelo("Operación cancelada. No se realizó ningún registro."))
                finalizarFlujo()
            }

            else -> {
                enviarMensajeSistema(modelo("No te entendí. Di 'Sí' para confirmar o 'No' para cancelar la compra de $nombreProducto."))
            }
        }
    }

    private fun finalizarFlujo() {
        estadoActual = EstadoCosto.INACTIVO
        nombreProducto = ""
        cantidadProducto = 1
        precioCompraTotal = 0
    }
}
