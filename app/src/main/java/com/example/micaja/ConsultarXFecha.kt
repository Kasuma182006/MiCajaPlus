package com.example.micaja.Calendario


import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.micaja.Principal
import com.example.micaja.R
import com.example.micaja.chat_Tienda
import com.example.micaja.databinding.ConsultarXFechaBinding
import com.example.micaja.viewmodel.OperacionesViewModel
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.CompositeDateValidator
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.time.LocalDate

class ConsultarXFecha : AppCompatActivity() {

    private var fechaInicial: String? = null
    private var fechaFinal: String? = null


    private lateinit var binding: ConsultarXFechaBinding
    private val operacionesViewModel: OperacionesViewModel by viewModels()
//    private val creditoViewModel: CreditoViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ConsultarXFechaBinding.inflate(layoutInflater)
        setContentView(binding.root)


        configurarBotones()
        observarDatos()
//        cancelar()
    }

    private fun configurarBotones() {
        binding.cvBtnInicio.setOnClickListener { mostrarCalendario(true) }

        binding.cvBtnFin.setOnClickListener { mostrarCalendario(false) }

        binding.btnCerrar.setOnClickListener { finish() }
    }

//    private fun cancelar() {
//        Toast.makeText(this, "Cancelaste la operación", Toast.LENGTH_SHORT).show()
//        startActivity(Intent(this, chat_Tienda::class.java))
//        finish()
//    }

    private fun consultarBalance(idTendero: String, fechaInicial: String, fechaFin: String) {
        operacionesViewModel.consultarEstadisticas(idTendero, fechaInicial, fechaFin)
    }

    fun observarDatos() {
        operacionesViewModel.estadisticas.observe(this) { datos ->
            if (datos != null) {
                val ventas = datos.ventas?.toIntOrNull() ?: 0
                val gastos = datos.gastos?.toIntOrNull() ?: 0
                val costos = datos.costos?.toIntOrNull() ?: 0
                val ncreditos = datos.ncreditos?.toIntOrNull() ?: 0
                val valorCredito = datos.valorCredito?.toIntOrNull() ?: 0



                val utilidad  = (ventas + valorCredito) - (gastos + costos)

                binding.tvVentas.text   = "$$ventas"
                binding.tvGastos.text   = "$$gastos"
                binding.tvCostos.text   = "$$costos"
                binding.tvFiados.text   = "$ncreditos"


                // Formatear texto de utilidad (ejemplo simple)
                val utilidadTexto = "$$utilidad"
                binding.tvUtilidad.text = utilidadTexto

                // Cambiar color según si es negativo o no
                if (utilidad < 0) {
                    // Usando color definido en colors.xml
                    binding.tvUtilidad.setTextColor(ContextCompat.getColor(this, R.color.utilidad_negative))
                    // O con Color.RED:
                    // binding.tvUtilidad.setTextColor(Color.RED)
                } else {
                    binding.tvUtilidad.setTextColor(ContextCompat.getColor(this, R.color.utilidad_positive))
                }
            } else {
                binding.tvVentas.text = "Ventas: 0"
                binding.tvGastos.text = "Gastos: 0"
                binding.tvCostos.text = "Costos: 0"
                binding.tvUtilidad.text = "Utilidad: 0"
                binding.tvUtilidad.setTextColor(ContextCompat.getColor(this, R.color.utilidad_positive))
            }
        }

        operacionesViewModel.numeroCreditos.observe(this) { cantidad ->
            val texto = "Clientes con crédito: ${cantidad ?: 0}"
            binding.tvFiados.text = texto
        }


        operacionesViewModel.mensajeError.observe(this) { error ->
            Toast.makeText(this, error, Toast.LENGTH_LONG).show()
        }
    }

    private fun mostrarCalendario(esFechaInicial: Boolean) {
        val prefs = getSharedPreferences("SesionTendero", Context.MODE_PRIVATE)


        val hoyMs = MaterialDatePicker.todayInUtcMilliseconds()

        // 2. Obtener la fecha de registro (Mínimo) y convertirla a Long
        val fechaRegistroStr = prefs.getString("fecha", null)
        Log.d("fecha", fechaRegistroStr.toString())
        val fechaMinimaMs: Long = if (fechaRegistroStr != null) {
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                sdf.timeZone = TimeZone.getTimeZone("UTC")
                sdf.parse(fechaRegistroStr)?.time ?: hoyMs //convertidor de string a Date
            } catch (e: Exception) {
                hoyMs
            }
        } else {
            hoyMs
        }

        // 3. Definir las RESTRICCIONES (Antes de crear el picker)
        val constraints = CalendarConstraints.Builder()
            .setStart(fechaMinimaMs) // Mes donde empieza el calendario
            .setEnd(hoyMs)           // Mes donde termina
            .setValidator(CompositeDateValidator.allOf(listOf(
                DateValidatorPointForward.from(fechaMinimaMs), // Bloquea antes del registro
                DateValidatorPointBackward.before(hoyMs)       // Bloquea después de hoy
            )))
            .build()

        // 4. Construir el Picker usando las restricciones
        val dateRangePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Selecciona la fecha")
            .setCalendarConstraints(constraints) // Aplicamos los bloqueos aquí
            .build()

        dateRangePicker.addOnPositiveButtonClickListener { selection ->
            // "selection" ya es un Long con los milisegundos de la fecha elegida
            val formatoEnvio = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            formatoEnvio.timeZone = TimeZone.getTimeZone("UTC")

            val fechaSeleccionada = formatoEnvio.format(Date(selection))

            // Lógica de asignación de fechas inicial/final
            if (esFechaInicial) {
                fechaInicial = "$fechaSeleccionada 00:00:00"
            } else {
                fechaFinal = "$fechaSeleccionada 23:59:59"
            }

            // Ajustar nulos
            if (fechaInicial != null && fechaFinal == null) {
                fechaFinal = fechaInicial!!.replace("00:00:00", "23:59:59")
            } else if (fechaFinal != null && fechaInicial == null) {
                fechaInicial = fechaFinal!!.replace("23:59:59", "00:00:00")
            }

            // Actualizar UI
//            val fechaInicioSoloDia = fechaInicial?.substring(0, 10) ?: ""
//            val fechaFinSoloDia = fechaFinal?.substring(0, 10) ?: ""


            Log.d("fecha", fechaInicial.toString())
            Log.d("fecha", fechaFinal.toString())


            // Consultar balance
            val tendero = prefs.getString("cedula", null)
            if (!tendero.isNullOrEmpty()) {
                consultarBalance(tendero, fechaInicial!!, fechaFinal!!)
            }
        }

        dateRangePicker.show(supportFragmentManager, "DATE_RANGE_PICKER")
    }
}