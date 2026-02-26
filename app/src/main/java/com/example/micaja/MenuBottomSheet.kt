package com.example.micaja

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.micaja.Calendario.ConsultarXFecha
import com.example.micaja.databinding.FragmentMenuBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/**
 * MenuBottomSheet
 * ────────────────────────────────────────────────────────────────
 * Reemplaza el PopupMenu (menu_principal.xml) por un Bottom Sheet
 * con el mismo estilo de tarjetas que activity_principal.
 *
 * USO desde Principal.kt (reemplaza el bloque del PopupMenu):
 *
 *   // Antes (PopupMenu):
 *   // val popup = PopupMenu(this, view)
 *   // popup.menuInflater.inflate(R.menu.menu_principal, popup.menu)
 *   // popup.show()
 *
 *   // Ahora (BottomSheet):
 *   override fun onCreateOptionsMenu(menu: Menu): Boolean {
 *       // Ya no inflas menu_principal.xml aquí
 *       // Solo infla un ícono de menú (tres puntos) en el toolbar
 *       menuInflater.inflate(R.menu.menu_icono, menu)   // solo el ícono
 *       return true
 *   }
 *
 *   override fun onOptionsItemSelected(item: MenuItem): Boolean {
 *       if (item.itemId == R.id.action_menu) {
 *           MenuBottomSheet().show(supportFragmentManager, "MenuBottomSheet")
 *           return true
 *       }
 *       return super.onOptionsItemSelected(item)
 *   }
 *
 * ────────────────────────────────────────────────────────────────
 * DRAWABLE requerido: bg_sheet_handle.xml (shape oval o rectangle
 * con cornerRadius="2dp") — crear en res/drawable/ si no existe:
 *
    <?xml version="1.0" encoding="utf-8"?>
    <shape xmlns:android="http://schemas.android.com/apk/res/android"
        android:shape="rectangle">
        <corners android:radius="2dp" />
    </shape>
 *
 * ────────────────────────────────────────────────────────────────
 */
/**
 * Recibe dos lambdas opcionales para que cada Activity/Fragment que abra
 * este BottomSheet pueda inyectar su propia lógica de Comandos y CerrarSesion
 * sin duplicar código.
 *
 * Uso desde chat_Tienda (o cualquier otra pantalla):
 *   MenuBottomSheet(
 *       onComandos    = { dialogo_comandos().show(supportFragmentManager, "DialogoComandos") },
 *       onCerrarSesion = { SesionManager.cerrarSesion(this) }
 *   ).show(supportFragmentManager, "MenuBottomSheet")
 */
class MenuBottomSheet(
    private val onComandos: (() -> Unit)? = null,
    private val onCerrarSesion: (() -> Unit)? = null
) : BottomSheetDialogFragment() {

    private var _binding: FragmentMenuBottomSheetBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMenuBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ── Balance ──────────────────────────────────────────────────────────
        binding.menuBtnBalance.setOnClickListener {
            dismiss()
            // TODO: reemplaza BalanceActivity::class.java por tu Activity real
            startActivity(Intent(requireContext(), ConsultarXFecha::class.java))
        }

        // ── Editar Cliente ───────────────────────────────────────────────────
        binding.menuBtnEditarCliente.setOnClickListener {
            dismiss()
            // TODO: reemplaza editar_clientes::class.java por tu Activity real
            startActivity(Intent(requireContext(), editar_clientes::class.java))
        }

        // ── Editar Producto ──────────────────────────────────────────────────
        binding.menuBtnEditarProducto.setOnClickListener {
            dismiss()
            // TODO: reemplaza fragment_editar_producto::class.java por tu Activity real
            startActivity(Intent(requireContext(), fragment_editar_producto::class.java))
        }

        // ── Comandos ─────────────────────────────────────────────────────────
        // Si el llamador inyectó un lambda lo usa; si no, muestra dialogo_comandos por defecto.
        binding.menuBtnComandos.setOnClickListener {
            dismiss()
            if (onComandos != null) {
                onComandos.invoke()
            } else {
                dialogo_comandos().show(parentFragmentManager, "Comandos")
            }
        }

        // ── Cerrar sesión ────────────────────────────────────────────────────
        // Si el llamador inyectó un lambda lo usa; si no, queda vacío (cada pantalla
        // puede tener distinto comportamiento post-logout).
        binding.menuBtnCerrarSesion.setOnClickListener {
            dismiss()
            onCerrarSesion?.invoke()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
