package com.example.micaja

import BuscarProductoDialog
import android.os.Bundle
import android.util.Log
import android.util.Log.e
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.example.micaja.ConexionService.ConexionServiceTienda
import com.example.micaja.databinding.ActivityFragmentEditarProductoBinding
import com.example.micaja.models.BuscarProductos
import com.example.micaja.models.EditarProducto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import retrofit2.Response
import kotlin.coroutines.coroutineContext
import kotlin.math.log

class fragment_editar_producto : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Forzar que la ventana deje que el sistema gestione insets (necesario para adjustResize)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        // Forzar ajuste del layout cuando aparece el teclado
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        // View Binding reemplaza setContentView(R.layout.activity_fragment_editar_producto)
        // Si aún no usas binding, deja el setContentView original y borra estas líneas
        val binding = ActivityFragmentEditarProductoBinding.inflate(layoutInflater)
        setContentView(binding.root)


        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }



        // Botón retroceso
        binding.btnRetroceso.setOnClickListener {
            finish()
        }

        binding.btnBuscarProducto.setOnClickListener {
            buscarProducto(binding)
        }


        binding.textBuscarProducto.setEndIconOnClickListener{
            inicializarEditarProducto(binding)

        }






    }

    fun buscarProducto(binding: ActivityFragmentEditarProductoBinding) {
        val producto = binding.etBuscarProducto.text.toString().lowercase().trimEnd()
        var lista : List<EditarProducto>
        if (producto.isNotEmpty()) {
            val preference = getSharedPreferences("SesionTendero", MODE_PRIVATE)
            val cedula = preference.getString("cedula", "").toString()

            if (cedula.isNotEmpty()) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val conexionService = ConexionServiceTienda.create()
                        val buscarProductos = BuscarProductos(cedula, producto)
                        val response = conexionService.buscarProductos(buscarProductos)

                        if (response.isSuccessful && response.body() != null) {
                             lista = response.body()!!


                            launch(Dispatchers.Main) {
                                val dialog = BuscarProductoDialog(producto, lista) { seleccionado ->
                                    binding.etNombreProducto.setText(seleccionado.nombre)
                                    binding.etDescripcionProducto.setText(seleccionado.presentacion)
                                    binding.etCantidadProducto.setText(seleccionado.cantidad.toString())
                                    binding.etPrecioProducto.setText(seleccionado.valorVenta.toString())
                                    binding.btnGuardarProducto.isEnabled = true
                                    binding.cvFormulario.alpha = 1f
                                    binding.btnGuardarProducto.setOnClickListener {
                                        editarProducto(binding,seleccionado )
                                    }
                                }
                                dialog.show(supportFragmentManager, "buscar_producto")

                            }
                        } else {
                            launch(Dispatchers.Main) {
                                Toast.makeText(this@fragment_editar_producto, "No se encontraron productos", Toast.LENGTH_SHORT).show()
                            }
                        }

                    } catch (e: Exception) {

                        launch(Dispatchers.Main) {
                            Log.e("ErrorAPI", "Mensaje: ${e.message}")
                            Toast.makeText(this@fragment_editar_producto, "No se han encontrado Coincidencias", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }

    }

    fun editarProducto(binding: ActivityFragmentEditarProductoBinding, producto: EditarProducto){
        var succesful: Map<String,String>
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val conexionService = ConexionServiceTienda.create()
                val productoEditado = EditarProducto(binding.etCantidadProducto.text.toString().toIntOrNull()!!,producto.idInventario,producto.idProductos,binding.etNombreProducto.text.toString().lowercase(),binding.etDescripcionProducto.text.toString(),binding.etPrecioProducto.text.toString().toIntOrNull()!!)

                val response = conexionService.editarProducto(productoEditado)
                succesful = response.body()!!
                launch(Dispatchers.Main){
                    if (succesful.containsKey("succesful")){
                        Toast.makeText(this@fragment_editar_producto,"El producto se ha editado con éxito", Toast.LENGTH_SHORT).show()
                        Log.d("Succesful","El producto se ha editado con Éxito")
                    }
                    else {
                        Log.d("Error","Ha habido un error al intentar editar el producto, por favorr vuelve a intentarlo")
                        Toast.makeText(this@fragment_editar_producto,"Ha habido un error al intentar editar el producto, por favorr vuelve a intentarlo", Toast.LENGTH_LONG).show()
                    }
                }



            }

            catch (e: Exception){

            }

        }

    }

    fun inicializarEditarProducto(binding: ActivityFragmentEditarProductoBinding){


        binding.etNombreProducto.setText("")
        binding.etPrecioProducto.setText("")
        binding.etDescripcionProducto.setText("")
        binding.etCantidadProducto.setText("")
        binding.etBuscarProducto.setText("")
        binding.btnGuardarProducto.isEnabled = false
        binding.cvFormulario.alpha = 0.5f



    }
}