package uk.mrs.saralarm.ui.settings.extra_ui.phone_numbers

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
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.Gson
import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.Phonenumber
import kotlinx.android.synthetic.main.settings_sms_numbers_fragment.view.*
import kotlinx.android.synthetic.main.settings_sms_numbers_recycler_view_row.view.*
import uk.mrs.saralarm.R
import uk.mrs.saralarm.ui.settings.extra_ui.support.ItemTouchViewHolder
import kotlin.jvm.internal.Intrinsics


class SMSNumbersRecyclerViewAdapter(val context: Context,
                                    val data: ArrayList<String>,
                                    val view: View
) : RecyclerView.Adapter<SMSNumbersRecyclerViewAdapter.ViewHolder?>() {

    private val mInflater: LayoutInflater = LayoutInflater.from(context)
    val phoneUtil: PhoneNumberUtil = PhoneNumberUtil.getInstance()
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
                undoSnackBar?.anchorView = view.sms_numbers_fab
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
            editor.putString("respondSMSNumbersJSON", "")
        else
            editor.putString("respondSMSNumbersJSON", Gson().toJson(data as Any))

        editor.apply()
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view: View = mInflater.inflate(R.layout.settings_sms_numbers_recycler_view_row, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (data[holder.adapterPosition].isBlank()) {
            holder.myTextView.setText("")
        } else {
            try {
                if (!phoneUtil.isValidNumber(phoneUtil.parse(data[holder.adapterPosition], "GB"))) {
                    holder.textInput.error = "SMS Number is in the wrong format"
                } else {
                    holder.textInput.error = ""
                }
            } catch (e: NumberParseException) {
                holder.textInput.error = "SMS Number is in the wrong format"
            }
            holder.myTextView.setText(data[holder.adapterPosition])
        }
    }


    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), ItemTouchViewHolder, View.OnClickListener {
        var myTextView: TextInputEditText = itemView.sms_numbers_edit_text
        var textInput: TextInputLayout = itemView.sms_numbers_recycler_text_input

        init {
            myTextView.inputType = 3
            myTextView.maxLines = 1
            myTextView.addTextChangedListener(object : TextWatcher {

                override fun afterTextChanged(editable: Editable) {
                    if (editable.isNotBlank())
                        try {
                            val formattedNumber: Phonenumber.PhoneNumber = phoneUtil.parse(editable.toString(), "GB")
                            if (!phoneUtil.isValidNumber(formattedNumber)) {
                                textInput.error = "SMS Number is in the wrong format"
                            } else {
                                myTextView.removeTextChangedListener(this)
                                val prevSelection: Int = myTextView.selectionStart
                                val prevLength: Int = myTextView.length()
                                myTextView.setText(phoneUtil.format(formattedNumber, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL))
                                myTextView.setSelection(
                                    if (myTextView.length() - prevLength + prevSelection > 0) {
                                        myTextView.length() - prevLength + prevSelection
                                    } else {
                                        0
                                    }
                                )
                                textInput.error = ""
                                myTextView.addTextChangedListener(this)
                            }
                        } catch (e: NumberParseException) {
                            textInput.error = "SMS Number is in the wrong format"
                        }
                    if (adapterPosition >= 0 && adapterPosition < data.size) {
                        data[adapterPosition] = myTextView.text.toString()
                    }
                }

                override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            })
        }

        override fun onItemSelected() {
            val animator = ObjectAnimator.ofFloat(itemView.sms_recycler_card_view, "cardElevation", dipToPixels(2.0f), dipToPixels(10.0f))
            animator.interpolator = AccelerateInterpolator()
            animator.start()
        }

        override fun onItemClear() {
            Intrinsics.checkNotNullExpressionValue(itemView, "itemView")
            val animator = ObjectAnimator.ofFloat(itemView.sms_recycler_card_view, "cardElevation", dipToPixels(10.0f), dipToPixels(2.0f))
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