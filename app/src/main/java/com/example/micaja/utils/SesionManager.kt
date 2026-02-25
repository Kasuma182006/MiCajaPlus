package com.example.micaja.utils

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.widget.Toast
import com.example.micaja.Login

object SesionManager {

    fun cerrarSesion(context: Context) {
        val prefs = context.getSharedPreferences("SesionTendero", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        Toast.makeText(context, "Sesión cerrada", Toast.LENGTH_SHORT).show()

        val intent = Intent(context, Login::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        context.startActivity(intent)

        val estadoTienda =  context.getSharedPreferences("EstadoTienda",MODE_PRIVATE)
        val editor = estadoTienda.edit()
        editor.putBoolean("abierta", false)
        editor.apply()

        val estadoBase = context.getSharedPreferences("EstadoBase",MODE_PRIVATE)
        estadoBase.edit().putBoolean("base",false).apply()
    }
}