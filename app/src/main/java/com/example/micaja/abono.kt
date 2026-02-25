package com.example.micaja

import android.app.AlertDialog
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.lifecycle.lifecycleScope
import com.example.micaja.ConexionService.ConexionServiceTienda
import com.example.micaja.databinding.AbonoBinding
import com.example.micaja.models.Credito
import com.example.micaja.models.Datos_Abono
import com.example.micaja.models.Identificacion
import com.example.micaja.models.cliente
import com.example.micaja.models.clienteNuevo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class Abono: Fragment() {
    lateinit var binding: AbonoBinding
    private var buscar = false
    private var montoadeber = 0
    private var abono = false
    private var cc_cliente: String = ""
    private val estadoNormal = ConstraintSet() //Reinicia las márgenes

    val api = ConexionServiceTienda.create()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = AbonoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        abono = arguments?.getBoolean("abono")?:false
        estadoNormal.clone(binding.constraint as ConstraintLayout)

        binding.btnRetroceso.setOnClickListener { parentFragmentManager.popBackStack() }

        if (abono == true) {
            binding.textView.setText("Abono")

        } else {
            binding.textView.setText("Crédito")
        }

        binding.buscar.setOnClickListener {
            if (identificacion_validaciones() == true) {
                if (buscar) {
                    // Modo 2: El botón es 'Cambiar', resetear la UI.
                    buscar()
                } else {
                    lifecycleScope.launch(Dispatchers.IO) {
                        // Modo 1: El botón es 'Buscar', iniciar la búsqueda.
                        buscar_bdd(abono)
                    }
                }
            }
        }
        binding.boton.setOnClickListener {
            if (abono ==false){
                lifecycleScope.launch(Dispatchers.IO) {
                    insertar_credito()
                }
            }else{
                lifecycleScope.launch(Dispatchers.IO) {
                    insertar_abono()
                }

            }
        }




    }

    fun identificacion_validaciones(): Boolean {
        val identificacion = binding.identificacion.text.toString()
        if (identificacion.isEmpty()) {
            Toast.makeText(
                requireContext(),
                "El campo de identificación, no puede estar vacío",
                Toast.LENGTH_SHORT
            ).show()
            return false
        } else if (identificacion.length < 8 || identificacion.length > 10) {
            Toast.makeText(
                requireContext(),
                "El campo de identificación, mínimo debe tener 8 números y máximo 10.",
                Toast.LENGTH_SHORT
            ).show()
            return false
        }
        return true

    }

    suspend fun buscar_bdd(abono: Boolean?):Boolean {
        val prefs = context?.getSharedPreferences("SesionTendero", MODE_PRIVATE)
        val cedula = prefs?.getString("cedula", null)
        val id = Identificacion(binding.identificacion.text.toString(), cedula!!)
        withContext(Dispatchers.Main) {

        }
        val response = api.busca_cliente(id)
        if (response.isNotEmpty()) {
            val clienteEncontrado = response.first()
            Log.i("llego info", response.toString())
            cc_cliente = clienteEncontrado.cedula!!
            montoadeber = clienteEncontrado.total?:0
            withContext(Dispatchers.Main) {

                if (clienteEncontrado.creditos == 0) {
                    activar_campos_cero(
                        clienteEncontrado.nombre.toString(),
                        clienteEncontrado.celular.toString(),
                        abono
                    )

                } else {
                    activar_campos_uno(
                        clienteEncontrado.nombre.toString(),
                        clienteEncontrado.celular.toString(),
                        abono, clienteEncontrado.creditos!!, clienteEncontrado.total!!
                    )
                }

            }
            return true


        } else {
            withContext(Dispatchers.Main) {
                if (abono == false) {
                    AlertDialog.Builder(requireContext())
                        .setTitle("El cliente no está registrado")
                        .setMessage("¿Te gustaría registrarlo?")
                        .setPositiveButton("Sí") { dialog, _ ->
                            activar_registro()

                            dialog.dismiss()
                            binding.boton.setOnClickListener {
                                lifecycleScope.launch(Dispatchers.IO) {
                                    inserta_cliente_credito()
                                }
                            }

                        }


                        .setNegativeButton("No") { dialog, _ ->
                            dialog.dismiss()
                        }
                        .show()
                }

            else if (abono == true) {
                    AlertDialog.Builder(requireContext())
                        .setTitle("El cliente no esta registrado")
                        .setMessage("Ve a credito para registrarlo")
                        .setPositiveButton("Cerrar") { dialog, _ ->
                            dialog.dismiss()

                        }
                        .show()


                }
                Log.i("No llego info", response.toString())
            }
        }
        return false
    }

    private suspend fun inserta_cliente_credito() {
        val prefs = context?.getSharedPreferences("SesionTendero", MODE_PRIVATE)
        val cedula = prefs?.getString("cedula", null)
        withContext(Dispatchers.Main) {
            if (validaciones_cliente_nuevo() == true) {
                Log.i("informacion", "Se metio en el if")
                val cliente_nuevo = clienteNuevo(
                    binding.identificacion.text.toString(),
                    cedula!!,
                    binding.camponombre.text.toString(),
                    binding.campotelefono.text.toString(),
                    binding.campomonto.text.toString().toInt()
                )
                    val response = api.insertar_cliente(cliente_nuevo)

                    if (response.isSuccessful){
                        Toast.makeText(requireContext(), "El cliente ha sido registrado con éxito", Toast.LENGTH_SHORT).show()
                        parentFragmentManager.popBackStack()
                    }


            }
        }
    }

    private suspend fun insertar_credito() {
        val prefs = context?.getSharedPreferences("SesionTendero", MODE_PRIVATE)
        val cedula = prefs?.getString("cedula", null)
        withContext(Dispatchers.Main) {
            if (validaciones_credito() == true) {
                val credito = Credito(
                    cc_cliente,
                    binding.campomonto.text.toString().toInt(),
                    cedula!!
                )
                val response = api.insertar_credito(credito)
                if (response.isSuccessful) {
                    Toast.makeText(
                        requireContext(),
                        "El credito ha sido registrado con éxito",
                        Toast.LENGTH_SHORT
                    ).show()
                    parentFragmentManager.popBackStack()
                }
            }
        }
    }

    private suspend fun insertar_abono() {
        val prefs = context?.getSharedPreferences("SesionTendero", MODE_PRIVATE)
        val cedula = prefs?.getString("cedula", null)
        withContext(Dispatchers.Main) {
            if (validaciones_credito() == true) {
                val abono = Datos_Abono(cc_cliente,cedula!!, binding.campomonto.text.toString().toInt())
                val response = api.abono(abono)
                if (response.isSuccessful) {
                    Toast.makeText(
                        requireContext(),
                        "El abono se ha sido registrado con éxito",
                        Toast.LENGTH_SHORT
                    ).show()
                    parentFragmentManager.popBackStack()
                }

            }
        }
    }


    private fun validaciones_credito(): Boolean{
        val identificacion = binding.identificacion.text.toString()
        val monto = binding.campomonto.text.toString()
        if (identificacion.isEmpty()) {
            Toast.makeText(
                requireContext(),
                "El campo de identificación no puede estar vacío. Por favor digite el numero de identificación",
                Toast.LENGTH_SHORT
            ).show()
            return false
        } else if (identificacion.length < 8 || identificacion.length > 10) {
            Toast.makeText(
                requireContext(),
                "El campo de identificación, minimo debe tener 8 números y máximo 10.",
                Toast.LENGTH_SHORT
            ).show()
            return false
    }
        else if (monto.isEmpty()) {
            Toast.makeText(
                requireContext(),
                "El campo de monto no puede estar vacío. Por favor digite el monto",
                Toast.LENGTH_SHORT
            ).show()
            return false
        }
        else if (monto.toInt() <= 0) {
            Toast.makeText(
                requireContext(),
                "El monto no puede ser cero.",
                Toast.LENGTH_SHORT
            ).show()
            return false
        } else if (abono == true && monto.toInt() > montoadeber) {
            Toast.makeText(
                requireContext(),
                "El monto a pagar no puede ser mayor a la deuda. Por favor digite el monto nuevamente",
                Toast.LENGTH_SHORT
            ).show()
            return  false
        }
        else if (abono == true && monto.toInt() <= 0) {
            Toast.makeText(
                requireContext(),
                "El monto a pagar no puede ser menor a cero. Por favor digite el monto nuevamente",
                Toast.LENGTH_SHORT
            ).show()
            return  false
        }
        return true
        }
    fun validaciones_cliente_nuevo(): Boolean {
        val identificacion = binding.identificacion.text.toString()
        val nombre = binding.camponombre.text.toString()
        val numero = binding.campotelefono.text.toString()
        val monto = binding.campomonto.text.toString()
        if (identificacion.isEmpty()) {
            Toast.makeText(
                requireContext(),
                "El campo de identificación no puede estar vacío. Por favor digite el número de identificación",
                Toast.LENGTH_SHORT
            ).show()
            return false
        } else if (identificacion.length < 8 || identificacion.length > 10) {
            Toast.makeText(
                requireContext(),
                "El campo de identificación, minimo debe tener mínimo 8 números y maximo 10.",
                Toast.LENGTH_SHORT
            ).show()
            return false
        }else if (nombre.isEmpty()){
            Toast.makeText(
                requireContext(),
                "El campo de nombre no puede estar vacío. Por favor digite el nombre",
                Toast.LENGTH_SHORT
            ).show()
            return false
        }else if (numero.isEmpty()){
            Toast.makeText(
                requireContext(),
                "El campo de número telefónico no puede estar vacío. Por favor digite el número",
                Toast.LENGTH_SHORT
            ).show()
            return false
        }else if (numero.length < 7 || numero.length > 10){
            Toast.makeText(
                requireContext(),
                "El número telefonico, mínimo debe tener minimo 7 números y máximo 10.",
                Toast.LENGTH_SHORT
            ).show()
            return false
        }else if (monto.isEmpty()){
            Toast.makeText(
                requireContext(),
                "El campo de monto no puede estar vacío. Por favor digite el monto",
                Toast.LENGTH_SHORT
            ).show()
            return false
            }
        else if (monto.toInt() <= 0){
            Toast.makeText(
                requireContext(),
                "El monto no puede ser cero.",
                Toast.LENGTH_SHORT
            ).show()
            return false
        }
        return true

    }




    private fun buscar() {
        // 1. Cambiar el estado
        buscar = false
        restaurar_posicion()

        // 2. Cambiar el texto del botón
        binding.buscar.text = "Buscar"
        // 3. Reactivar el EditText (input)
        binding.identificacion.isEnabled = true
        binding.identificacion.alpha = 1f
        // 4. Restaurar la apariencia del EditText y limpiar el texto
        binding.identificacion.setText("")
        binding.identificacion.clearFocus()



    }

    private fun activar_registro() {
        binding.nombre.visibility = View.VISIBLE
        binding.camponombre.visibility = View.VISIBLE
        binding.telefono.visibility = View.VISIBLE
        binding.campotelefono.visibility = View.VISIBLE
        binding.monto.visibility = View.VISIBLE
        val miConstraintLayout = binding.constraint as ConstraintLayout // o tu ID de layout
        val constraintSet = ConstraintSet()
        constraintSet.clone(miConstraintLayout)
        constraintSet.connect(
            R.id.nombre,
            ConstraintSet.TOP,
            R.id.identificacion,
            ConstraintSet.BOTTOM,
            25
        )
        constraintSet.applyTo(miConstraintLayout)
        binding.campomonto.visibility = View.VISIBLE
        binding.boton.visibility = View.VISIBLE
    }


    private fun activar_campos_cero(nombre: String, celular: String, abono: Boolean?) {

        if (abono == false) {
            binding.constraint2.visibility = View.VISIBLE
            binding.boton.visibility = View.VISIBLE
            binding.nombrePersona.setText(nombre)
            binding.telefonoPersona.setText(celular)
            binding.montoquedebe.setText("No tiene créditos pendientes")
            binding.campomonto.visibility = View.VISIBLE
            binding.monto.visibility = View.VISIBLE
            val miConstraintLayout = binding.constraint as ConstraintLayout // o tu ID de layout
            val constraintSet = ConstraintSet()
            constraintSet.clone(miConstraintLayout)
            constraintSet.connect(
                R.id.monto,
                ConstraintSet.TOP,
                R.id.constraint2,
                ConstraintSet.BOTTOM,
                25
            )
            constraintSet.applyTo(miConstraintLayout)

        }else{
            binding.campomonto.visibility = View.GONE
            binding.monto.visibility = View.GONE
            binding.boton.visibility = View.GONE
            binding.constraint2.visibility = View.VISIBLE
            binding.nombrePersona.setText(nombre)
            binding.telefonoPersona.setText(celular)
            binding.montoquedebe.setText("No tiene créditos pendientes")

        }
        }


    private fun activar_campos_uno(nombre: String, celular: String, abono: Boolean?, creditos: Int, total:Int){
        val colorInt = Color.parseColor("#F4DBFA")
        val colorStateList = ColorStateList.valueOf(colorInt)
        binding.constraint2.backgroundTintList = colorStateList
        if (abono == false) {
            binding.constraint2.visibility = View.VISIBLE
            binding.boton.visibility = View.VISIBLE
            binding.nombrePersona.setText(nombre)
            binding.telefonoPersona.setText(celular)
            binding.montoquedebe.setText("Tiene $$total pesos pendientes en créditos")
            binding.creditospendientes.visibility = View.VISIBLE
            binding.creditospendientes.setText("Créditos pendientes: $creditos")

            binding.campomonto.visibility = View.VISIBLE
            binding.monto.visibility = View.VISIBLE
            val miConstraintLayout = binding.constraint as ConstraintLayout // o tu ID de layout
            val constraintSet = ConstraintSet()
            constraintSet.clone(miConstraintLayout)
            constraintSet.connect(
                R.id.monto,
                ConstraintSet.TOP,
                R.id.constraint2,
                ConstraintSet.BOTTOM,
                25
            )
            constraintSet.applyTo(miConstraintLayout)

        }else{

            binding.boton.visibility = View.VISIBLE
            binding.constraint2.visibility = View.VISIBLE
            binding.montoquedebe.setText("Tiene $$total pesos pendientes en créditos")
            binding.creditospendientes.visibility = View.VISIBLE
            binding.creditospendientes.setText("Créditos pendientes: $creditos")
            binding.nombrePersona.setText(nombre)
            binding.telefonoPersona.setText(celular)
            binding.campomonto.visibility = View.VISIBLE
            binding.monto.visibility = View.VISIBLE

        }
    }



    private fun restaurar_posicion() {
        estadoNormal.applyTo(binding.constraint2 as ConstraintLayout)
        val colorInt = Color.parseColor("#DBDCFF")
        val colorStateList = ColorStateList.valueOf(colorInt)
        binding.constraint2.backgroundTintList = colorStateList
    }
}