package uk.mrs.saralarm.ui.settings.deepui.phone_numbers

import android.animation.ObjectAnimator
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import android.text.Editable
import android.text.InputFilter
import android.text.InputFilter.LengthFilter
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
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.Gson
import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.Phonenumber
import kotlinx.android.synthetic.main.settings_sms_numbers_recycler_view_row.view.*
import uk.mrs.saralarm.R
import uk.mrs.saralarm.support.ItemTouchViewHolder
import kotlin.jvm.internal.Intrinsics


class SMSNumbersRecyclerViewAdapter(context: Context, data: ArrayList<String>) : RecyclerView.Adapter<SMSNumbersRecyclerViewAdapter.ViewHolder?>() {
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
        val list = ArrayList<String>()
        val it: Iterator<String> = mData.iterator()
        while (it.hasNext()) {
            val t = it.next()
            if (t == "") {
                list.add(t)
            }
        }
        mData.removeAll(list)

        val sharedPrefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext)
        val editor: SharedPreferences.Editor = sharedPrefs.edit()
        if (mData.isEmpty())
            editor.putString("respondSMSNumbersJSON", "")
        else
            editor.putString("respondSMSNumbersJSON", Gson().toJson(mData as Any))

        editor.apply()
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view: View = mInflater.inflate(R.layout.settings_sms_numbers_recycler_view_row, parent, false)
        return ViewHolder(this, view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val phoneUtil: PhoneNumberUtil = PhoneNumberUtil.getInstance()
        if (mData[holder.adapterPosition].isBlank()) {
            holder.myTextView.setText("")
        } else {
            try {
                if (!phoneUtil.isValidNumber(phoneUtil.parse(mData[holder.adapterPosition], "GB"))) {
                    holder.textInput.error = "SMS Number is in the wrong format"
                } else {
                    holder.textInput.error = ""
                }
            } catch (e: NumberParseException) {
                holder.textInput.error = "SMS Number is in the wrong format"
            }
            holder.myTextView.setText(mData[holder.adapterPosition])
        }
        holder.myTextView.inputType = 3
        holder.myTextView.maxLines = 1
        holder.myTextView.filters = arrayOf<InputFilter>(LengthFilter(16))


        holder.myTextView.addTextChangedListener(object : TextWatcher {
            var editing = false

            override fun afterTextChanged(s: Editable) {
                if (!editing) {
                    try {
                        val formattedNumber: Phonenumber.PhoneNumber = phoneUtil.parse(s.toString(), "GB")
                        if (!phoneUtil.isValidNumber(formattedNumber)) {
                            holder.textInput.error = "SMS Number is in the wrong format"
                        } else {
                            editing = true
                            val prevSelection: Int = holder.myTextView.selectionStart
                            val prevLength: Int = holder.myTextView.length()
                            holder.myTextView.setText(phoneUtil.format(formattedNumber, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL))
                            holder.myTextView.setSelection(holder.myTextView.length() - prevLength + prevSelection)
                            holder.textInput.error = ""
                        }
                    } catch (e: NumberParseException) {
                        holder.textInput.error = "SMS Number is in the wrong format"
                    }
                    editing = false
                }
                mData[holder.adapterPosition] = holder.myTextView.text.toString()
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
        })
    }

    inner class ViewHolder(v: SMSNumbersRecyclerViewAdapter, itemView: View) : RecyclerView.ViewHolder(itemView), ItemTouchViewHolder, View.OnClickListener {
        var myTextView: TextInputEditText = itemView.SMSNumbersCustomMessageEditText
        var textInput: TextInputLayout = itemView.SMSNumbersRecyclerTextInput

        override fun onItemSelected() {
            val animator = ObjectAnimator.ofFloat(itemView.sms_recycler_cardview, "cardElevation", dipToPixels(2.0f), dipToPixels(10.0f))
            animator.interpolator = AccelerateInterpolator()
            animator.start()
        }

        override fun onItemClear() {
            Intrinsics.checkNotNullExpressionValue(itemView, "itemView")
            val animator = ObjectAnimator.ofFloat(itemView.sms_recycler_cardview, "cardElevation", dipToPixels(10.0f), dipToPixels(2.0f))
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