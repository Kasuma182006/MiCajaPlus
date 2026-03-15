package com.example.micaja.models

import com.google.gson.annotations.SerializedName

data class Tendero(
    @SerializedName("cedula") val cedula: String,
    @SerializedName("telefono") val telefono: String,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("fechaCreacion") val fechaCreacion: String
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
    val fechaFin: String? = null
)

data class TipoOperacionXFecha(
    val ventas: String,
    val valorCredito: String,
    val ncreditos: String,
    val costos: String,
    val gastos: String
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
    val idTendero: String,
    val cedula: String ? = null,
    val mensaje: String,
    val tipoPago: String,
    val nombre: String,
    val cantidad: Int,
    val presentacion: String
)

data class cantidadIn (
    val idTendero: String,
    val cantidad: Int,
    val presentacion: String,
    val nombre: String
)

data class AgregarProducto(
    val idTendero: String,
    val nombre: String,
    val presentacion: String,
    val cantidad: Int,
    val valorVenta: Int,
    val idCategoria: Int,
    val valorCompra: Int
)

data class Gasto(
    val idTendero: String,
    val mensaje: String,
    var valor: Int
)

data class costoDetectado(
    val idTendero: String,
    val mensaje: String,
    val precioCompra: Int,
    val proveedor: String
)


data class gastoDetectado(
    val idTendero: String,
    val mensaje: String,
    val precio: Int
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
    val idTendero: String,
    val idInventario: Int,
    val nombreProducto: String,
    val presentacion: String,
    val valorVenta: Int,
    val valorCompra:Int
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

data class Producto(
    val idTendero: String,
    val categoria: String,
    val nombre: String,
    val presentacion: String
)


