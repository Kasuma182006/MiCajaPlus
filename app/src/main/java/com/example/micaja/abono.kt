package com.example.micaja
import com.example.micaja.ConexionService.ConexionServiceTienda
import com.example.micaja.models.DatosAbono
import com.example.micaja.models.Identificacion
import com.example.micaja.models.modelo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class Abonos {
    private enum class EstadoAbono {
        INACTIVO, ESPERANDO_CEDULA, ESPERANDO_MONTO, ESPERANDO_CONFIRMACION
    }
    private var estadoActual = EstadoAbono.INACTIVO
    private var cedulaGuardada = ""
    private var nombreCliente = ""
    private var montoAbono = 0
    private var saldoActualCliente= 0

    fun iniciarFlujoAbono(enviarMensajeSistema: (modelo) -> Unit) {
        estadoActual = EstadoAbono.ESPERANDO_CEDULA
        enviarMensajeSistema(
            modelo("He detectado un abono. Por favor dicte la cédula del cliente."))
    }
    suspend fun procesarRespuesta(
        texto: String,
        enviarMensajeSistema: (modelo) -> Unit,
        idTendero: String): Boolean {
        if (estadoActual == EstadoAbono.INACTIVO) return false
        val textoLimpio = texto.lowercase().trim()

        when (estadoActual) {
            EstadoAbono.ESPERANDO_CEDULA -> {
                consultarClienteEnServidor(textoLimpio, enviarMensajeSistema, idTendero)
            }
            EstadoAbono.ESPERANDO_MONTO -> {
                validarMonto(textoLimpio, enviarMensajeSistema)
            }
            EstadoAbono.ESPERANDO_CONFIRMACION -> {
                validarConfirmacion(textoLimpio, enviarMensajeSistema, idTendero)
            }
            EstadoAbono.INACTIVO -> {
            }
        }
        return true
    }
    private suspend fun consultarClienteEnServidor(
        texto: String,
        enviarMensajeSistema: (modelo) -> Unit,
        idTendero: String){
        val cedulaLimpia = texto.replace(Regex("[^0-9]"), "")

        if (cedulaLimpia.length >= 7) {
            try {
                val respuesta = withContext(Dispatchers.IO) {
                    ConexionServiceTienda.create().consultarCliente(Identificacion(cedulaLimpia, idTendero))
                }
                if (respuesta.isSuccessful && respuesta.body()?.nombre != null) {
                    val cliente = respuesta.body()!!
                    val saldoRecibido = cliente.saldo ?: 0

                    if (saldoRecibido <= 0) {
                        enviarMensajeSistema(modelo("El cliente ${cliente.nombre} no tiene saldos pendientes."))
                        finalizarFlujo()
                        return
                    }

                    cedulaGuardada = cliente.cedula ?: cedulaLimpia
                    nombreCliente = cliente.nombre!!
                    this.saldoActualCliente = cliente.saldo ?: 0
                    estadoActual = EstadoAbono.ESPERANDO_MONTO

                    val mensaje = """
                        Cliente: $nombreCliente
                        Cédula: $cedulaGuardada
                        Saldo Pendiente: $$saldoActualCliente
                        
                        ¿Cuánto desea abonar el cliente?
                    """.trimIndent()
                    enviarMensajeSistema(modelo(mensaje))
                } else {
                    enviarMensajeSistema(modelo("No se encontró deuda para la cédula $cedulaLimpia."))
                    finalizarFlujo()
                }
            } catch (e: Exception) {
                enviarMensajeSistema(modelo("No se pudo verificar el cliente."))
            }
        } else {
            enviarMensajeSistema(modelo("La cédula debe ser numérica y tener al menos 7 dígitos."))
        }
    }
    private fun validarMonto(texto: String, enviarMensajeSistema: (modelo) -> Unit) {
        val monto = texto.replace(Regex("[^0-9]"), "").toIntOrNull()
        if (monto != null && monto > 0) {
            if (monto > saldoActualCliente) {
                enviarMensajeSistema(
                    modelo("El cliente solo debe $$saldoActualCliente. No puede abonar $$monto. Por favor dicte un valor válido."))
            } else {
                montoAbono = monto
                estadoActual = EstadoAbono.ESPERANDO_CONFIRMACION
                enviarMensajeSistema(
                    modelo("¿Confirma el abono de $$montoAbono para $nombreCliente? (Responda Sí o No)"))
            }
        } else {
            enviarMensajeSistema(modelo("No entendí el valor. Por favor, diga el monto en números."))
        }
    }
    private suspend fun validarConfirmacion(
        texto: String,
        enviarMensajeSistema: (modelo) -> Unit,
        idTendero: String
    ) {
        val afirmativo = listOf("si", "sí", "confirmar", "confirmo", "claro", "aceptar", "vale")
        val negativo = listOf("no", "cancelar", "incorrecto", "parar")
        when {
            afirmativo.any { texto.contains(it) } -> {
                try {
                    // Preparamos los datos para el servidor
                    val solicitud = DatosAbono(
                        cedula = cedulaGuardada,
                        idTendero = idTendero,
                        abono = montoAbono
                    )
                    val respuesta = withContext(Dispatchers.IO) {
                        ConexionServiceTienda.create().registrarAbono(solicitud)
                    }
                    if (respuesta.isSuccessful && respuesta.body() != null) {
                        val body = respuesta.body()!!
                        val mensajeExito = """
                            Abono registrado con exito.
                            Cliente: $nombreCliente
                            Monto: $$montoAbono
                            Nuevo Saldo: ${body.nuevoSaldo}
                        """.trimIndent()
                        enviarMensajeSistema(modelo(mensajeExito))
                        finalizarFlujo()
                    } else {
                        enviarMensajeSistema(modelo("No se pudo procesar el abono. Intente más tarde."))
                    }
                } catch (e: Exception) {
                    enviarMensajeSistema(modelo("No se pudo conectar con el servidor."))
                }
            }
            negativo.any { texto.contains(it) } -> {
                enviarMensajeSistema(modelo("Operación cancelada. No se realizó ningún cargo."))
                finalizarFlujo()
            }
            else -> {
                enviarMensajeSistema(modelo("No te entendí. Di 'Sí' para confirmar o 'No' para cancelar el abono de $$montoAbono."))
            }
        }
    }
    private fun finalizarFlujo() {
        estadoActual = EstadoAbono.INACTIVO
        cedulaGuardada = ""
        nombreCliente = ""
        montoAbono = 0
        saldoActualCliente = 0
    }
}
