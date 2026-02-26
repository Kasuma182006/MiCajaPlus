package com.example.micaja

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.micaja.databinding.ActivityEditarClientesBinding

class editar_clientes : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // View Binding reemplaza setContentView(R.layout.activity_editar_clientes)
        // Si aún no usas binding, deja el setContentView original y borra estas líneas
        val binding = ActivityEditarClientesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Botón retroceso
        binding.btnRetroceso.setOnClickListener {
            finish() // cierra esta Activity y vuelve a la anterior
        }
    }
}