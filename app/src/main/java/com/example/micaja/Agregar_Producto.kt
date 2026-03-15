package com.example.micaja

import android.os.Bundle
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

class Agregar_Producto : AppCompatActivity() {

    // Lista de categorías disponibles en el dropdown.
    // Agregar o quitar categorías aquí sin tocar el XML ni el resto del código.
    private val categorias = listOf(
        "Lácteos",
        "Bebidas",
        "Carnes",
        "Granos y cereales",
        "Frutas y verduras",
        "Panadería",
        "Aseo y limpieza",
        "Snacks y dulces",
        "Enlatados",
        "Otros"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, true)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        setContentView(R.layout.activity_agregar_producto)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        configurarDropdownCategoria()
    }

    private fun configurarDropdownCategoria() {
        // ArrayAdapter conecta la lista de strings con el AutoCompleteTextView.
        // android.R.layout.simple_dropdown_item_1line es el layout estándar de Material
        // para cada fila del desplegable — texto en una sola línea.
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            categorias
        )

        val autoComplete = findViewById<AutoCompleteTextView>(R.id.etCatagoriaProducto)
        autoComplete.setAdapter(adapter)

        // Al tocar el campo se despliega la lista completa sin necesidad de escribir.
        autoComplete.setOnClickListener {
            autoComplete.showDropDown()
        }
    }
}