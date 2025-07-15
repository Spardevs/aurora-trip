package br.com.ticpass.pos.view.ui.login.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import br.com.ticpass.pos.R
import br.com.ticpass.pos.data.api.PosItem

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
            tvClosing.text = if (item.session?.closing === "") "Fechado" else "Aberto"

            tvCommission.text = item.commission
                ?.toString()
                ?.plus("%")
                ?: "Não há comissão"
            itemView.setOnClickListener { onClick(item) }
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
