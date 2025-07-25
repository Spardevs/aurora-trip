import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.recyclerview.widget.RecyclerView
import br.com.ticpass.pos.R

data class PaymentMethod(
    val type: PaymentType,
    @DrawableRes val iconRes: Int,
    val label: String
)
enum class PaymentType {
    PIX, CREDIT_CARD, DEBIT, CASH, VR
}


class PaymentAdapter(
    private val items: List<PaymentMethod>,
    private val onItemClick: (PaymentMethod) -> Unit
) : RecyclerView.Adapter<PaymentAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val icon  = view.findViewById<ImageView>(R.id.ivIcon)
        private val label = view.findViewById<TextView>(R.id.tvLabel)

        fun bind(item: PaymentMethod) {
            icon.setImageResource(item.iconRes)
            label.text = item.label
            itemView.setOnClickListener {
                onItemClick(item)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_payment_method, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size
}
