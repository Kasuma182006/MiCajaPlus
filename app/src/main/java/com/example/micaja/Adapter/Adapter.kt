package com.example.micaja.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.micaja.R
import com.example.micaja.databinding.ActivityItemMensajesBinding
import com.example.micaja.models.modelo
import com.example.micaja.viewmodel.TenderoViewModel

class Adapter ( var dataset: MutableList<modelo>, var sistemaData: MutableList<modelo>):
    RecyclerView.Adapter<Adapter.ViewHolder>() {
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val binding = ActivityItemMensajesBinding.bind(view)

        fun inicializar(modelo:modelo,sistema: modelo){
            binding.mensaje.text = modelo.mensaje
            binding.mensajeSistema.text = sistema.mensaje
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int

    ): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.activity_item_mensajes, parent, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val usuario = dataset.getOrNull(position)
        val sistema = sistemaData.getOrNull(position)

        if (usuario != null && sistema != null) {
            holder.inicializar(usuario, sistema)
        }
    }

    override fun getItemCount(): Int = minOf(dataset.size, sistemaData.size)
}