package com.example.micaja

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.micaja.databinding.FragmentDialogoComandosBinding
import com.example.micaja.databinding.ItemComandoBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip

class dialogo_comandos : BottomSheetDialogFragment() {

    private var _binding: FragmentDialogoComandosBinding? = null
    private val binding get() = _binding!!




    private val comandosConVariantes = mapOf(
        "Abrir tienda" to listOf(
            "abierto","iniciar","inicio","open","abrir","abriendo",
            "comenzar","comienzo","arrancar","empezar","empezemos",
            "dia","día","Ej: 'Abrir' tienda"
        ),
        "Registrar base inicial" to listOf(
            "base","base inicial","registrar base",
            "Ej: 'Base' \$ 25000"
        ),
        "Registrar venta" to listOf(
            "venta","ventas","vendí","vendiendo","vender","vendido",
            "vendieron","vendo","vendió","vendimos","vende","vendes",
            "vendidos","ingreso","ingresos","Ej: 'Venta' \$ 50000"
        ),
        "Registrar gasto" to listOf(
            "gasto","gasté","gastando","gastos","gasta","gastaron",
            "gastamos","gastan","gasten","gastemos","gastó",
            "egresos","Ej: 'gaste' \$ 30000"
        ),
        "Registrar costo" to listOf(
            "costo","costos","pago","pagos","pagué","pagando","pagamos",
            "paguen","pague","compra","compré","proveedor","proveedores",
            "paguemos","Ej: 'pague' \$ 70000 a alpina"
        ),
        "Cerrar tienda" to listOf(
            "cerrar","cerrando","final","terminar","finalizar","fin",
            "end","cierre","acabar","Ej: 'Cerrar' tienda"
        ),
        "Registrar crédito o abono" to listOf(
            "credito","crédito","créditos","creditos","fiado","fiados",
            "fiar","adelanto","abono","abonando","cuota","abonos",
            "abonó","adelantó","pagó","saldó","saldar",
            "Ej: 'Credito'","Ej: 'Abono'"
        )
    )


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDialogoComandosBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.cerrar.setOnClickListener {
            dismiss() // Esto cierra el BottomSheet
        }

        val comandos = comandosConVariantes.keys.toList()

        binding.listaComandos.layoutManager = LinearLayoutManager(requireContext())
        binding.listaComandos.adapter = ComandosAdapter(comandos) { comandoSeleccionado ->
            val variantes = comandosConVariantes[comandoSeleccionado] ?: emptyList()
            VariantesBottomSheet
                .newInstance(comandoSeleccionado, variantes)
                .show(parentFragmentManager, "VariantesComando")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null

    }


    private inner class ComandosAdapter(
        private val items: List<String>,
        private val onClick: (String) -> Unit
    ) : RecyclerView.Adapter<ComandosAdapter.ViewHolder>() {

        inner class ViewHolder(val b: ItemComandoBinding) : RecyclerView.ViewHolder(b.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            ViewHolder(
                ItemComandoBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
            )

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val comando  = items[position]
            val variantes = comandosConVariantes[comando] ?: emptyList()

            with(holder.b) {


                // Nombre
                tvNombreComando.text = comando

                // Ejemplo: último ítem que empiece con "Ej:"
                tvEjemplo.text = variantes.lastOrNull { it.startsWith("Ej:") }
                    ?: variantes.firstOrNull()
                            ?: ""

                root.setOnClickListener { onClick(comando) }
            }
        }
    }
}


class VariantesBottomSheet : BottomSheetDialogFragment() {

    companion object {
        private const val ARG_COMANDO   = "comando"
        private const val ARG_VARIANTES = "variantes"

        fun newInstance(comando: String, variantes: List<String>) =
            VariantesBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_COMANDO, comando)
                    putStringArrayList(ARG_VARIANTES, ArrayList(variantes))
                }
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_variantes_comando, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val comando   = arguments?.getString(ARG_COMANDO) ?: ""
        val variantes = arguments?.getStringArrayList(ARG_VARIANTES) ?: arrayListOf()

        // Título
        view.findViewById<android.widget.TextView>(R.id.tvTituloVariantes).text = comando

        // Chips
        val chipGroup = view.findViewById<com.google.android.material.chip.ChipGroup>(
            R.id.chipGroupVariantes
        )

        variantes.forEach { variante ->
            Chip(requireContext()).apply {
                text          = variante
                isClickable   = false
                isCheckable   = false

                if (variante.startsWith("Ej:")) {
                    // Ejemplo: morado oscuro para destacar
                    setChipBackgroundColorResource(R.color.btn_registrarse)
                    setTextColor(resources.getColor(android.R.color.white, null))
                } else {
                    // Variante normal: lavanda claro
                    setChipBackgroundColorResource(R.color.colorPrimary)
                    setTextColor(resources.getColor(R.color.colorOnSecondary, null))
                }

                chipGroup.addView(this)
            }
        }
    }
}