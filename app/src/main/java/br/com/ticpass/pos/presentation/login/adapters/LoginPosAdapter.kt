package br.com.ticpass.pos.presentation.login.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import br.com.ticpass.pos.R
import br.com.ticpass.pos.core.util.NumericConversionUtils
import br.com.ticpass.pos.domain.pos.model.Pos

class LoginPosAdapter(
    private val onClick: (Pos) -> Unit,
    private val onLongClick: (Pos) -> Unit
) : RecyclerView.Adapter<LoginPosAdapter.VH>() {

    private var items: List<Pos> = emptyList()

    fun setItems(list: List<Pos>) {
        items = list
        notifyDataSetChanged()
    }

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        private val iv = view.findViewById<ImageView>(R.id.imageViewPos)
        private val tvState = view.findViewById<TextView>(R.id.textViewPosClosing)
        private val tvName = view.findViewById<TextView>(R.id.textViewPosName)
        private val tvCommission = view.findViewById<TextView>(R.id.textViewPOSCommission)

        private val numericUtils = NumericConversionUtils

        fun bind(p: Pos) {
            tvName.text = "${p.prefix} ${p.sequence}"
            val closed = p.session == null
            tvState.text = if (closed) "Fechado" else "Aberto"

            if (closed) {
                iv.setColorFilter(itemView.context.getColor(R.color.colorGreen))
                tvState.setTextColor(itemView.context.getColor(R.color.colorGreen))
            } else {
                iv.setColorFilter(itemView.context.getColor(R.color.colorRed))
                tvState.setTextColor(itemView.context.getColor(R.color.colorRed))
                tvName.setTextColor(itemView.context.getColor(R.color.colorGray))
                tvCommission.setTextColor(itemView.context.getColor(R.color.colorGray))
            }

            tvCommission.text = if (closed) {
                if (p.commission > 0) "${numericUtils.convertLongToPercentString(p.commission)} de comissão" else "Sem comissão"
            } else {
                p.session?.cashier?.name ?: "Caixa aberto"
            }

            // color-logic like old adapter (use resources as in original)
            itemView.setOnClickListener { if (closed) onClick(p) }
            itemView.setOnLongClickListener {
                if (!closed) {
                    onLongClick(p)
                    true
                } else false
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_login_pos, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])
    override fun getItemCount(): Int = items.size
}