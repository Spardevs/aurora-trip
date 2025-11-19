package br.com.ticpass.pos.view.ui.login.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import br.com.ticpass.pos.R
// REMOVA esse import se não for mais usar PosItem
// import br.com.ticpass.pos.data.api.PosItem
import br.com.ticpass.pos.data.model.Pos
import br.com.ticpass.pos.util.calculatePercent
import java.math.BigInteger
import com.google.gson.JsonParser


class PosAdapter(
    private val items: MutableList<Pos> = mutableListOf(),
    private val onClick: (Pos) -> Unit,
    private val onLongClick: (Pos) -> Unit
) : RecyclerView.Adapter<PosAdapter.PosViewHolder>() {

    inner class PosViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvName = view.findViewById<TextView>(R.id.textViewPosName)
        private val tvClosing = view.findViewById<TextView>(R.id.textViewPosClosing)
        private val tvCommission = view.findViewById<TextView>(R.id.textViewPOSCommission)
        private val ivPos = view.findViewById<ImageView>(R.id.imageViewPos)

        fun bind(item: Pos) {
            tvName.text = item.name
            val isClosed = item.session == null
            tvClosing.text = if (isClosed) "Fechado" else "Aberto"

            if (isClosed) {
                val commissionConverted = item.commission?.let {
                    if (it > BigInteger.ZERO) "${calculatePercent(it)}% de comissão" else "Sem comissão"
                }
                tvCommission.text = commissionConverted ?: "Sem comissão"

                ivPos.setColorFilter(itemView.context.getColor(R.color.colorGreen))
                tvClosing.setTextColor(itemView.context.getColor(R.color.colorGreen))
                tvName.setTextColor(itemView.context.getColor(R.color.design_default_color_on_secondary))
                tvCommission.setTextColor(itemView.context.getColor(R.color.colorBlack))
                itemView.setOnClickListener { onClick(item) }
                itemView.setOnLongClickListener(null)
            } else {
                val cashierName = item.session?.cashier?.trim().takeIf { !it.isNullOrBlank() } ?: "Caixa aberto"
                tvCommission.text = cashierName
                ivPos.setColorFilter(itemView.context.getColor(R.color.colorRed))
                tvClosing.setTextColor(itemView.context.getColor(R.color.colorRed))
                tvName.setTextColor(itemView.context.getColor(R.color.colorGray))
                tvCommission.setTextColor(itemView.context.getColor(R.color.colorGray))

                // Set click listeners
                itemView.setOnClickListener(null)
                itemView.setOnLongClickListener {
                    onLongClick(item)
                    true
                }
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
    fun setItems(newItems: List<Pos>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}