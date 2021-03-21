package uk.mrs.saralarm.ui.settings.extra_ui.support

import android.content.Context
import android.graphics.Canvas
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView


class DragAdapter(private val dragListener: DragListener, context: Context, dragDirs: Int, swipeDirs: Int) :
    ItemTouchHelper.SimpleCallback(dragDirs, swipeDirs) {

    private val dragAdapterUtil = DragAdapterUtil(context)

    override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
        dragListener.swapItems(viewHolder.adapterPosition, target.adapterPosition)
        return true
    }

    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        if (actionState != 0 && viewHolder is ItemTouchViewHolder) {
            (viewHolder as ItemTouchViewHolder).onItemSelected()
        }
        super.onSelectedChanged(viewHolder, actionState)
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)
        if (viewHolder is ItemTouchViewHolder) {
            (viewHolder as ItemTouchViewHolder).onItemClear()
        }
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        dragListener.removeItems(viewHolder.adapterPosition, true)
    }

    override fun onChildDraw(c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {
        dragAdapterUtil.onChildDraw(c, viewHolder, dX, isCurrentlyActive)
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }
}

interface DragListener {
    fun removeItems(adapterPosition: Int, allowUndo: Boolean)
    fun swapItems(fromPosition: Int, toPosition: Int)
}