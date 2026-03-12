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

    // Página actual (cada página muestra pageSize productos)
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

        // Esperar a que el layout se haya medido para calcular alturas y aplicar maxHeight
        view.post {
            aplicarMaxHeightParaCards(visibleCards = 2) // mostrar 2 cards sin scroll
            if (listaProductos.isEmpty()) {
                mostrarVacio()
            } else {
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

    // ── Header ────────────────────────────────────────────────────────────
    private fun configurarHeader() {
        binding.tvQueryTitulo.text = "\"${query.replaceFirstChar { it.uppercase() }}\""
        binding.tvContador.text = "${listaProductos.size} presentación(es) encontrada(s)"
    }

    // ── Estado vacío ──────────────────────────────────────────────────────
    private fun mostrarVacio() {
        binding.layoutVacio.visibility = View.VISIBLE
        binding.layoutCardsScroll.visibility = View.GONE
    }

    // ── Renderiza los productos de la página actual ───────────────────────
    private fun renderizarPagina(pagina: Int) {
        // Ocultar todas las cards primero para evitar residuos visuales
        val allCards = listOf(binding.card1, binding.card2, binding.card3)
        allCards.forEach { it.visibility = View.GONE }

        val slots = slots().take(pageSize) // tomamos solo los slots necesarios (2)
        val inicio = pagina * pageSize

        slots.forEachIndexed { i, slot ->
            val itemIndex = inicio + i
            if (itemIndex < listaProductos.size) {
                val item = listaProductos[itemIndex]
                slot.card.visibility = View.VISIBLE
                slot.nombre.text = item.nombre.replaceFirstChar { it.uppercase() }
                slot.presentacion.text = "${item.presentacion ?: "Sin presentación"}"
                slot.precio.text = "$ ${item.valorVenta}"
                slot.stock.text = "Stock: ${item.cantidad}"
                slot.seleccionar.setOnClickListener {
                    onProductoSeleccionado(item)
                    dismiss()
                }
            } else {
                slot.card.visibility = View.GONE
            }
        }

        val desde = inicio + 1
        val hasta = minOf(inicio + pageSize, listaProductos.size)
        binding.tvPosicion.text = "$desde–$hasta de ${listaProductos.size}"
    }

    // ── Navegación (flechas) ──────────────────────────────────────────────
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
        } else {
            binding.layoutNavegacion.visibility = View.GONE
        }
    }

    private fun actualizarEstadoBotones() {
        binding.btnAnterior.alpha = if (paginaActual == 0) 0.3f else 1f
        binding.btnSiguiente.alpha = if (paginaActual == totalPaginas() - 1) 0.3f else 1f
    }

    // ── Helpers ───────────────────────────────────────────────────────────
    private fun totalPaginas() = Math.ceil(listaProductos.size / pageSize.toDouble()).toInt()

    /** Devuelve la lista completa de slots (3) para poder tomar dinámicamente los primeros N */
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

    /**
     * Calcula y aplica un maxHeight al NestedScrollView (binding.layoutCardsScroll)
     * para que quepan exactamente `visibleCards` sin necesidad de scroll.
     * Si hay más items, el NestedScrollView permitirá desplazamiento interno.
     */
    private fun aplicarMaxHeightParaCards(visibleCards: Int = 2) {
        val nested = binding.layoutCardsScroll
        val cardRef = binding.card1

        // Forzar medida si aún no fue medida
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

        // Calcular paddings y márgenes internos del contenedor
        val verticalPadding = nested.paddingTop + nested.paddingBottom
        val inner = binding.layoutCardsInner
        val innerPadding = inner.paddingTop + inner.paddingBottom
        val extraSpacing = 16 // margen extra por seguridad

        // Altura objetivo: N * cardHeight + paddings + espacio extra
        val targetHeight = (visibleCards * cardHeight) + verticalPadding + innerPadding + extraSpacing

        // Aplicar como altura del NestedScrollView
        val lp = nested.layoutParams
        lp.height = targetHeight
        nested.layoutParams = lp

        // No forzamos fillViewport; queremos que el contenido pueda scrollear si excede
        nested.isFillViewport = false

        // Ajustar pageSize para la paginación lógica
        pageSize = visibleCards.coerceIn(1, 3)
    }
}
