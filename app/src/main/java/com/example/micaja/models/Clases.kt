package com.example.micaja.models

import com.google.gson.annotations.SerializedName

data class Tendero(
    @SerializedName("cedula") val cedula: String,
    @SerializedName("telefono") val telefono: String,
    @SerializedName("nombre") val nombre: String
)


data class ConsultaCedulaTendero(
    val cedula: String
)

data class cliente(
    val cedula: String?=null,
    val nombre: String?=null,
    val celular: String? =null,
    val creditos: Int?=null,
    val total: Int?=null
)

////ABONOS
data class clienteAbono(
    val nombre: String?,
    val cedula: String?,
    val saldo: Int?
)

/////ABONOS
data class DatosAbono(
    val cedula: String,
    val idTendero:String,
    val abono: Int
)
//////ABONOS
data class RespuestaAbono(
    val nuevoSaldo: Int
)


data class cliente1(
    val nombre: String?=null,
    val saldo: Int?=null,
)

data class clienteNuevo(
    val cedulaCliente: String,
    val cedulaTendero:String,
    val nombre: String,
    val celular: String,

)

data class Identificacion(
    val cedula: String,
    val idTendero: String
)

data class Credito(
    val cedulaCliente: String,
    val monto: Int,
    val cedulaTendero: String
)
data class ConsultarOperaXFecha(
    val idTendero: String,
    val fechaInicial: String? = null,
    val fechaFin: String? = null,
    val precio: Int? = null
)

data class TipoOperacionXFecha(
    val ventas: String,
    val costos: String,
    val gastos: String
)

data class NumeroCreditosResponse(
    val Ncredito: String
)

data class ModeloBase(
    val idTendero: String,
    val baseInicial: Int
)

data class Venta(
    var nombreProducto: String ?= null,
    val presentacion: String ?= null,
    val cantidad: Int ?= null,
    val idTendero: String ?= null,
    val idcliente: String ?=null
)



data class ventaDetectada(
    var idTendero: String,
    val mensaje: String,
    val tipoPago: String,
    val nombre: String,
    val cantidad: Int
)

data class Gasto(
    val idTendero: String,
    val mensaje: String,
    var valor: Int
)

data class gastoDetectado(
    val mensaje: String,
    val precio: Int
)

data class Costo(
    val idTendero: String,
    val mensaje: String,
    var precioCompra: Int
)

data class compra_Mercancia(
    val idTendero: String = "",
    val nombre: String,
    val presentacion: String,
    val cantidadStock: Int,
    val precioCompra: Int,
    val proveedor: String
)




//Esta data class es para las operaciones de ventas de unidades o agregar unidades existentes en el inventario (ventas o gastos en operciones)
data class OperacionesInventario(
    val idTendero: String,
    val nombre: String,
    val presentacion: String,
    val cantidad: Int,
    val operacion: String // en operacion se agrega la palabra "descontar" en caso de que sea una venta o "agregar" en caso de ser un costo
)





// No toquen esto, estas data class son para el modulo de editar Productos

data class BuscarProductos(
    val idTendero: String,
    val nombre: String
)

data class EditarProducto(
    val cantidad: Int,
    val idInventario: Int,
    val idProductos: Int,
    val nombre: String,
    val presentacion: String,
    val valorVenta: Int
)

//data clas para traer datos del cliente
data class ClienteCompleto(
    val nombre: String?,
    val cedula: String?,
    val telefono: String?,
    val saldo: Int?
)
//data clas para actualizar datos del cliente
data class ActualizarCliente(
    val idTendero: String,
    val cedula: String,
    val nombre: String,
    val telefono: String,
    val saldo: Int
)


