package com.example.micaja.Operaciones

import com.example.micaja.ConexionService.ConexionServiceTienda
import com.example.micaja.Operaciones.OperacionVenta.Companion.limpiarN
import com.example.micaja.models.compra_Mercancia
import com.example.micaja.models.modelo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OperacionCosto {
    private enum class EstadoCosto { INACTIVO, ESPERANDO_NOMBRE, ESPERANDO_CANTIDAD, ESPERANDO_PROVEEDOR, ESPERANDO_CONFIRMACION }

    private var estadoActual = EstadoCosto.INACTIVO
    private var nombreProducto = ""
    private var cantidadProducto = 1
    private var presentacionProducto = "Presentación"
    private var proveedor = ""

    companion object {
        val unidades = listOf(
            "arroba", "atomizador", "barra", "bidon", "bidón", "bolsa", "bolsita", "botella", "botellas", "botellita", "botellitas",
            "bulto", "bultos", "caja", "cajas", "cajetilla", "cajetillas", "canasta", "caneca", "capsula", "carton", "cartón",
            "chuspa", "chuspas", "cubeta", "docena", "envase", "etapa", "etapas", "frasco", "galon", "galón", "garrafa", "gr",
            "gramo", "gramos", "grande", "grandes", "granel", "hoja", "hojas", "k", "kg", "kilo", "kilogramo", "kilos", "l",
            "lata", "laton", "latón", "lb", "libra", "libras", "lt", "lts", "litro", "litron", "litros", "manojo", "ml", "media",
            "mediana", "medianas", "mediano", "medianos", "mililitro", "mililitros", "mini", "oz", "onza", "paca", "panal",
            "pequeña", "pequeñas", "pequeño", "pequeños", "personal", "pet", "plastico", "plástico", "plasticos", "plásticos",
            "paquete", "paquetes", "pokeron", "pokerón", "polvo", "racimo", "retornable", "rollo", "rollos", "sixpack", "six-pack",
            "sobre", "spray", "tableta", "tarrito", "tarro", "tubo", "unidad", "unidades", "vasito", "vasitos", "vaso", "vasos", "vidrio"
        )

        val numerosMap = mapOf(
            "un" to "1", "uno" to "1", "dos" to "2", "tres" to "3", "cuatro" to "4", "cinco" to "5",
            "seis" to "6", "siete" to "7", "ocho" to "8", "nueve" to "9", "diez" to "10"
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

        if (textoLimpio.lowercase().contains("fin")) {
            cancelarFlujo()
            enviarMensajeSistema(modelo("Registro de compra cancelado correctamente."))
            return true
        }

        when (estadoActual) {
            EstadoCosto.ESPERANDO_NOMBRE -> {
                var nombreLimpio = textoLimpio.lowercase()
                limpiarN.forEach { ruido ->
                    nombreLimpio = nombreLimpio.replace(Regex("\\b$ruido\\b"), "").trim()
                }
                nombreProducto = nombreLimpio.replace(Regex("""\s+"""), " ").replaceFirstChar { it.uppercase() }

                if (nombreProducto.isBlank() || nombreProducto.length < 2) {
                    enviarMensajeSistema(modelo("No pude entender el nombre del producto. ¿Qué producto compraste?"))
                    return true
                }

                estadoActual = EstadoCosto.ESPERANDO_CANTIDAD
                enviarMensajeSistema(modelo("¿Qué cantidad y presentación de '$nombreProducto' compraste? (Ej: 10 libras, 2 cajas)"))
            }

            EstadoCosto.ESPERANDO_CANTIDAD -> {
                var procesado = textoLimpio.lowercase()
                numerosMap.forEach { (palabra, numero) ->
                    procesado = procesado.replace(Regex("\\b$palabra\\b"), numero)
                }

                presentacionProducto = "Unidad"
                for (u in unidades) {
                    if (procesado.contains(u)) {
                        presentacionProducto = u.replaceFirstChar { it.lowercase() }
                        break
                    }
                }

                val numero = Regex("""\b\d+\b""").find(procesado)?.value?.toIntOrNull()
                if (numero != null && numero > 0) {
                    cantidadProducto = numero
                } else if (presentacionProducto != "Unidad") {
                    cantidadProducto = 1
                } else {
                    enviarMensajeSistema(modelo("No pude identificar la cantidad. Por favor, dime cuántas unidades o paquetes compraste (Ej: 5 cajas)."))
                    return true
                }

                try {
                    val peticion = compra_Mercancia(
                        idTendero = idTendero,
                        nombre = nombreProducto,
                        presentacion = presentacionProducto
                    )
                    val respuestaVerificacion = withContext(Dispatchers.IO) { ConexionServiceTienda.create().verificarProductoExistente(peticion) }
                    val productoExiste = respuestaVerificacion.body()?.get("existe") == true
                    if (respuestaVerificacion.isSuccessful && productoExiste) {
                        // Si el producto existe, pasamos a pedir el proveedor
                        estadoActual = EstadoCosto.ESPERANDO_PROVEEDOR
                        enviarMensajeSistema(modelo("¡Perfecto! ¿A qué proveedor le compraste $nombreProducto $presentacionProducto?"))
                    } else {
                        enviarMensajeSistema(modelo("El producto '$nombreProducto' '$presentacionProducto' no existe en tu inventario. Debes primero registrar el producto con el comando 'Agregar producto' "))
                        cancelarFlujo()
                    }
                } catch (e: Exception) {
                    enviarMensajeSistema(modelo("Hubo un problema de conexión mientras consultaba el producto. Por favor, inténtalo de nuevo."))
                    cancelarFlujo()
                }
            }

            EstadoCosto.ESPERANDO_PROVEEDOR -> {
                var nombreProveedor = textoLimpio.lowercase()
                limpiarN.forEach { ruido ->
                    nombreProveedor = nombreProveedor.replace(Regex("\\b$ruido\\b"), "").trim()
                }

                // Corregido: Se quitó el ?: "" innecesario
                proveedor = nombreProveedor.replace(Regex("""\s+"""), " ").replaceFirstChar { it.uppercase() }

                if (proveedor.isBlank() || proveedor.length < 2) {
                    enviarMensajeSistema(modelo("No pude registrar el proveedor correctamente. ¿A quién le compraste la mercancía?"))
                    return true
                }

                estadoActual = EstadoCosto.ESPERANDO_CONFIRMACION
                enviarMensajeSistema(modelo("¿Confirma la entrada de $cantidadProducto $presentacionProducto(s) de $nombreProducto al proveedor $proveedor? (Sí/No)"))
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
                        presentacion = presentacionProducto,
                        nombre = nombreProducto,
                        proveedor = proveedor
                    )

                    val respuesta = withContext(Dispatchers.IO) {
                        ConexionServiceTienda.create().compra_Mercancia(factura)
                    }

                    if (respuesta.isSuccessful) {
                        enviarMensajeSistema(modelo("¡Compra registrada! Se han sumado $cantidadProducto $presentacionProducto(s) de $nombreProducto al inventario."))
                        finalizarFlujo()
                    } else {
                        if (respuesta.code() == 404) {
                            enviarMensajeSistema(modelo("El producto '$nombreProducto' no existe en tu inventario. Por favor, cancela esta operación y agrégalo primero en la sección de productos."))
                            cancelarFlujo() // Buena lógica aquí
                        } else {
                            enviarMensajeSistema(modelo("Error al guardar en el servidor (Código: ${respuesta.code()}). ¿Deseas intentar registrarlo de nuevo? (Sí/No)"))
                        }
                    }
                } catch (e: Exception) {
                    enviarMensajeSistema(modelo("Error de conexión. No pude registrar la compra de tu mercancía. ¿Deseas intentar enviarlo de nuevo? (Sí/No)"))
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
        presentacionProducto = "Presentación"
        proveedor = ""
    }

    fun cancelarFlujo() {
        finalizarFlujo()
    }
}