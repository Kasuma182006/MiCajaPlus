package com.example.micaja.ConexionService

import com.example.micaja.models.ActualizarCliente
import com.example.micaja.models.AgregarProducto
import com.example.micaja.models.BuscarProductos
import com.example.micaja.models.ClienteCompleto
import com.example.micaja.models.ConsultaCedulaTendero
import com.example.micaja.models.ConsultarOperaXFecha
import com.example.micaja.models.Credito
import com.example.micaja.models.DatosAbono
import com.example.micaja.models.EditarProducto
import com.example.micaja.models.Identificacion
import com.example.micaja.models.ModeloBase
import com.example.micaja.models.OperacionesInventario
import com.example.micaja.models.RespuestaAbono
import com.example.micaja.models.Tendero
import com.example.micaja.models.TipoOperacionXFecha
import com.example.micaja.models.cantidadIn
import com.example.micaja.models.cliente1
import com.example.micaja.models.clienteAbono
import com.example.micaja.models.clienteNuevo
import com.example.micaja.models.compra_Mercancia
import com.example.micaja.models.gastoDetectado
import com.example.micaja.models.ventaDetectada
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface ConexionServiceTienda {

    //LO RELACIONADO CON EL TENDERO
    @POST("/login")
    @Headers("Content-Type: application/json")
    suspend fun login(@Body body: Tendero): Response<Tendero>
    @POST("/addtendero")
    @Headers("Content-Type: application/json")
    suspend fun addTendero(@Body tendero: Tendero): Response<Tendero>
    @POST("/consultaCedulaTendero")
    suspend fun buscarTendero(@Body consulta: ConsultaCedulaTendero): Response<Tendero>
    @POST("/agregarBase")
    suspend fun addBase(@Body base: ModeloBase): Response<Map<String, Any>>

    //OPERACIONES BÁSICAS DE LA TIENDA
    @POST("/crearventas")
    suspend fun ventaDetectada(@Body venta: ventaDetectada): Response<ventaDetectada>
    @POST("/creargasto")
    suspend fun gastoDetectadoss(@Body body: gastoDetectado): Response<gastoDetectado>
    @POST("/crearcosto")
    suspend fun compra_Mercancia(@Body request: compra_Mercancia): Response<compra_Mercancia>

    /////CREDITOS Y ABONOS
    @POST("/abonos")
    suspend fun registrarAbono(@Body datos: DatosAbono): Response<RespuestaAbono>
    @POST("/consultarcliente")
    suspend fun consultarCliente(@Body datos: Identificacion): Response<clienteAbono>
    @POST("/buscar_cliente")
    @Headers("Content-Type: application/json")
    suspend fun busca_cliente(@Body identificacion: Identificacion): Response<cliente1>
    @POST("/insertar_cliente")
    suspend fun insertar_cliente(@Body cliente: clienteNuevo): Response<Boolean>
    @POST("/insertar_credito")
    suspend fun insertar_credito(@Body credito: Credito): Response<Void>
    @POST("/registrar_credito")
    suspend fun registrarCredito(@Body venta: ventaDetectada): Response<String>
    @POST("/consultarEstadisticas")
    suspend fun consultarXFecha(@Body request: ConsultarOperaXFecha): Response<TipoOperacionXFecha>


    //INVENTARIO
    @POST("/agregarProducto")
    suspend fun addProducto(@Body body: AgregarProducto): Response<AgregarProducto>

    @POST("/editarProducto")
    suspend fun editarProducto(@Body producto: EditarProducto): Response<Map<String, String>>

    @POST("/sugerirProductos")
    suspend fun buscarProductos(@Body nombre: BuscarProductos): Response<List<EditarProducto>>

    @POST("/operacionesInventario")
    suspend fun operacionesInventario(@Body body: OperacionesInventario): Response<OperacionesInventario>

    @POST("/cantidadProducto")
    suspend fun cantidadProducto(@Body body: cantidadIn): Response<cantidadIn>


    //EDITAR CLIENTE
    @POST("/consultarcliente")
    suspend fun consultarClienteEdit(@Body datos: Identificacion): Response<ClienteCompleto>
    @POST("/actualizar_cliente")
    suspend fun actualizarCliente(@Body datos: ActualizarCliente): Response<Map<String, String>>


    companion object messi {
        private const val BASE_URL = "http://192.168.1.27:4000"
        fun create(): ConexionServiceTienda {
            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            return retrofit.create(ConexionServiceTienda::class.java)
        }
    }
}