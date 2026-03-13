package com.example.micaja.Operaciones

import com.example.micaja.ConexionService.ConexionServiceTienda
import com.example.micaja.models.Producto
import com.example.micaja.models.modelo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GestionProductos {

    // 1. Definimos los estados del flujo
    private enum class EstadoProducto {
        INACTIVO, ESPERANDO_CATEGORIA, ESPERANDO_NOMBRE, ESPERANDO_PRESENTACION, ESPERANDO_CONFIRMACION
    }

    private var estadoActual = EstadoProducto.INACTIVO
    private var categoriaTmp = ""
    private var nombreTmp = ""
    private var presentacionTmp = ""

    // 2. Iniciamos el proceso
    fun iniciarFlujoProducto(enviarMensajeSistema: (modelo) -> Unit) {
        estadoActual = EstadoProducto.ESPERANDO_CATEGORIA
        enviarMensajeSistema(modelo("¡Listo! Vamos a agregar un producto. ¿A qué categoría pertenece? (ejemplo: Lácteos, Aseo, Bebidas)"))
    }

    // 3. Procesador de respuestas (El cerebro)
    suspend fun procesarRespuesta(
        texto: String,
        enviarMensajeSistema: (modelo) -> Unit,
        idTendero: String
    ): Boolean {
        if (estadoActual == EstadoProducto.INACTIVO) return false
        val textoLimpio = texto.trim()

        when (estadoActual) {
            EstadoProducto.ESPERANDO_CATEGORIA -> {
                guardarCategoria(textoLimpio, enviarMensajeSistema)
            }
            EstadoProducto.ESPERANDO_NOMBRE -> {
                guardarNombre(textoLimpio, enviarMensajeSistema)
            }
            EstadoProducto.ESPERANDO_PRESENTACION -> {
                guardarPresentacion(textoLimpio, enviarMensajeSistema)
            }
            EstadoProducto.ESPERANDO_CONFIRMACION -> {
                validarConfirmacion(textoLimpio, enviarMensajeSistema, idTendero)
            }
            else -> {}
        }
        return true
    }

    // --- Funciones de validación paso a paso ---

    private fun guardarCategoria(texto: String, enviarMensajeSistema: (modelo) -> Unit) {
        if (texto.length > 2) {
            categoriaTmp = texto
            estadoActual = EstadoProducto.ESPERANDO_NOMBRE
            enviarMensajeSistema(modelo("Entendido, categoría: $categoriaTmp. Ahora, ¿cuál es el nombre del producto?"))
        } else {
            enviarMensajeSistema(modelo("Por favor, dime un nombre de categoría válido."))
        }
    }

    private fun guardarNombre(texto: String, enviarMensajeSistema: (modelo) -> Unit) {
        if (texto.length > 2) {
            nombreTmp = texto
            estadoActual = EstadoProducto.ESPERANDO_PRESENTACION
            enviarMensajeSistema(modelo("Perfecto: $nombreTmp. ¿Cuál es la presentación? (ejemplo: Bolsa un litro, Libra, Lata)"))
        } else {
            enviarMensajeSistema(modelo("El nombre es muy corto, por favor dime el nombre del producto."))
        }
    }

    private fun guardarPresentacion(texto: String, enviarMensajeSistema: (modelo) -> Unit) {
        if (texto.length >= 2) {
            presentacionTmp = texto
            estadoActual = EstadoProducto.ESPERANDO_CONFIRMACION
            val resumen = """
                ¿Confirmas este producto?
                Categoría: $categoriaTmp
                Nombre: $nombreTmp
                Presentación: $presentacionTmp
                
                Diga 'Sí' para guardar o 'No' para cancelar.
            """.trimIndent()
            enviarMensajeSistema(modelo(resumen))
        } else {
            enviarMensajeSistema(modelo("Dime la presentación del producto para terminar."))
        }
    }

    private suspend fun validarConfirmacion(
        texto: String,
        enviarMensajeSistema: (modelo) -> Unit,
        idTendero: String
    ) {
        val textoMinus = texto.lowercase()
        val afirmativo = listOf("si", "sí", "confirmar", "claro", "vale", "aceptar", "guardar")
        val negativo = listOf("no", "cancelar", "parar", "borrar")

        when {
            afirmativo.any { textoMinus.contains(it) } -> {
                try {
                    val nuevoProducto = Producto(
                        idTendero = idTendero,
                        categoria = categoriaTmp,
                        nombre = nombreTmp,
                        presentacion = presentacionTmp
                    )

                    val respuesta = withContext(Dispatchers.IO) {
                        ConexionServiceTienda.create().addProducto(nuevoProducto)
                    }

                    if (respuesta.isSuccessful) {
                        enviarMensajeSistema(modelo("¡Excelente! El producto $nombreTmp ha sido guardado."))
                        finalizarFlujo()
                    } else {
                        enviarMensajeSistema(modelo("Hubo un error en el servidor al guardar el producto."))
                    }
                } catch (e: Exception) {
                    enviarMensajeSistema(modelo("No pude conectarme al servidor. Verifica tu internet."))
                }
            }
            negativo.any { textoMinus.contains(it) } -> {
                enviarMensajeSistema(modelo("Operación cancelada. No se guardó el producto."))
                finalizarFlujo()
            }
            else -> {
                enviarMensajeSistema(modelo("No te entendí. Di 'Sí' para guardar el producto o 'No' para cancelar."))
            }
        }
    }

    private fun finalizarFlujo() {
        estadoActual = EstadoProducto.INACTIVO
        categoriaTmp = ""
        nombreTmp = ""
        presentacionTmp = ""
    }
}