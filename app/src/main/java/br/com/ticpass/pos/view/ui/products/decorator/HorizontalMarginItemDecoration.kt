package br.com.ticpass.pos.view.ui.products.decorator

import android.content.Context
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import br.com.ticpass.pos.R

class HorizontalMarginItemDecoration(
    context: Context,
    private val nextItemVisible: Int,
    private val currentItemHorizontalMargin: Int
) : RecyclerView.ItemDecoration() {

    private val marginPx: Int = context.resources.getDimension(R.dimen.viewpager_item_margin).toInt()

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        super.getItemOffsets(outRect, view, parent, state)

        val itemPosition = parent.getChildAdapterPosition(view)
        val itemCount = state.itemCount

        if (itemPosition == 0) {
            outRect.left = currentItemHorizontalMargin
        } else {
            outRect.left = marginPx
        }

        if (itemPosition == itemCount - 1) {
            outRect.right = currentItemHorizontalMargin
        } else {
            outRect.right = nextItemVisible
        }
    }
}