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

data class clienteNuevo(
    val cedulaCliente: String,
    val cedulaTendero:String,
    val nombre: String,
    val celular: String,
    val monto: Int
)

data class Identificacion(
    val cedula: String,
    val tendero: String
)

data class Credito(
    val cedulaCliente: String,
    val monto: Int,
    val cedulaTendero: String
)

data class Datos_Abono(
    val cedulaCliente: String,
    val cedulaTendero:String,
    val monto: Int
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

data class modeloOperaciones (
    val idTendero: String,
    val tipo: Int,
    val monto: String,
    val mensaje: String
)

data class listaProductos (
    val idTendero: String,
    val nombreProducto: String,
    val cantidad: Int,
    val precioCompra: Int,
    val precioVenta: Int,
    val proveedor: String
)


data class Inventario (

    val idInventario: Int,
    val idProductos: Int,
    val cantidad: Int,
    val valorVenta: Int,
    val valorCompra: Int,
    val idCategorias: Int,
    val nombre: String,
    val presentacion: String
)