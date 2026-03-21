package com.example.micaja

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.micaja.Calendario.ConsultarXFecha
import com.example.micaja.databinding.ActivityPrincipalBinding

class Principal : AppCompatActivity() {

    private lateinit var binding: ActivityPrincipalBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityPrincipalBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val prefs = getSharedPreferences("SesionTendero", MODE_PRIVATE)
        val nombre = prefs.getString("nombre",null)

        if (nombre != null)
            binding.NombreTendero.text = "Bienvenido: $nombre"
        else
            binding.NombreTendero.text = "Bienvenido Tendero"
        botones()
    }

    private fun botones() {
        binding.btnTienda.setOnClickListener {
            val intent = Intent(this, chat_Tienda::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            startActivity(intent)
        }

        binding.btnBalance.setOnClickListener {
            val intent = Intent(this, ConsultarXFecha::class.java)
            startActivity(intent)
            finish()
        }
    }
}