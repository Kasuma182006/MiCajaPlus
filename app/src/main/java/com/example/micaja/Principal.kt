package com.example.micaja


import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem // ✅ Import necesario
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
// ✅ Import necesario
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

        // Configura el Toolbar como ActionBar
//        val toolbar = findViewById<Toolbar>(R.id.toolbarPrincipal)
//        setSupportActionBar(toolbar)

        val prefs = getSharedPreferences("SesionTendero", MODE_PRIVATE)
        val cedula = prefs.getString("cedula", null)
        val nombre = prefs.getString("nombre",null)

        if (nombre != null)
            binding.NombreTendero.text = "Bienvenido: $nombre"

        else
            binding.NombreTendero.text = "Bienvenido Tendero"
        botones()
    }

    private fun botones() {
        //abrir chat_Tienda / Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        // para traer la actividad anterior al frente si ya existe
        //haci se evita perder los datos de lo que ocurra en el chat
        //al retroceder
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

    // Infla el menú
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_principal, menu)
        return true
    }

    // Maneja la opción de cerrar sesión
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_CerrarSeccion -> {
                cerrarSesion()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // Función para cerrar sesión
    private fun cerrarSesion() {
        val prefs = getSharedPreferences("SesionTendero", MODE_PRIVATE)
        prefs.edit().clear().apply()

        val estadoTienda = getSharedPreferences("EstadoTienda",MODE_PRIVATE)
        val editor = estadoTienda.edit()
        editor.putBoolean("abierta", false)
        editor.apply()
        val estadoBase = getSharedPreferences("EstadoBase",MODE_PRIVATE)
        estadoBase.edit().putBoolean("base",false).apply()

        Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show()

        val intent = Intent(this, Login::class.java) // Ajusta si tu login tiene otro nombre
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }
}
