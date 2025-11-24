package dam.moviles.pruebafirebase_dual.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import dam.moviles.pruebafirebase_dual.data.Finca
import dam.moviles.pruebafirebase_dual.databinding.ItemFincaBinding
import java.util.Locale

class FincaAdapter(
    context: Context,
    private val data: MutableList<Finca>
) : BaseAdapter() {

    private val inflater = LayoutInflater.from(context)

    fun updateData(nuevasFincas: List<Finca>) {
        data.clear()
        data.addAll(nuevasFincas)
        notifyDataSetChanged()
    }

    override fun getCount(): Int = data.size

    override fun getItem(position: Int): Finca = data[position]

    override fun getItemId(position: Int): Long = data[position].id.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val binding: ItemFincaBinding
        val view: View

        if (convertView == null) {
            binding = ItemFincaBinding.inflate(inflater, parent, false)
            view = binding.root
            view.tag = binding
        } else {
            view = convertView
            binding = convertView.tag as ItemFincaBinding
        }

        val finca = getItem(position)
        binding.textNombre.text = finca.nombre
        binding.textUbicacion.text = finca.ubicacion ?: "-"
        binding.textEstado.text = finca.estado.lowercase(Locale.getDefault())
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

        val superficieTexto = finca.superficie?.let { String.format(Locale.getDefault(), "%.2f ha", it) } ?: "-"
        binding.textSuperficie.text = superficieTexto

        return view
    }
}

