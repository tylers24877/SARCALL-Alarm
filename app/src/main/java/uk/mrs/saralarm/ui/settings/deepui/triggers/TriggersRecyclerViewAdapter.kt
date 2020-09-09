package uk.mrs.saralarm.ui.settings.deepui.triggers

import android.animation.ObjectAnimator
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import android.text.Editable
import android.text.TextWatcher
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.widget.EditText
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import kotlinx.android.synthetic.main.settings_triggers_recycler_view_row.view.*
import uk.mrs.saralarm.R
import uk.mrs.saralarm.support.ItemTouchViewHolder
import java.util.*
import kotlin.jvm.internal.Intrinsics


class TriggersRecyclerViewAdapter(context: Context, data: ArrayList<String>) : RecyclerView.Adapter<TriggersRecyclerViewAdapter.ViewHolder?>(){
    var mContext: Context = context
    private val mData: ArrayList<String> = data
    private val mInflater: LayoutInflater = LayoutInflater.from(context)

    override fun getItemCount(): Int {
        return mData.size
    }
    fun swapItems(fromPosition: Int, toPosition: Int) {
        val original = mData[fromPosition]
        mData.removeAt(fromPosition)
        mData.add(toPosition, original)
        notifyItemMoved(fromPosition, toPosition)
    }

    fun removeItems(adapterPosition: Int) {
        mData.removeAt(adapterPosition)
        notifyItemRemoved(adapterPosition)
    }

    fun addItem() {
        mData.add("")
        notifyDataSetChanged()
    }

    fun saveData() {
        mData.removeAll(Collections.singleton(""))

        val sharedPrefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext)
        val editor: SharedPreferences.Editor = sharedPrefs.edit()
        editor.putString("triggersJSON", Gson().toJson(mData))
        editor.apply()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view: View = mInflater.inflate(R.layout.settings_triggers_recycler_view_row, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        holder.textView.setText(mData[holder.adapterPosition])

        holder.textView.addTextChangedListener(object : TextWatcher {

            override fun afterTextChanged(s: Editable) {
            mData[holder.adapterPosition]= holder.textView.text.toString()
            }
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
        })
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), ItemTouchViewHolder, View.OnClickListener {
        var textView: EditText = itemView.triggersCustomMessageEditText

        override fun onItemSelected() {
            val animator = ObjectAnimator.ofFloat(itemView.triggers_recycler_cardview, "cardElevation", dipToPixels(2.0f), dipToPixels(10.0f))
            animator.interpolator = AccelerateInterpolator()
            animator.start()
        }

        override fun onItemClear() {
            Intrinsics.checkNotNullExpressionValue(itemView, "itemView")
            val animator = ObjectAnimator.ofFloat(itemView.triggers_recycler_cardview, "cardElevation", dipToPixels(10.0f), dipToPixels(2.0f))
            animator.interpolator = AccelerateInterpolator()
            animator.start()
        }

        private fun dipToPixels(dipValue: Float): Float {
            val metrics: DisplayMetrics?
            val resources: Resources = mContext.resources
            metrics = resources.displayMetrics
            return TypedValue.applyDimension(1, dipValue, metrics)
        }
        override fun onClick(v: View?) {}
    }
}