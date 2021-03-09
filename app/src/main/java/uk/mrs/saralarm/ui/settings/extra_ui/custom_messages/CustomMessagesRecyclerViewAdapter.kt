package uk.mrs.saralarm.ui.settings.extra_ui.custom_messages

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
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.Gson
import kotlinx.android.synthetic.main.settings_custom_messages_recycler_view_row.view.*
import uk.mrs.saralarm.R
import uk.mrs.saralarm.ui.settings.extra_ui.support.ItemTouchViewHolder
import java.util.*


class CustomMessagesRecyclerViewAdapter(context: Context, data: ArrayList<String>) : RecyclerView.Adapter<CustomMessagesRecyclerViewAdapter.ViewHolder?>(){
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
        if (adapterPosition >= 0 && adapterPosition < mData.size) {
            mData.removeAt(adapterPosition)
            notifyItemRemoved(adapterPosition)
        }
    }

    fun addItem() {
        mData.add("")
        notifyDataSetChanged()
    }

    fun saveData() {
        //mData.removeAll(Collections.singleton(""))

        val sharedPrefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext)
        val editor: SharedPreferences.Editor = sharedPrefs.edit()
        if (mData.isEmpty())
            editor.putString("customMessageJSON", "")
        else
            editor.putString("customMessageJSON", Gson().toJson(mData))
        editor.apply()
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view: View = mInflater.inflate(R.layout.settings_custom_messages_recycler_view_row, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (mData[holder.layoutPosition].isBlank()) {
            holder.myTextView.setText("")
            holder.myTextView.hint = "Type Here..."
        } else {
            holder.myTextView.setText(mData[holder.layoutPosition])
        }
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), ItemTouchViewHolder, View.OnClickListener {
        var myTextView: TextInputEditText = itemView.custom_message_edit_text

        init {
            myTextView.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

                }

                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

                }

                override fun afterTextChanged(s: Editable) {

                    mData[adapterPosition] = myTextView.text.toString()

                }
            })
        }

        override fun onItemSelected() {
            val animator = ObjectAnimator.ofFloat(itemView.custom_message_recycler_card_view, "cardElevation", dipToPixels(2.0f), dipToPixels(10.0f))
            animator.interpolator = AccelerateInterpolator()
            animator.start()
        }

        override fun onItemClear() {
            val animator = ObjectAnimator.ofFloat(itemView.custom_message_recycler_card_view, "cardElevation", dipToPixels(10.0f), dipToPixels(2.0f))
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