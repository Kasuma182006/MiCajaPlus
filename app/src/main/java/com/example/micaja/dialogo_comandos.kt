package com.example.micaja

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.micaja.databinding.FragmentDialogoComandosBinding
import com.example.micaja.databinding.ItemComandoBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class dialogo_comandos : BottomSheetDialogFragment() {

    private var _binding: FragmentDialogoComandosBinding? = null
    private val binding get() = _binding!!
    private val comandosConVariantes = mapOf(
        "Abrir tienda" to listOf(
            "abrir", "arrancar", "comenzar", "empezar"
        ),
        "Cerrar tienda" to listOf(
            "cerrar", "cerrando", "cierre"
        ),
        "Registrar venta" to listOf(
            "venta", "vendí", "vendieron", "vendió", "Ej: 3 arroz diana libra"
        ),
        "Registrar gasto" to listOf(
            "gasto", "gasté", "gastar", "Ej: Gasté en un domicilio $15.000"
        ),
        "Registrar costo" to listOf(
            "compra", "compré", "costo"
        ),
        "Registrar crédito" to listOf(
            "crédito", "fiado", "fiar", "fié"
        ),
        "Abonos" to listOf(
            "abono", "abonar", "cuota", "deuda"
        ),
        "Agregar producto" to listOf(
            "agregar", "añadir", "producto"
        ),
        "Agregar cliente" to listOf(
            "cliente", "nombre", "nuevo", "clientes"
        ),
        "Consultar cliente" to listOf(
            "busca", "buscar", "consulta", "filtrar"
        ),
        "Cancelar operación" to listOf(
            "cancelar", "cancela", "abortar"
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

        binding.cerrar.setOnClickListener { dismiss() }
        val comandos = comandosConVariantes.keys.toList()

        binding.listaComandos.layoutManager = GridLayoutManager(requireContext(), 2)
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
            ViewHolder(ItemComandoBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun getItemCount() = items.size
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val comando   = items[position]
            val variantes = comandosConVariantes[comando] ?: emptyList()
            with(holder.b) {
                tvNombreComando.text = comando
                tvEjemplo.text = variantes.lastOrNull { it.startsWith("Ej:") }
                    ?: variantes.firstOrNull() ?: ""
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
        view.findViewById<TextView>(R.id.tvTituloVariantes).text = comando

        val palabras  = variantes.filter { !it.startsWith("Ej:") }
        val ejemplos  = variantes.filter {  it.startsWith("Ej:") }
        view.findViewById<TextView>(R.id.tvVariantes).text =
            palabras.joinToString(separator = "  ,  ")

        val labelEjemplos = view.findViewById<TextView>(R.id.labelEjemplos)
        val tvEjemplos    = view.findViewById<TextView>(R.id.tvEjemplos)

        if (ejemplos.isNotEmpty()) {
            labelEjemplos.visibility = View.VISIBLE
            tvEjemplos.visibility    = View.VISIBLE
            tvEjemplos.text = ejemplos.joinToString(separator = "\n")
        } else {
            labelEjemplos.visibility = View.GONE
            tvEjemplos.visibility    = View.GONE
        }
    }
}