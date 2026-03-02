package com.example.micaja

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.micaja.ConexionService.ConexionServiceTienda
import com.example.micaja.models.inventario

class BuscarProductoDialog(
    private val query: String,
    private val onProductoSeleccionado: (inventario) -> Unit
) : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = buildLayout()

    private fun buildLayout(): View {
        val ctx = requireContext()

        val mainLayout = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(56, 48, 56, 32)
            setBackgroundColor(Color.WHITE)
        }

        // Título
        mainLayout.addView(TextView(ctx).apply {
            text = "Resultados para \"$query\""
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#1A1A2E"))
            setPadding(0, 0, 0, 28)
        })

        // Filtrar inventario por nombre
        val productos = ConexionServiceTienda.obtenerInventario().value.filter {
            it.nombre.lowercase().contains(query.lowercase())
        }

        if (productos.isEmpty()) {
            mainLayout.addView(TextView(ctx).apply {
                text = "No se encontraron productos."
                textSize = 14f
                setTextColor(Color.GRAY)
                gravity = Gravity.CENTER
                setPadding(0, 40, 0, 40)
            })
        } else {
            val recycler = RecyclerView(ctx).apply {
                layoutManager = LinearLayoutManager(ctx)
                adapter = ProductoAdapter(productos) { seleccionado ->
                    onProductoSeleccionado(seleccionado)
                    dismiss()
                }
            }
            mainLayout.addView(recycler)
        }

        // Botón cancelar
        mainLayout.addView(Button(ctx).apply {
            text = "Cancelar"
            setTextColor(Color.parseColor("#6C63FF"))
            background = null
            setOnClickListener { dismiss() }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.END
                topMargin = 20
            }
        })

        return mainLayout
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setLayout(
                (resources.displayMetrics.widthPixels * 0.92).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setBackgroundDrawableResource(android.R.color.transparent)
        }
    }

    // ── Adapter interno ──────────────────────────────────────────────────────
    private inner class ProductoAdapter(
        private val items: List<inventario>,
        private val onClick: (inventario) -> Unit
    ) : RecyclerView.Adapter<ProductoAdapter.VH>() {

        inner class VH(val card: LinearLayout) : RecyclerView.ViewHolder(card)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val ctx = parent.context

            val tvNombre = TextView(ctx).apply {
                tag = "nombre"; textSize = 15f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.parseColor("#1A1A2E"))
            }
            val tvPresentacion = TextView(ctx).apply {
                tag = "presentacion"; textSize = 13f
                setTextColor(Color.GRAY); setPadding(0, 4, 0, 0)
            }
            val tvPrecio = TextView(ctx).apply {
                tag = "precio"; textSize = 13f
                setTextColor(Color.parseColor("#6C63FF")); setPadding(0, 4, 0, 0)
            }
            val tvCantidad = TextView(ctx).apply {
                tag = "cantidad"; textSize = 13f
                setTextColor(Color.parseColor("#4CAF50")); setPadding(0, 4, 0, 0)
            }
            val divider = View(ctx).apply {
                setBackgroundColor(Color.parseColor("#E0E0E0"))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 1
                ).apply { topMargin = 16 }
            }

            val card = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(8, 20, 8, 4)
                layoutParams = RecyclerView.LayoutParams(
                    RecyclerView.LayoutParams.MATCH_PARENT,
                    RecyclerView.LayoutParams.WRAP_CONTENT
                )
                addView(tvNombre); addView(tvPresentacion)
                addView(tvPrecio); addView(tvCantidad); addView(divider)
            }
            return VH(card)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = items[position]
            with(holder.card) {
                (findViewWithTag("nombre") as TextView).text =
                    item.nombre.replaceFirstChar { it.uppercase() }
                (findViewWithTag("presentacion") as TextView).text =
                    "Presentación: ${item.presentacion}"
                (findViewWithTag("precio") as TextView).text =
                    "Precio compra: \$ ${item.valorCompra}"
                (findViewWithTag("cantidad") as TextView).text =
                    "Stock: ${item.cantidad}"
                setOnClickListener { onClick(item) }
            }
        }

        override fun getItemCount() = items.size
    }
}