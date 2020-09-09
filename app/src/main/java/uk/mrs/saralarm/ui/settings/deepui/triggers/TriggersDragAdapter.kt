package uk.mrs.saralarm.ui.settings.deepui.triggers

import android.content.Context
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import uk.mrs.saralarm.support.ItemTouchViewHolder


class TriggersDragAdapter(adapterCustomMessages: TriggersRecyclerViewAdapter, context: Context?, dragDirs: Int, swipeDirs: Int) :
    ItemTouchHelper.SimpleCallback(dragDirs, swipeDirs) {

    private val mAdapter: TriggersRecyclerViewAdapter = adapterCustomMessages

    override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
        mAdapter.swapItems(viewHolder.adapterPosition, target.adapterPosition)
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
        mAdapter.removeItems(viewHolder.adapterPosition)
    }

}