package com.example.micaja.ConexionService

import com.example.micaja.models.ConsultarOperaXFecha
import com.example.micaja.models.Credito
import com.example.micaja.models.Datos_Abono
import com.example.micaja.models.Identificacion
import com.example.micaja.models.ModeloBase
import com.example.micaja.models.NumeroCreditosResponse
import com.example.micaja.models.Tendero
import com.example.micaja.models.TipoOperacionXFecha
import com.example.micaja.models.cliente
import com.example.micaja.models.clienteNuevo
import com.example.micaja.models.modeloOperaciones
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query

interface ConexionServiceTienda {

    @POST("login")
    @Headers("Content-Type: application/json")
    suspend fun login(@Body body: Tendero): Response<Tendero>

    @POST("addtendero")
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

    @POST("ConsultarEstadisticas")
    suspend fun consultarXFecha(@Body request: ConsultarOperaXFecha): Response<List<TipoOperacionXFecha>>

    @POST ("numerocredito")
    suspend fun numeroCredito(@Body request: ConsultarOperaXFecha): Response<NumeroCreditosResponse>

    @POST("agregarBase")
    suspend fun addBase(@Body base: ModeloBase): Response<Map <String,Any>>

    @POST("agregarOperacion")
    suspend fun addOperacion(@Body operacion : modeloOperaciones): Response<Map<String,Any>>

    companion object messi {
        private const val BASE_URL = "http://54.156.114.70:65021"

        fun create(): ConexionServiceTienda {

            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            return retrofit.create(ConexionServiceTienda::class.java)

        }
    }
}