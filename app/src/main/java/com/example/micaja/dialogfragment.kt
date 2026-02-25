package com.example.micaja

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.example.micaja.databinding.FragmentDialogfragmentBinding


class dialogfragment : Fragment() {



    lateinit var binding: FragmentDialogfragmentBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentDialogfragmentBinding.inflate(inflater, container, false)
        return binding.root
    }
    //cambio
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.i("acciones", arguments.toString())

        val ventas = arguments?.getInt("ventas")
        val gastos = arguments?.getInt("gastos")
        val costos = arguments?.getInt("costos")

        binding.ventas.append("$" + ventas.toString())
        binding.gastos.append("$" + gastos.toString())
        binding.costos.append("$" + costos.toString())


        binding.ver.setOnClickListener {
            val utilidad = binding.utilidad
            utilidad.isVisible = true
            val utilidad_total = util(ventas!!,gastos!!,costos!!)
            utilidad.setText("$" + utilidad_total)
        }

        binding.cerrar.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

    }
    fun util(v: Int,g:Int,c: Int): Int{
        val operacion = v -(g + c)
        return  operacion

    }





}