package com.example.micaja.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.micaja.ConexionService.ConexionServiceTienda
import com.example.micaja.models.ConsultarOperaXFecha
import com.example.micaja.models.TipoOperacionXFecha
import com.example.micaja.models.consultarTenderoFecha
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class OperacionesViewModel : ViewModel() {
    private val _estadisticas = MutableLiveData<TipoOperacionXFecha?>()
    val estadisticas: LiveData<TipoOperacionXFecha?> get() = _estadisticas
    val mensajeError = MutableLiveData<String>()

    fun consultarEstadisticas(idTendero: String, fechaInicial: String, fechaFin: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val api = ConexionServiceTienda.create()
                val request = ConsultarOperaXFecha(idTendero, fechaInicial, fechaFin)
                val responseEstadisticas = api.consultarXFecha(request)

                if (responseEstadisticas.isSuccessful) {
                    val datos = responseEstadisticas.body()

                    if (datos != null) {
                        _estadisticas.postValue(datos)
                    } else {
                        mensajeError.postValue("No existen datos en ese rango de fechas")
                    }
                } else {
                    mensajeError.postValue("Error en la respuesta del servidor")
                }

            } catch (e: Exception) {
                mensajeError.postValue("Error de conexion intenta mas tarde")
            }
        }
    }
    private val _fechaRegistro = MutableLiveData<String?>()
    val fechaRegistro: LiveData<String?> get() = _fechaRegistro

    fun obtenerFechaRegistro(idTendero: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val api = ConexionServiceTienda.create()
                val request = consultarTenderoFecha(idTendero)
                val respuesta = api.consultarTenderoFecha(request)

                if (respuesta.isSuccessful && respuesta.body() != null) {
                    // Aquí tomamos la fecha
                    _fechaRegistro.postValue(respuesta.body()?.fecha)
                }
            } catch (e: Exception) {
                _fechaRegistro.postValue(null)
            }
        }
    }
}