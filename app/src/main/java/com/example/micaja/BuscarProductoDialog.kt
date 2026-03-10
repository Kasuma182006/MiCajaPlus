import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.micaja.databinding.DialogBuscarProductoBinding
import com.example.micaja.databinding.ItemProductoDialogBinding
import com.example.micaja.models.EditarProducto

class BuscarProductoDialog(
    private val query: String,
    private val listaProductos: List<EditarProducto>, // Recibimos la lista cargada
    private val onProductoSeleccionado: (EditarProducto) -> Unit
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
        binding.btnCancelar.setOnClickListener { dismiss() }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            // Ajuste de ancho al 93% y fondo transparente para bordes redondeados
            setLayout((resources.displayMetrics.widthPixels * 0.93).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
    }

    private fun configurarHeader() {
        binding.tvQueryTitulo.text = "\"${query.replaceFirstChar { it.uppercase() }}\""
        binding.tvContador.text = "${listaProductos.size} presentación(es) encontrada(s)"
    }

    private fun configurarLista() {
        if (listaProductos.isEmpty()) {
            binding.layoutVacio.visibility = View.VISIBLE
            binding.rvProductos.visibility = View.GONE
        } else {
            binding.layoutVacio.visibility = View.GONE
            binding.rvProductos.visibility = View.VISIBLE
            binding.rvProductos.layoutManager = LinearLayoutManager(requireContext())
            binding.rvProductos.adapter = ProductoAdapter(listaProductos) { seleccionado ->
                onProductoSeleccionado(seleccionado)
                dismiss()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // --- Adaptador Interno ---
    private inner class ProductoAdapter(
        private val items: List<EditarProducto>,
        private val onClick: (EditarProducto) -> Unit
    ) : RecyclerView.Adapter<ProductoAdapter.VH>() {

        inner class VH(val binding: ItemProductoDialogBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
            ItemProductoDialogBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = items[position]
            with(holder.binding) {
                // Ajusta estos nombres según los campos reales de tu modelo EditarProducto
                tvNombre.text = item.nombre.replaceFirstChar { it.uppercase() }
                tvPresentacion.text = "📦  ${item.presentacion ?: "Sin presentación"}"
                tvPrecio.text = "$ ${item.valorVenta}"
                tvStock.text = "Stock: ${item.cantidad}"

                root.setOnClickListener { onClick(item) }
            }
        }

        override fun getItemCount() = items.size
    }
}