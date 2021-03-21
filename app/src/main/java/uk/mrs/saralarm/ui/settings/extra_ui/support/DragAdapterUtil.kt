package uk.mrs.saralarm.ui.settings.extra_ui.support

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.drawable.ColorDrawable
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getColor
import androidx.recyclerview.widget.RecyclerView
import uk.mrs.saralarm.R

class DragAdapterUtil(context: Context) {

    private val deleteIcon = ContextCompat.getDrawable(context, R.drawable.ic_baseline_delete_24)
    private val intrinsicWidth = deleteIcon!!.intrinsicWidth
    private val intrinsicHeight = deleteIcon!!.intrinsicHeight
    private val background = ColorDrawable()
    private val backgroundColor = getColor(context, R.color.error)
    private val clearPaint = Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR) }

    fun onChildDraw(c: Canvas,
                    viewHolder: RecyclerView.ViewHolder,
                    dX: Float,
                    isCurrentlyActive: Boolean
    ): Boolean {
        val itemView = viewHolder.itemView

        if (dX == 0f && !isCurrentlyActive) {
            clearCanvas(c, itemView.right + dX, itemView.top.toFloat(), itemView.right.toFloat(), itemView.bottom.toFloat())
            return false
        }
        background.color = backgroundColor
        val itemHeight = itemView.bottom - itemView.top
        val deleteIconTop = itemView.top + (itemHeight - intrinsicHeight) / 2
        val deleteIconBottom = deleteIconTop + intrinsicHeight
        val deleteIconMargin = (itemHeight - intrinsicHeight) / 4

        when {
            dX > 0 -> {//swipe to the right
                // Draw the red delete background
                background.setBounds(itemView.left, itemView.top, itemView.left + dX.toInt() + 30, itemView.bottom)
                background.draw(c)

                // Calculate position of delete icon
                val deleteIconLeft = itemView.left + deleteIconMargin
                val deleteIconRight = itemView.left + deleteIconMargin + intrinsicWidth

                // Draw the delete icon
                deleteIcon!!.setBounds(deleteIconLeft, deleteIconTop, deleteIconRight, deleteIconBottom)
                deleteIcon.draw(c)
            }
            dX < 0 -> {
                // Swiping to the left
                // Draw the red delete background
                background.setBounds(itemView.right + dX.toInt() - 30, itemView.top, itemView.right, itemView.bottom)
                background.draw(c)

                // Calculate position of delete icon
                val deleteIconLeft = itemView.right - deleteIconMargin - intrinsicWidth
                val deleteIconRight = itemView.right - deleteIconMargin

                // Draw the delete icon
                deleteIcon!!.setBounds(deleteIconLeft, deleteIconTop, deleteIconRight, deleteIconBottom)
                deleteIcon.draw(c)
            }
        }
        return true
    }

    private fun clearCanvas(c: Canvas?, left: Float, top: Float, right: Float, bottom: Float) {
        c?.drawRect(left, top, right, bottom, clearPaint)
    }
}