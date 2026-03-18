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

    private val _estadisticas = MutableLiveData<TipoOperacionXFecha?>()
    val estadisticas: LiveData<TipoOperacionXFecha?> get() = _estadisticas
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

                    if (!lista.isNullOrEmpty()) { _estadisticas.postValue(lista.first()) }
                    else { mensajeError.postValue("No existen datos en ese rango de fechas") }
                }
            } catch (e: Exception) { mensajeError.postValue("Error de conexion intenta mas tarde") }
        }
    }
}