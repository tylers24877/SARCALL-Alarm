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
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.Gson
import kotlinx.android.synthetic.main.settings_custom_messages_fragment.view.*
import kotlinx.android.synthetic.main.settings_custom_messages_recycler_view_row.view.*
import uk.mrs.saralarm.R
import uk.mrs.saralarm.ui.settings.extra_ui.support.ItemTouchViewHolder
import java.util.*


class CustomMessagesRecyclerViewAdapter(val context: Context,
                                        val data: ArrayList<String>,
                                        val view: View
) : RecyclerView.Adapter<CustomMessagesRecyclerViewAdapter.ViewHolder?>() {

    private val mInflater: LayoutInflater = LayoutInflater.from(context)
    var undoSnackBar: Snackbar? = null

    override fun getItemCount(): Int {
        return data.size
    }

    fun swapItems(fromPosition: Int, toPosition: Int) {
        val original = data[fromPosition]
        data.removeAt(fromPosition)
        data.add(toPosition, original)
        notifyItemMoved(fromPosition, toPosition)
    }

    fun removeItems(adapterPosition: Int, allowUndo: Boolean) {
        if (adapterPosition >= 0 && adapterPosition < data.size) {
            if (allowUndo) {
                val temp = data[adapterPosition]

                undoSnackBar = Snackbar.make(view, "Deleted", Snackbar.LENGTH_LONG).apply {
                    setAction("Undo") {
                        data.add(adapterPosition, temp)
                        notifyDataSetChanged()
                    }
                    duration = 9000
                }
                undoSnackBar?.anchorView = view.custom_message_fab
                undoSnackBar?.show()
            }
            data.removeAt(adapterPosition)
            notifyItemRemoved(adapterPosition)
        }
    }

    fun addItem() {
        undoSnackBar?.dismiss()
        data.add("")
        notifyDataSetChanged()
    }

    fun saveData() {
        //mData.removeAll(Collections.singleton(""))

        val sharedPrefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val editor: SharedPreferences.Editor = sharedPrefs.edit()
        if (data.isEmpty())
            editor.putString("customMessageJSON", "")
        else
            editor.putString("customMessageJSON", Gson().toJson(data))
        editor.apply()
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view: View = mInflater.inflate(R.layout.settings_custom_messages_recycler_view_row, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (data[holder.layoutPosition].isBlank()) {
            holder.myTextView.setText("")
            holder.myTextView.hint = "Type Here..."
        } else {
            holder.myTextView.setText(data[holder.layoutPosition])
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

                    data[adapterPosition] = myTextView.text.toString()

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
            val resources: Resources = context.resources
            metrics = resources.displayMetrics
            return TypedValue.applyDimension(1, dipValue, metrics)
        }
        override fun onClick(v: View?) {}
    }
}