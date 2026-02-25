package com.example.micaja

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.fragment.app.DialogFragment
import com.example.micaja.databinding.FragmentDialogoComandosBinding

class dialogo_comandos : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext())
        val binding = FragmentDialogoComandosBinding.inflate(requireActivity().layoutInflater)
        val comandos = comandosConVariantes.keys.toList()

        val adapter = ArrayAdapter(requireContext(), R.layout.
        item_row, R.id.textItem, comandos)
        binding.listaComandos.adapter = adapter

        binding.listaComandos.setOnItemClickListener { _, _, position, _ ->
            val comando = comandos[position]
            val variantes = comandosConVariantes[comando]?.joinToString("\n") ?: "Sin variantes registradas"

            AlertDialog.Builder(requireContext())
                .setTitle("Formas de decir \"$comando\"")
                .setMessage(variantes)
                .setPositiveButton("Cerrar", null)
                .show()
        }

        builder.setView(binding.root)
            .setPositiveButton("Cerrar") { dialog, _ -> dialog.dismiss() }

        return builder.create()
    }

    private val comandosConVariantes = mapOf(
        "Abrir tienda" to listOf("abierto","iniciar","inicio","open","abrir","abriendo","comenzar","comienzo","arrancar","empezar","empezemos","dia","día","Ej: 'Abrir' tienda"),
        "Registrar base inicial" to listOf("base", "base inicial", "registrar base","Ej: 'Base' $ 25000"),
        "Registrar venta" to listOf("venta","ventas" ,"vendí", "vendiendo", "vender", "vendido", "vendieron", "vendo", "vendió", "vendimos", "vende", "vendes", "vendidos", "ingreso", "ingresos","Ej: 'Venta' $ 50000"),
        "Registrar gasto" to listOf("gasto", "gasté", "gastando", "gastos", "gasta", "gastaron", "gastamos", "gastan", "gasten", "gastemos", "gastó", "egresos", "egresos","Ej: 'gaste' $ 30000"),
        "Registrar costo" to listOf("costo", "costos", "pago", "pagos", "pagué", "pagando", "pagamos", "paguen", "pague", "compra", "compré", "proveedor", "proveedores", "paguemos","Ej: 'pague' $ 70000 a alpina"),
        "Cerrar tienda" to listOf("cerrar","cerrando","final","terminar","finalizar","fin","end","cierre","acabar","Ej: 'Cerrar' tienda"),
        "Registrar crédito o abono" to listOf("credito", "crédito", "créditos", "creditos", "fiado", "fiados", "crédito", "fiar","adelanto", "abono", "abonando", "cuota", "abonos", "abonó", "adelantó", "pagó", "saldó", "saldar","Ej: 'Cretido'","Ej: 'Abono'")
    )


}
