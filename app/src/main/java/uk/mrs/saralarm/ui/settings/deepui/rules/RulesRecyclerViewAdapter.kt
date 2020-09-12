package uk.mrs.saralarm.ui.settings.deepui.rules

import android.animation.ObjectAnimator
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.os.Build
import android.text.Editable
import android.text.TextWatcher
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.widget.RadioGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.Gson
import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.Phonenumber
import kotlinx.android.synthetic.main.settings_rules_recycler_view_row.view.*
import uk.mrs.saralarm.R
import uk.mrs.saralarm.support.ItemTouchViewHolder
import uk.mrs.saralarm.ui.settings.deepui.rules.support.RulesChoice
import uk.mrs.saralarm.ui.settings.deepui.rules.support.RulesObject
import java.util.*
import kotlin.jvm.internal.Intrinsics


var TextView.drawableEnd: Drawable?
    get() = compoundDrawablesRelative[2]
    set(value) = setDrawables(end = value)

fun TextView.setDrawables(
    start: Drawable? = null,
    top: Drawable? = null,
    end: Drawable? = null,
    bottom: Drawable? = null
) = setCompoundDrawablesRelativeWithIntrinsicBounds(start, top, end, bottom)

class RulesRecyclerViewAdapter(context: Context, data: ArrayList<RulesObject>) : RecyclerView.Adapter<RulesRecyclerViewAdapter.ViewHolder?>() {
    var mContext: Context = context
    private val mData: ArrayList<RulesObject> = data
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
        mData.add(RulesObject())
        notifyDataSetChanged()
    }

    fun saveData() {
        val list = ArrayList<RulesObject>()
        val it: Iterator<RulesObject> = mData.iterator()
        while (it.hasNext()) {
            val t = it.next()
            when (t.choice) {
                RulesChoice.PHRASE ->
                    if (t.phrase == "") list.add(t)
                RulesChoice.SMS_NUMBER ->
                    if (t.smsNumber == "") list.add(t)
                RulesChoice.ALL -> {
                    if (t.smsNumber == "" && t.phrase == "") {
                        list.add(t)
                        break
                    }
                    if (t.smsNumber == "" && t.phrase != "") t.choice = RulesChoice.PHRASE else if (t.smsNumber != "" && t.phrase == "") t.choice = RulesChoice.SMS_NUMBER
                }
            }
        }
        mData.removeAll(list)

        val sharedPrefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext)
        val editor: SharedPreferences.Editor = sharedPrefs.edit()
        if (mData.isEmpty())
            editor.putString("rulesJSON", "")
        else
            editor.putString("rulesJSON", Gson().toJson(mData))

        editor.apply()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view: View = mInflater.inflate(R.layout.settings_rules_recycler_view_row, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val phoneUtil: PhoneNumberUtil = PhoneNumberUtil.getInstance()

        when (mData[holder.adapterPosition].choice) {
            RulesChoice.ALL -> {
                holder.rulesRadioGroup.check(R.id.rulesRadioBoth)
                holder.smsNumberTextInputLayout.visibility = View.VISIBLE
                holder.phraseRulesRecyclerTextInputLayout.visibility = View.VISIBLE
            }
            RulesChoice.SMS_NUMBER -> {
                holder.rulesRadioGroup.check(R.id.rulesRadioSMSNumber)
                holder.smsNumberTextInputLayout.visibility = View.VISIBLE
                holder.phraseRulesRecyclerTextInputLayout.visibility = View.GONE
            }
            RulesChoice.PHRASE -> {
                holder.rulesRadioGroup.check(R.id.rulesRadioTrigger)
                holder.smsNumberTextInputLayout.visibility = View.GONE
                holder.phraseRulesRecyclerTextInputLayout.visibility = View.VISIBLE
            }
        }

        if (mData[holder.adapterPosition].smsNumber.isNotBlank()) {
            try {
                if (!phoneUtil.isValidNumber(phoneUtil.parse(mData[holder.adapterPosition].smsNumber, "GB"))) {
                    holder.smsNumberTextInputLayout.error = "SMS Number is in the wrong format"
                } else {
                    holder.smsNumberTextInputLayout.error = ""
                }
            } catch (e: NumberParseException) {
                holder.smsNumberTextInputLayout.error = "SMS Number is in the wrong format"
            }
        }
        holder.smsNumberEditText.inputType = 3
        holder.smsNumberEditText.maxLines = 1

        holder.smsNumberEditText.setText(mData[holder.adapterPosition].smsNumber)
        holder.phraseEditText.setText(mData[holder.adapterPosition].phrase)

        holder.smsNumberEditText.addTextChangedListener(object : TextWatcher {
            var smsNumberEditing = false

            override fun afterTextChanged(s: Editable) {
                if (!smsNumberEditing) {
                    try {
                        val formattedNumber: Phonenumber.PhoneNumber = phoneUtil.parse(s.toString(), "GB")
                        if (!phoneUtil.isValidNumber(formattedNumber)) {
                            holder.smsNumberTextInputLayout.error = "SMS Number is in the wrong format"
                        } else {
                            smsNumberEditing = true
                            val prevSelection: Int = holder.smsNumberEditText.selectionStart
                            val prevLength: Int = holder.smsNumberEditText.length()
                            holder.smsNumberEditText.setText(phoneUtil.format(formattedNumber, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL))
                            holder.smsNumberEditText.setSelection(holder.smsNumberEditText.length() - prevLength + prevSelection)
                            holder.smsNumberTextInputLayout.error = ""
                        }
                    } catch (e: NumberParseException) {
                        holder.smsNumberTextInputLayout.error = "SMS Number is in the wrong format"
                    }
                    smsNumberEditing = false
                }
                mData[holder.adapterPosition].smsNumber = holder.smsNumberEditText.text.toString()
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
        })

        holder.phraseEditText.addTextChangedListener(object : TextWatcher {

            override fun afterTextChanged(s: Editable) {
                mData[holder.adapterPosition].phrase = holder.phraseEditText.text.toString()
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}


            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
        })

        holder.rulesRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rulesRadioBoth -> {
                    mData[holder.adapterPosition].choice = RulesChoice.ALL
                    holder.smsNumberTextInputLayout.visibility = View.VISIBLE
                    holder.phraseRulesRecyclerTextInputLayout.visibility = View.VISIBLE
                }
                R.id.rulesRadioTrigger -> {
                    mData[holder.adapterPosition].choice = RulesChoice.PHRASE
                    holder.smsNumberTextInputLayout.visibility = View.GONE
                    holder.phraseRulesRecyclerTextInputLayout.visibility = View.VISIBLE
                }
                R.id.rulesRadioSMSNumber -> {
                    mData[holder.adapterPosition].choice = RulesChoice.SMS_NUMBER
                    holder.smsNumberTextInputLayout.visibility = View.VISIBLE
                    holder.phraseRulesRecyclerTextInputLayout.visibility = View.GONE
                }
            }
        }

        holder.customiseAlarmRulesTextView.setOnClickListener {
            if (holder.customiseAlarmRulesConstraintLayout.visibility == View.GONE) {
                holder.customiseAlarmRulesConstraintLayout.visibility = View.VISIBLE
                holder.customiseAlarmRulesTextView.drawableEnd = getDrawable(mContext, R.drawable.ic_baseline_expand_less_24)
            } else {
                holder.customiseAlarmRulesConstraintLayout.visibility = View.GONE
                holder.customiseAlarmRulesTextView.drawableEnd = getDrawable(mContext, R.drawable.ic_baseline_expand_more_24)

            }

        }
    }

    private fun getDrawable(context: Context, id: Int): Drawable? {
        val version = Build.VERSION.SDK_INT
        return if (version >= 21) {
            ContextCompat.getDrawable(context, id)
        } else {
            ResourcesCompat.getDrawable(context.resources, id, null)
        }
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), ItemTouchViewHolder, View.OnClickListener {

        val rulesRadioGroup: RadioGroup = itemView.rulesRadioGroup
        val smsNumberEditText: TextInputEditText = itemView.SMSNumbersRulesEditText
        val smsNumberTextInputLayout: TextInputLayout = itemView.SMSNumbersRulesRecyclerTextInput
        val phraseEditText: TextInputEditText = itemView.phraseRulesEditText
        val phraseRulesRecyclerTextInputLayout: TextInputLayout = itemView.phraseRulesRecyclerTextInput

        val customiseAlarmRulesTextView: TextView = itemView.customiseAlarmRulesTextView
        val customiseAlarmRulesConstraintLayout: ConstraintLayout = itemView.customiseAlarmRulesConstraintLayout
        override fun onItemSelected() {
            val animator = ObjectAnimator.ofFloat(itemView.rules_recycler_cardview, "cardElevation", dipToPixels(2.0f), dipToPixels(10.0f))
            animator.interpolator = AccelerateInterpolator()
            animator.start()
        }

        override fun onItemClear() {
            Intrinsics.checkNotNullExpressionValue(itemView, "itemView")
            val animator = ObjectAnimator.ofFloat(itemView.rules_recycler_cardview, "cardElevation", dipToPixels(10.0f), dipToPixels(2.0f))
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