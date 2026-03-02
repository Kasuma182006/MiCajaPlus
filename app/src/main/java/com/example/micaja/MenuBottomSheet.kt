package com.example.micaja

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.micaja.Calendario.ConsultarXFecha
import com.example.micaja.databinding.FragmentMenuBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

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

        //Balance
        binding.menuBtnBalance.setOnClickListener {
            dismiss()
            // TODO: reemplaza BalanceActivity::class.java por tu Activity real
            startActivity(Intent(requireContext(), ConsultarXFecha::class.java))
        }

        //Editar Cliente
        binding.menuBtnEditarCliente.setOnClickListener {
            dismiss()
            // TODO: reemplaza editar_clientes::class.java por tu Activity real
            startActivity(Intent(requireContext(), editar_clientes::class.java))
        }

        //Editar Producto
        binding.menuBtnEditarProducto.setOnClickListener {
            dismiss()
            // TODO: reemplaza fragment_editar_producto::class.java por tu Activity real
            startActivity(Intent(requireContext(), fragment_editar_producto::class.java))
        }

        // Comandos
        binding.menuBtnComandos.setOnClickListener {
            dismiss()
            if (onComandos != null) {
                onComandos.invoke()
            } else {
                dialogo_comandos().show(parentFragmentManager, "Comandos")
            }
        }

        //Cerrar sesion
        binding.icCerrarSeccion.setOnClickListener {
            dismiss()
            onCerrarSesion?.invoke()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
