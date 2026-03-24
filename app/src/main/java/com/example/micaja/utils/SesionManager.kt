package com.example.micaja.utils

import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.micaja.Login

object SesionManager {
    private const val PREF_NAME = "SesionTendero"
    private const val KEY_CEDULA = "cedula"
    private const val KEY_TIEMPO_LOGIN = "tiempo_login"

    private const val TIEMPO_MAXIMO_SESION = 12 * 60 * 60 * 1000L

    fun iniciarSesion(context: Context, cedula: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putString(KEY_CEDULA, cedula)
        editor.putLong(KEY_TIEMPO_LOGIN, System.currentTimeMillis())
        editor.apply()
    }

    fun esSesionValida(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val tiempoLogin = prefs.getLong(KEY_TIEMPO_LOGIN, 0L)

        if (tiempoLogin == 0L) return false // Nunca se ha logueado
        val tiempoActual = System.currentTimeMillis()
        val tiempoTranscurrido = tiempoActual - tiempoLogin

        return tiempoTranscurrido <= TIEMPO_MAXIMO_SESION
    }

    fun cerrarSesion(context: Context, mensaje: String? = null) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()

        val estadoTienda = context.getSharedPreferences("EstadoTienda", Context.MODE_PRIVATE)
        estadoTienda.edit().putBoolean("abierta", false).apply()
        val estadoBase = context.getSharedPreferences("EstadoBase", Context.MODE_PRIVATE)
        estadoBase.edit().putBoolean("base", false).apply()

        // Si pasamos un mensaje, mostramos ese. Si no, el de seguridad por defecto.
        val textoMostrar = mensaje ?: "Tu sesión ha expirado por seguridad (12 horas)."
        Toast.makeText(context, textoMostrar, Toast.LENGTH_SHORT).show()

        val intent = Intent(context, Login::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        context.startActivity(intent)
    }
}