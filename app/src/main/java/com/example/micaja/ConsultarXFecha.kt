package com.example.micaja.Calendario

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.micaja.R
import com.example.micaja.databinding.ConsultarXFechaBinding
import com.example.micaja.viewmodel.OperacionesViewModel
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.CompositeDateValidator
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class ConsultarXFecha : AppCompatActivity() {

    var cedulaGlobal = ""
    private var fechaInicial: String? = null
    private var fechaFinal: String? = null
    private lateinit var binding: ConsultarXFechaBinding
    private val operacionesViewModel: OperacionesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ConsultarXFechaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.header) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(view.paddingLeft, systemBars.top, view.paddingRight, view.paddingBottom)
            insets
        }
        val preferencia = getSharedPreferences("SesionTendero", MODE_PRIVATE)
        val cedula = preferencia.getString("cedula", null)
        cedulaGlobal = cedula.toString()
        configurarBotones()
        observarDatos()

        if (cedula != null) { operacionesViewModel.obtenerFechaRegistro(cedula) }
    }

    private fun configurarBotones() {
        binding.cvBtnInicio.setOnClickListener { mostrarCalendario(true) }
        binding.cvBtnFin.setOnClickListener { mostrarCalendario(false) }
        binding.btnCerrar.setOnClickListener { finish() }
    }

    private fun consultarBalance(idTendero: String, fechaInicial: String, fechaFin: String) {
        operacionesViewModel.consultarEstadisticas(idTendero, fechaInicial, fechaFin)
    }

    fun observarDatos() {
        operacionesViewModel.estadisticas.observe(this) { datos ->
            if (datos != null) {
                val ventas = datos.ventas.toIntOrNull() ?: 0
                val gastos = datos.gastos.toIntOrNull() ?: 0
                val costos = datos.costos.toIntOrNull() ?: 0
                val ncreditos = datos.ncreditos.toIntOrNull() ?: 0
                val valorCredito = datos.valorCredito.toIntOrNull() ?: 0

                val utilidad = (ventas + valorCredito) - (gastos + costos) // Abonos ¿?

                val puntuacionVentas =
                    ventas.toString().replace(Regex("""(\d)(?=(\d{3})+(?!\d))"""), "$1.")
                val puntuacionGastos =
                    gastos.toString().replace(Regex("""(\d)(?=(\d{3})+(?!\d))"""), "$1.")
                val puntuacionCostos =
                    costos.toString().replace(Regex("""(\d)(?=(\d{3})+(?!\d))"""), "$1.")
                val puntuacionNcredito =
                    ncreditos.toString().replace(Regex("""(\d)(?=(\d{3})+(?!\d))"""), "$1.")
                val puntuacionUtilidad =
                    utilidad.toString().replace(Regex("""(\d)(?=(\d{3})+(?!\d))"""), "$1.")

                if (ventas == 0 && costos == 0 && gastos == 0) {
                    Toast.makeText(
                        this,
                        "No existen datos en ese rango de fechas",
                        Toast.LENGTH_LONG
                    ).show()
                }
                binding.tvVentas.text = "$$puntuacionVentas"
                binding.tvGastos.text = "$$puntuacionGastos"
                binding.tvCostos.text = "$$puntuacionCostos"
                binding.tvFiados.text = "$$puntuacionNcredito"

                val utilidadTexto = "$$puntuacionUtilidad"
                binding.tvUtilidad.text = utilidadTexto

                if (utilidad < 0) {
                    binding.tvUtilidad.setTextColor(
                        ContextCompat.getColor(
                            this,
                            R.color.utilidad_negative
                        )
                    )
                } else {
                    binding.tvUtilidad.setTextColor(
                        ContextCompat.getColor(
                            this,
                            R.color.utilidad_positive
                        )
                    )
                }
            } else {
                binding.tvVentas.text = "Ventas: 0"
                binding.tvGastos.text = "Gastos: 0"
                binding.tvCostos.text = "Costos: 0"
                binding.tvUtilidad.text = "Utilidad: 0"
                binding.tvUtilidad.setTextColor(
                    ContextCompat.getColor(
                        this,
                        R.color.utilidad_positive
                    )
                )
            }
        }
        operacionesViewModel.fechaRegistro.observe(this) { fecha ->
            if (!fecha.isNullOrEmpty()) {
                val prefs = getSharedPreferences("SesionTendero", MODE_PRIVATE)
                prefs.edit().putString("fecha", fecha).apply()
            }
        }
    }

    private fun mostrarCalendario(esFechaInicial: Boolean) {
        val prefs = getSharedPreferences("SesionTendero", Context.MODE_PRIVATE)

        val fechaRegistroStr =
            operacionesViewModel.fechaRegistro.value ?: prefs.getString("fecha", null)
        val hoyMs = MaterialDatePicker.todayInUtcMilliseconds()
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

        val constraints = CalendarConstraints.Builder()
            .setStart(fechaMinimaMs) // Mes donde empieza el calendario
            .setEnd(hoyMs)           // Mes donde termina
            .setValidator(
                CompositeDateValidator.allOf(
                    listOf(
                        DateValidatorPointForward.from(fechaMinimaMs), // Bloquea antes del registro
                        DateValidatorPointBackward.before(hoyMs)       // Bloquea después de hoy
                    )
                )
            )
            .build()

        // 4. Construir el Picker usando las restricciones
        val dateRangePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Selecciona la fecha")
            .setCalendarConstraints(constraints) // Aplicamos los bloqueos aquí
            .build()

        dateRangePicker.addOnPositiveButtonClickListener { selection ->
            val formatoEnvio = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            formatoEnvio.timeZone = TimeZone.getTimeZone("UTC")

            val fechaSeleccionada = formatoEnvio.format(Date(selection))

            if (esFechaInicial) {
                fechaInicial = "$fechaSeleccionada 00:00:00"
            } else {
                fechaFinal = "$fechaSeleccionada 23:59:59"
            }

            if (fechaInicial != null && fechaFinal == null) {
                fechaFinal = fechaInicial!!.replace("00:00:00", "23:59:59")
            } else if (fechaFinal != null && fechaInicial == null) {
                fechaInicial = fechaFinal!!.replace("23:59:59", "00:00:00")
            }
            Log.d("fecha", fechaInicial.toString())
            Log.d("fecha", fechaFinal.toString())

            if (!cedulaGlobal.isNullOrEmpty()) { consultarBalance(cedulaGlobal, fechaInicial!!, fechaFinal!!) }
        }
        dateRangePicker.show(supportFragmentManager, "DATE_RANGE_PICKER")
    }
}