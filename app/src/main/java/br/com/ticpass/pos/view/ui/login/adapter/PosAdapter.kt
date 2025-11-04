package br.com.ticpass.pos.view.ui.login.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import br.com.ticpass.pos.R
import br.com.ticpass.pos.data.api.PosItem
import br.com.ticpass.pos.util.calculatePercent

class PosAdapter(
    private val items: MutableList<PosItem> = mutableListOf(),
    private val onClick: (PosItem) -> Unit
) : RecyclerView.Adapter<PosAdapter.PosViewHolder>() {

    inner class PosViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvName= view.findViewById<TextView>(R.id.textViewPosName)
        private val tvClosing = view.findViewById<TextView>(R.id.textViewPosClosing)
        private val tvCommission = view.findViewById<TextView>(R.id.textViewPOSCommission)

        fun bind(item: PosItem) {
            tvName.text = item.name
            val isClosed = item.session?.closing == ""
            tvClosing.text = if (isClosed) "Fechado" else "Aberto"

            val commissionConverted = item.commission?.let { if (it > 0.toBigInteger()) "${calculatePercent(it)}% de comissão" else "Sem comissão" }
            tvCommission.text = commissionConverted ?: "Sem comissão"

            if (isClosed) {
                // Ícone vermelho
                itemView.findViewById<ImageView>(R.id.imageViewPos).setColorFilter(
                    itemView.context.getColor(R.color.colorRed)
                )
                // Textos cinza
                tvClosing.setTextColor(itemView.context.getColor(R.color.colorGray))
                tvName.setTextColor(itemView.context.getColor(R.color.colorGray))
                tvCommission.setTextColor(itemView.context.getColor(R.color.colorGray))
                // Desabilitar clique
                itemView.setOnClickListener(null)
            } else {
                // Ícone verde (padrão)
                itemView.findViewById<ImageView>(R.id.imageViewPos).setColorFilter(
                    itemView.context.getColor(R.color.colorGreen)
                )
                // Textos padrão
                tvClosing.setTextColor(itemView.context.getColor(R.color.colorGreen))
                tvName.setTextColor(itemView.context.getColor(R.color.design_default_color_on_secondary))
                tvCommission.setTextColor(itemView.context.getColor(R.color.colorBlack))
                // Habilitar clique
                itemView.setOnClickListener { onClick(item) }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PosViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pos, parent, false)
        return PosViewHolder(view)
    }

    override fun onBindViewHolder(holder: PosViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    @SuppressLint("NotifyDataSetChanged")
    fun setItems(newItems: List<PosItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}
