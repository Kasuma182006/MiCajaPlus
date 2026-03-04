package com.example.micaja.Calendario

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.micaja.Principal
import com.example.micaja.R
import com.example.micaja.databinding.ConsultarXFechaBinding
import com.example.micaja.viewmodel.OperacionesViewModel
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.util.Date
import java.util.Locale
import java.util.TimeZone

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
    }

    private fun configurarBotones() {
        binding.btnCalendarioStar.setOnClickListener { mostrarCalendario(true) }

        binding.btnCalendarioEnd.setOnClickListener { mostrarCalendario(false) }

        binding.btnCerrar.setOnClickListener { cancelar() }
    }

    private fun cancelar() {
        Toast.makeText(this, "Cancelaste la operación", Toast.LENGTH_SHORT).show()
        startActivity(Intent(this, Principal::class.java))
        finish()
    }

    private fun consultarBalance(idTendero: String, fechaInicial: String, fechaFin: String) {
        operacionesViewModel.consultarEstadisticas(idTendero, fechaInicial, fechaFin)
        operacionesViewModel.consultarNumeroCreditos(idTendero, fechaInicial, fechaFin)
    }

    fun observarDatos() {
        operacionesViewModel.estadisticas.observe(this) { datos ->
            if (datos != null) {
                val ventas = datos.ventas?.toIntOrNull() ?: 0
                val gastos = datos.gastos?.toIntOrNull() ?: 0
                val costos = datos.costos?.toIntOrNull() ?: 0

                val respuesta = ventas - gastos - costos
                val utilidad  = respuesta

                binding.tvVentas.text   = "$$ventas"
                binding.tvGastos.text   = "$$gastos"
                binding.tvCostos.text   = "$$costos"

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
        val dateRangePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Selecciona la fecha")
            .build()

        dateRangePicker.addOnPositiveButtonClickListener { selection ->
            val formato = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            formato.timeZone = TimeZone.getTimeZone("UTC")

            val fechaSeleccionada = formato.format(Date(selection))


            if (esFechaInicial) {
                fechaInicial = "$fechaSeleccionada 00:00:00"
            } else {
                fechaFinal = "$fechaSeleccionada 23:59:59"
            }

            if (fechaInicial != null && fechaFinal == null) {
                fechaFinal = fechaInicial!!.replace("00:00:00", "23:59:59")
            }

            if (fechaFinal != null && fechaInicial == null) {
                fechaInicial = fechaFinal!!.replace("23:59:59", "00:00:00")
            }else{
                fechaInicial = fechaInicial
                fechaFinal = fechaFinal
            }

            val fechaInicioSoloDia = fechaInicial!!.substring(0, 10)
            val fechaFinSoloDia = fechaFinal!!.substring(0, 10)

            binding.tvRangoFecha.text =
                if (fechaInicioSoloDia == fechaFinSoloDia) {
                    fechaInicioSoloDia
                }else{
                    "$fechaInicioSoloDia - $fechaFinSoloDia"
                }



            val prefs = getSharedPreferences("SesionTendero", MODE_PRIVATE)
            val tendero = prefs.getString("cedula", null)


            if (tendero.isNullOrEmpty()) {
                Toast.makeText(this, "Error: No se encontró la sesión del tendero", Toast.LENGTH_LONG).show()
                return@addOnPositiveButtonClickListener
            }

            val idTendero = tendero


            consultarBalance(idTendero, fechaInicial.toString(), fechaFinal.toString())
        }

        dateRangePicker.show(supportFragmentManager, "DATE_RANGE_PICKER")
    }
}