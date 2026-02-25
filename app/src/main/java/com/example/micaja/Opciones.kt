package com.example.micaja

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import com.example.micaja.databinding.AbonoBinding
import com.example.micaja.databinding.FragmentOpcionesBinding

class Opciones : Fragment() {

    lateinit var binding: FragmentOpcionesBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentOpcionesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.abono.setOnClickListener { fragmento(true) }
        binding.credito.setOnClickListener { fragmento(false) }
        binding.cerrar.setOnClickListener { parentFragmentManager.popBackStack() }
    }

    fun fragmento(abono: Boolean) {
        val fragmento = Abono().apply {
            arguments = bundleOf(
                "abono" to abono,
            )
        }

        parentFragmentManager
            .beginTransaction()
            .replace(R.id.fragmento_credito, fragmento)
            .addToBackStack(null)
            .commit()
    }
}