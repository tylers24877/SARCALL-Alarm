package uk.mrs.saralarm.ui.settings.extra_ui.team_prefix

import android.animation.ObjectAnimator
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import android.text.Editable
import android.text.InputFilter
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
import kotlinx.android.synthetic.main.settings_team_prefix_fragment.view.*
import kotlinx.android.synthetic.main.settings_team_prefix_recycler_view_row.view.*
import uk.mrs.saralarm.R
import uk.mrs.saralarm.ui.settings.extra_ui.support.ItemTouchViewHolder
import kotlin.jvm.internal.Intrinsics


class TeamPrefixRecyclerViewAdapter(val context: Context,
                                    val data: ArrayList<String>,
                                    val view: View
) : RecyclerView.Adapter<TeamPrefixRecyclerViewAdapter.ViewHolder?>() {

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
                undoSnackBar?.anchorView = view.team_prefix_fab
                undoSnackBar?.show()
            }
            data.removeAt(adapterPosition)
            notifyItemRemoved(adapterPosition)
        }
    }

    fun addItem() {
        undoSnackBar?.dismiss()
        data.add("")
        notifyItemInserted(data.size)
    }

    fun saveData() {
        val list = ArrayList<String>()
        val it: Iterator<String> = data.iterator()
        while (it.hasNext()) {
            val t = it.next()
            if (t == "") {
                list.add(t)
            }
        }
        data.removeAll(list)

        val sharedPrefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val editor: SharedPreferences.Editor = sharedPrefs.edit()
        if (data.isEmpty())
            editor.putString("respondTeamPrefixJSON", "")
        else
            editor.putString("respondTeamPrefixJSON", Gson().toJson(data))

        editor.apply()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view: View = mInflater.inflate(R.layout.settings_team_prefix_recycler_view_row, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (data[holder.adapterPosition].isBlank()) {
            holder.myTextView.setText("")
        } else {
            holder.myTextView.setText(data[holder.adapterPosition])
        }
    }


    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), ItemTouchViewHolder, View.OnClickListener {
        var myTextView: TextInputEditText = itemView.team_prefix_edit_text

        init {
            myTextView.maxLines = 1
            myTextView.filters += InputFilter.AllCaps()
            myTextView.addTextChangedListener(object : TextWatcher {

                override fun afterTextChanged(editable: Editable) {
                    if (adapterPosition >= 0 && adapterPosition < data.size) {
                        data[adapterPosition] = myTextView.text.toString()
                    }
                }

                override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            })
            val filter =
                InputFilter { source, start, end, _, _, _ ->
                    for (i in start until end) {
                        if (Character.isWhitespace(source[i])) {
                            return@InputFilter ""
                        }
                    }
                    null
                }
            myTextView.filters += arrayOf(filter)
        }

        override fun onItemSelected() {
            val animator = ObjectAnimator.ofFloat(itemView.team_prefix_card_view, "cardElevation", dipToPixels(2.0f), dipToPixels(10.0f))
            animator.interpolator = AccelerateInterpolator()
            animator.start()
        }

        override fun onItemClear() {
            Intrinsics.checkNotNullExpressionValue(itemView, "itemView")
            val animator = ObjectAnimator.ofFloat(itemView.team_prefix_card_view, "cardElevation", dipToPixels(10.0f), dipToPixels(2.0f))
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