package com.example.micaja.ui.dialogs

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.DialogFragment
import com.example.micaja.databinding.DialogBuscarProductoBinding
import com.example.micaja.models.EditarProducto

class BuscarProductoDialog(
    private val query: String,
    private val listaProductos: List<EditarProducto>,
    private val onProductoSeleccionado: (EditarProducto) -> Unit
) : DialogFragment() {

    private var _binding: DialogBuscarProductoBinding? = null
    private val binding get() = _binding!!
    private var paginaActual = 0
    private var pageSize = 2 // ahora agrupamos de a 2 en 2

    // Slots: cada slot agrupa las vistas de una card (card, nombre, presentacion, precio, stock, boton)
    private data class Slot(
        val card: CardView,
        val nombre: TextView,
        val presentacion: TextView,
        val precio: TextView,
        val stock: TextView,
        val seleccionar: TextView
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogBuscarProductoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configurarHeader()

        view.post {
            aplicarMaxHeightParaCards(visibleCards = 2) // mostrar 2 cards sin scroll
            if (listaProductos.isEmpty()) { mostrarVacio() }
            else {
                binding.layoutCardsScroll.visibility = View.VISIBLE
                renderizarPagina(paginaActual)
                configurarNavegacion()
            }
        }
        binding.btnCancelar.setOnClickListener { dismiss() }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setLayout(
                (resources.displayMetrics.widthPixels * 0.93).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
    }

    private fun configurarHeader() {
        binding.tvQueryTitulo.text = "\"${query.replaceFirstChar { it.uppercase() }}\""
        binding.tvContador.text = "${listaProductos.size} presentación(es) encontrada(s)"
    }

    private fun mostrarVacio() {
        binding.layoutVacio.visibility = View.VISIBLE
        binding.layoutCardsScroll.visibility = View.GONE
    }

    private fun renderizarPagina(pagina: Int) {
        val allCards = listOf(binding.card1, binding.card2, binding.card3)
        allCards.forEach { it.visibility = View.GONE }

        val slots = slots().take(pageSize) // tomamos solo los slots necesarios (2)
        val inicio = pagina * pageSize

        slots.forEachIndexed { i, slot ->
            val itemIndex = inicio + i
            if (itemIndex < listaProductos.size) {
                val item = listaProductos[itemIndex]
                slot.card.visibility = View.VISIBLE
                slot.nombre.text = item.nombreProducto?.replaceFirstChar { it.uppercase() } ?: "Sin nombre"
                slot.presentacion.text = "${item.presentacion ?: "Sin presentación"}"
                val puntuacionVentas = item.valorVenta.toString().replace(Regex("""(\d)(?=(\d{3})+(?!\d))"""), "$1.")
                val puntuacionStock = item.cantidad.toString().replace(Regex("""(\d)(?=(\d{3})+(?!\d))"""), "$1.")
                slot.precio.text = "$ ${puntuacionVentas}"
                slot.stock.text = "Stock: ${puntuacionStock}"
                slot.seleccionar.setOnClickListener {
                    onProductoSeleccionado(item)
                    dismiss()
                }
            } else { slot.card.visibility = View.GONE }
        }

        val desde = inicio + 1
        val hasta = minOf(inicio + pageSize, listaProductos.size)
        binding.tvPosicion.text = "$desde–$hasta de ${listaProductos.size}"
    }

    private fun configurarNavegacion() {
        val totalPaginas = totalPaginas()
        if (totalPaginas > 1) {
            binding.layoutNavegacion.visibility = View.VISIBLE
            actualizarEstadoBotones()

            binding.btnAnterior.setOnClickListener {
                if (paginaActual > 0) {
                    paginaActual--
                    renderizarPagina(paginaActual)
                    actualizarEstadoBotones()
                }
            }

            binding.btnSiguiente.setOnClickListener {
                if (paginaActual < totalPaginas - 1) {
                    paginaActual++
                    renderizarPagina(paginaActual)
                    actualizarEstadoBotones()
                }
            }
        } else { binding.layoutNavegacion.visibility = View.GONE }
    }

    private fun actualizarEstadoBotones() {
        binding.btnAnterior.alpha = if (paginaActual == 0) 0.3f else 1f
        binding.btnSiguiente.alpha = if (paginaActual == totalPaginas() - 1) 0.3f else 1f
    }

    private fun totalPaginas() = Math.ceil(listaProductos.size / pageSize.toDouble()).toInt()
    private fun slots() = listOf(
        Slot(binding.card1, binding.tvNombre1, binding.tvPresentacion1,
            binding.tvPrecio1, binding.tvStock1, binding.tvSeleccionar1),
        Slot(binding.card2, binding.tvNombre2, binding.tvPresentacion2,
            binding.tvPrecio2, binding.tvStock2, binding.tvSeleccionar2),
        Slot(binding.card3, binding.tvNombre3, binding.tvPresentacion3,
            binding.tvPrecio3, binding.tvStock3, binding.tvSeleccionar3)
    )

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun aplicarMaxHeightParaCards(visibleCards: Int = 2) {
        val nested = binding.layoutCardsScroll
        val cardRef = binding.card1

        if (cardRef.measuredHeight == 0) {
            val widthSpec = if (nested.width > 0) {
                View.MeasureSpec.makeMeasureSpec(nested.width, View.MeasureSpec.AT_MOST)
            } else {
                View.MeasureSpec.makeMeasureSpec(resources.displayMetrics.widthPixels, View.MeasureSpec.AT_MOST)
            }
            cardRef.measure(
                widthSpec,
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
        }

        val cardHeight = cardRef.measuredHeight.takeIf { it > 0 }
            ?: (resources.displayMetrics.heightPixels * 0.18).toInt()
        val verticalPadding = nested.paddingTop + nested.paddingBottom
        val inner = binding.layoutCardsInner
        val innerPadding = inner.paddingTop + inner.paddingBottom
        val extraSpacing = 16 // margen extra por seguridad
        val targetHeight = (visibleCards * cardHeight) + verticalPadding + innerPadding + extraSpacing
        val lp = nested.layoutParams
        lp.height = targetHeight
        nested.layoutParams = lp
        nested.isFillViewport = false
        pageSize = visibleCards.coerceIn(1, 3)
    }
}