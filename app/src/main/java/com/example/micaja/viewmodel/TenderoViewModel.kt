package com.example.micaja.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.micaja.models.modelo

class TenderoViewModel: ViewModel() {

    val mensajes = MutableLiveData<MutableList<modelo>>()
    val mensajesSistema = MutableLiveData<MutableList<modelo>>()

    fun addMensaje(mensaje:modelo){

        val list = mensajes.value?: mutableListOf()
        list.add(mensaje)
        mensajes.value = list
    }

    fun addMensajeSistema(mensaje:modelo){

        val list = mensajesSistema.value?: mutableListOf()
        list.add(mensaje)
        mensajesSistema.value = list
    }


}