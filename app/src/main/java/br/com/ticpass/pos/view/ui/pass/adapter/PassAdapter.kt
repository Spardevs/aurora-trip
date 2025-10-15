package br.com.ticpass.pos.view.ui.pass.adapter

import android.content.Context
import android.content.SharedPreferences
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import br.com.ticpass.pos.R
import br.com.ticpass.pos.util.generateEAN13BarcodeBitmap
import br.com.ticpass.pos.view.ui.pass.PassData
import br.com.ticpass.pos.view.ui.pass.PassType

class PassAdapter(
    private var passType: PassType,
    private var passList: List<PassData>
) : RecyclerView.Adapter<PassAdapter.PassViewHolder>() {

    companion object {
        private const val TYPE_COMPACT = 0
        private const val TYPE_EXPANDED = 1
        private const val TYPE_GROUPED = 2

        private const val PREFS_NAME = "ConfigPrefs"
        private const val PREF_KEY_PRINT_FORMAT = "print_format"

        fun mapFormatToPassType(format: String?): PassType {
            return when (format?.uppercase()) {
                "COMPACT" -> PassType.ProductCompact
                "EXPANDED" -> PassType.ProductExpanded
                "GROUPED" -> PassType.ProductGrouped
                "DEFAULT", null, "" -> PassType.ProductExpanded // padrão
                else -> PassType.ProductExpanded
            }
        }
    }

    private var currentFormatValue: String? = null

    abstract class PassViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        abstract fun bind(passData: PassData)

        protected fun bindCommonViews(passData: PassData) {
            val barcodeImage = itemView.findViewById<ImageView>(R.id.barcodeImageView)
            val barcodeBitmap = try {
                generateEAN13BarcodeBitmap(passData.header.barcode)
            } catch (_: Exception) {
                null
            }
            barcodeImage?.setImageBitmap(barcodeBitmap)

            itemView.findViewById<TextView>(R.id.attendantName)?.text = passData.footer.cashierName
            itemView.findViewById<TextView>(R.id.menuName)?.text = passData.footer.menuName
            itemView.findViewById<TextView>(R.id.passPrinter)?.text = passData.footer.printerInfo
            itemView.findViewById<TextView>(R.id.passDescription)?.text = passData.footer.description
            itemView.findViewById<TextView>(R.id.printTime)?.text = passData.footer.printTime

            itemView.findViewById<ImageView>(R.id.passCutInHere)?.visibility =
                if (passData.showCutLine) View.VISIBLE else View.GONE
        }
    }

    class CompactViewHolder(itemView: View) : PassViewHolder(itemView) {
        private val productName: TextView = itemView.findViewById(R.id.productName)
        private val productPrice: TextView = itemView.findViewById(R.id.productPrice)
        private val eventTitle: TextView = itemView.findViewById(R.id.eventTitle)
        private val eventTime: TextView = itemView.findViewById(R.id.eventTime)
        private val productObservation: TextView = itemView.findViewById(R.id.productObservation)

        override fun bind(passData: PassData) {
            passData.productData?.let {
                productName.text = it.name
                productPrice.text = it.price
                eventTitle.text = it.eventTitle
                eventTime.text = it.eventTime

                it.observation?.let { obs ->
                    productObservation.text = "Obs: $obs"
                    productObservation.visibility = View.VISIBLE
                } ?: run {
                    productObservation.visibility = View.GONE
                }
            }
            bindCommonViews(passData)
        }
    }

    class ExpandedViewHolder(itemView: View) : PassViewHolder(itemView) {
        private val productName: TextView = itemView.findViewById(R.id.productName)
        private val productPrice: TextView = itemView.findViewById(R.id.productPrice)
        private val eventTitle: TextView = itemView.findViewById(R.id.eventTitle)
        private val eventTime: TextView = itemView.findViewById(R.id.eventTime)
        private val productObservation: TextView = itemView.findViewById(R.id.productObservation)

        override fun bind(passData: PassData) {
            passData.productData?.let {
                productName.text = it.name
                productPrice.text = it.price
                eventTitle.text = it.eventTitle
                eventTime.text = it.eventTime

                if (!it.observation.isNullOrEmpty()) {
                    productObservation.text = "Obs: ${it.observation}"
                    productObservation.visibility = View.VISIBLE
                } else {
                    productObservation.visibility = View.GONE
                }
            }
            bindCommonViews(passData)
        }
    }

    class GroupedViewHolder(itemView: View) : PassViewHolder(itemView) {
        private val headerTitle: TextView = itemView.findViewById(R.id.headerTitle)
        private val headerDate: TextView = itemView.findViewById(R.id.headerDate)
        private val itemsContainer: LinearLayout = itemView.findViewById(R.id.itemsContainer)
        private val totalItems: TextView = itemView.findViewById(R.id.totalItems)

        override fun bind(passData: PassData) {
            headerTitle.text = passData.header.title
            headerDate.text = passData.header.date

            itemsContainer.removeAllViews()

            passData.groupedData?.items?.forEach { item ->
                val iv = LayoutInflater.from(itemView.context)
                    .inflate(R.layout.item_grouped_product, itemsContainer, false)
                iv.findViewById<TextView>(R.id.itemQuantity).text = "${item.quantity}x"
                iv.findViewById<TextView>(R.id.itemName).text = item.name
                iv.findViewById<TextView>(R.id.itemPrice).text = item.price
                itemsContainer.addView(iv)
            }

            passData.groupedData?.let {
                totalItems.text = "${it.totalItems} itens - ${it.totalPrice}"
            }

            bindCommonViews(passData)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (passType) {
            PassType.ProductCompact -> TYPE_COMPACT
            PassType.ProductExpanded -> TYPE_EXPANDED
            PassType.ProductGrouped -> TYPE_GROUPED
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PassViewHolder {
        return when (viewType) {
            TYPE_COMPACT -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.printer_pass_compact, parent, false)
                CompactViewHolder(view)
            }
            TYPE_EXPANDED -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.printer_pass_expanded, parent, false)
                ExpandedViewHolder(view)
            }
            TYPE_GROUPED -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.printer_pass_grouped, parent, false)
                GroupedViewHolder(view)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: PassViewHolder, position: Int) {
        holder.bind(passList[position])
    }

    override fun getItemCount() = passList.size

    fun updateData(newPassType: PassType, newPassList: List<PassData>) {
        passType = newPassType
        passList = newPassList
        notifyDataSetChanged()
    }

    fun initTypeFromPrefs(context: Context) {
        val prefs = getConfigPrefs(context)
        val format = prefs.getString(PREF_KEY_PRINT_FORMAT, "DEFAULT")
        currentFormatValue = format
        passType = mapFormatToPassType(format)
    }

    fun updateDataFromPrefs(context: Context, newPassList: List<PassData>) {
        val prefs = getConfigPrefs(context)
        val format = prefs.getString(PREF_KEY_PRINT_FORMAT, "DEFAULT")
        val newType = mapFormatToPassType(format)

        val changed = (currentFormatValue?.uppercase() ?: "") != (format?.uppercase() ?: "")
        currentFormatValue = format

        passType = newType
        passList = newPassList

        notifyDataSetChanged()
    }

    private fun getConfigPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
}