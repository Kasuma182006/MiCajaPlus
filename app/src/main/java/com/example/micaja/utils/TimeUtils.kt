package com.example.micaja.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utilidad para obtener la hora actual formateada.
 * Se usa en el chat para registrar el timestamp de cada mensaje.
 *
 * Separado de chat_Tienda.kt para mantener ese archivo limpio
 * y poder reutilizar esta función desde cualquier parte del proyecto.
 */
object TimeUtils {

    /**
     * Retorna la hora actual del dispositivo como texto.
     * Formato: "hh:mm a"  →  ej: "03:45 PM" / "11:02 AM"
     *
     * @return String con la hora formateada según el idioma del dispositivo
     */
    fun horaActual(): String =
        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
}
