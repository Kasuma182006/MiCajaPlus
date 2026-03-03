package com.example.micaja.ConexionService

import com.example.micaja.models.BuscarProductos
import com.example.micaja.models.ConsultarOperaXFecha
import com.example.micaja.models.Costo
import com.example.micaja.models.Credito
import com.example.micaja.models.Datos_Abono
import com.example.micaja.models.EditarProducto
import com.example.micaja.models.Gasto
import com.example.micaja.models.Identificacion
import com.example.micaja.models.ModeloBase
import com.example.micaja.models.NumeroCreditosResponse
import com.example.micaja.models.OperacionesProductos
import com.example.micaja.models.Tendero
import com.example.micaja.models.TipoOperacionXFecha
import com.example.micaja.models.cliente
import com.example.micaja.models.clienteNuevo
import com.example.micaja.models.compra_Mercancia
import com.example.micaja.models.venta
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.PUT

interface ConexionServiceTienda {

    @POST("/login")
    @Headers("Content-Type: application/json")
    suspend fun login(@Body body: Tendero): Response<Tendero>

    @POST("/addtendero")
    @Headers("Content-Type: application/json")
    suspend fun addTendero(@Body tendero: Tendero): Response<Tendero>

    @POST("/buscar_cliente")
    @Headers("Content-Type: application/json")
    suspend fun busca_cliente(@Body identificacion: Identificacion): List<cliente>

    @POST("/insertarcliente_credito")
    suspend fun insertar_cliente(@Body cliente: clienteNuevo): Response<Void>

    @POST("/insertar_credito")
    suspend fun insertar_credito(@Body credito: Credito): Response<Void>

    @PUT("/abono")
    suspend fun abono(@Body abono: Datos_Abono): Response<Void>

    @POST("/ConsultarEstadisticas")
    suspend fun consultarXFecha(@Body request: ConsultarOperaXFecha): Response<List<TipoOperacionXFecha>>

    @POST("/venta")
    suspend fun venta(@Body venta: venta): Response<venta>

    @POST("/Gasto")
    suspend fun Gasto(@Body gasto: Gasto): Response<Gasto>

    @POST ("/Costo")
    suspend fun compra_Mercancia(@Body request: compra_Mercancia): Response<Costo>

    @POST ("/numerocredito")
    suspend fun numeroCredito(@Body request: ConsultarOperaXFecha): Response<NumeroCreditosResponse>

    @POST("/agregarBase")
    suspend fun addBase(@Body base: ModeloBase): Response<Map <String,Any>>


    @POST("/descargaProductos")
    suspend fun descargaProductos(@Body producto: OperacionesProductos): Response<Any>

    @POST("/buscarProducto")
    suspend fun buscarProducto(@Body producto: BuscarProductos): Response<List<EditarProducto>>

    @POST("editarProducto")
    suspend fun editarProducto(@Body producto: EditarProducto): Response <Any>

    companion object messi {
<<<<<<< HEAD
        private const val BASE_URL = "http://10.6.124.62:4000"
        var inventario = MutableStateFlow<List<inventario>>(emptyList())
=======
        private const val BASE_URL = "http://10.6.125.229:4000"

>>>>>>> 354347b5210e4031cb271c0c87e3ec00923287d3

        fun create(): ConexionServiceTienda {
            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            return retrofit.create(ConexionServiceTienda::class.java)
        }



    }
}