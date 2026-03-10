package com.example.micaja.models

import com.google.gson.annotations.SerializedName

data class Tendero(
    @SerializedName("cedula") val cedula: String,
    @SerializedName("telefono") val telefono: String,
    @SerializedName("nombre") val nombre: String
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
    val monto: Int
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

data class venta(
    val idTendero: String,
    val tipo: String,
    val monto: Int,
    val mensaje: String
)

data class ventaDetectada(
    var nombreProducto: String,
    val mensaje: String,
    val total_Venta: Int
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
    val idTendero: String,
    var mensaje: String,
    var monto: Int,
    val categoria: String,
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
    val nombre: String  //Nombre es el nombre del producto que el tendero agrega en el input a la hora de buscar los productos
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





data class consultarIn(
    val nombre: String
)