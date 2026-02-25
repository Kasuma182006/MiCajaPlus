package com.example.micaja.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.micaja.ConexionService.ConexionServiceTienda
import com.example.micaja.models.ConsultarOperaXFecha
import com.example.micaja.models.TipoOperacionXFecha
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class OperacionesViewModel : ViewModel() {

    // Estadísticas generales (ventas, gastos, costos)
    private val _estadisticas = MutableLiveData<TipoOperacionXFecha?>()
    val estadisticas: LiveData<TipoOperacionXFecha?> get() = _estadisticas

    // Número de créditos como entero
    private val _numeroCreditos = MutableLiveData<Int?>()
    val numeroCreditos: LiveData<Int?> get() = _numeroCreditos

    val mensajeError = MutableLiveData<String>()

    fun consultarEstadisticas(idTendero: String, fechaInicial: String, fechaFin: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val api = ConexionServiceTienda.create()
                val request = ConsultarOperaXFecha(idTendero, fechaInicial, fechaFin)

                val responseEstadisticas = api.consultarXFecha(request)
                if (responseEstadisticas.isSuccessful) {
                    val lista = responseEstadisticas.body()
                    if (!lista.isNullOrEmpty()) {
                        _estadisticas.postValue(lista.first())
                    } else {
                        mensajeError.postValue("No se encontraron estadísticas en ese rango")
                    }
                } else {
                    mensajeError.postValue("Error en estadísticas: ${responseEstadisticas.code()}")
                }
            } catch (e: Exception) {
                mensajeError.postValue("Error: ${e.message}")
            }
        }
    }

    fun consultarNumeroCreditos(idTendero: String, fechaInicial: String, fechaFin: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val api = ConexionServiceTienda.create()
                val request = ConsultarOperaXFecha(idTendero, fechaInicial, fechaFin)

                val responseCreditos = api.numeroCredito(request)
                if (responseCreditos.isSuccessful) {
                    val cantidad = responseCreditos.body()?.Ncredito?.toIntOrNull()
                    _numeroCreditos.postValue(cantidad)
                } else {
                    mensajeError.postValue("Error en número de créditos: ${responseCreditos.code()}")
                }
            } catch (e: Exception) {
                mensajeError.postValue("Error: ${e.message}")
            }
        }
    }
}