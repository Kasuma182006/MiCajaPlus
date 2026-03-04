package com.example.micaja

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.micaja.ConexionService.ConexionServiceTienda
import com.example.micaja.databinding.DialogBuscarProductoBinding
import com.example.micaja.databinding.ItemProductoDialogBinding
import com.example.micaja.models.inventario

class BuscarProductoDialog(
    private val query: String,
    private val onProductoSeleccionado: (inventario) -> Unit
) : DialogFragment() {

    private var _binding: DialogBuscarProductoBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogBuscarProductoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configurarHeader()
        configurarLista()
        configurarFooter()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setLayout((resources.displayMetrics.widthPixels * 0.93).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
            setBackgroundDrawableResource(android.R.color.transparent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun configurarLista() {
        val productos = ConexionServiceTienda.obtenerInventario().value.filter {
            it.nombre.lowercase().contains(query.lowercase())
        }

        if (productos.isEmpty()) {
            binding.layoutVacio.visibility = View.VISIBLE
            binding.rvProductos.visibility = View.GONE
        } else {
            binding.layoutVacio.visibility = View.GONE
            binding.rvProductos.visibility = View.VISIBLE
            binding.rvProductos.layoutManager = LinearLayoutManager(requireContext())
            binding.rvProductos.adapter = ProductoAdapter(productos) { seleccionado ->
                onProductoSeleccionado(seleccionado)
                dismiss()
            }
        }
    }
    private fun configurarHeader() {
        val total = ConexionServiceTienda.obtenerInventario().value
            .count { it.nombre.lowercase().contains(query.lowercase()) }


        binding.tvQueryTitulo.text = "\"${query.replaceFirstChar { it.uppercase() }}\""
        binding.tvContador.text = if (total > 0)
            "$total presentación(es) encontrada(s)"
        else
            "Sin coincidencias"
    }
    private fun configurarFooter() {
        binding.btnCancelar.setOnClickListener { dismiss() }
    }


    // --- Adapter ---
    private inner class ProductoAdapter(
        private val items: List<inventario>,
        private val onClick: (inventario) -> Unit
    ) : RecyclerView.Adapter<ProductoAdapter.VH>() {

        inner class VH(val binding: ItemProductoDialogBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
            ItemProductoDialogBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = items[position]
            with(holder.binding) {
                tvNombre.text = item.nombre.replaceFirstChar { it.uppercase() }
                tvPresentacion.text = "📦  ${item.presentacion}"
                tvPrecio.text = "$ ${item.valorCompra}"
                tvStock.text = "Stock: ${item.cantidad}"

                cardProducto.setOnClickListener { onClick(item) }
            }
        }

        override fun getItemCount() = items.size
    }
}