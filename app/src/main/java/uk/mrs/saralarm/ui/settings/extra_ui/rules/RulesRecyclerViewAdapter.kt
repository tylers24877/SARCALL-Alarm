package uk.mrs.saralarm.ui.settings.extra_ui.rules

import android.animation.ObjectAnimator
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.text.Editable
import android.text.TextWatcher
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.*
import android.view.animation.AccelerateInterpolator
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.Phonenumber
import kotlinx.android.synthetic.main.colour_dialog_fragment.*
import kotlinx.android.synthetic.main.settings_rules_recycler_view_row.view.*
import kotlinx.android.synthetic.main.settings_rules_sound_dialog.*
import uk.mrs.saralarm.R
import uk.mrs.saralarm.ui.settings.extra_ui.rules.colour.ColourDragAdapter
import uk.mrs.saralarm.ui.settings.extra_ui.rules.colour.ColourRecyclerViewAdapter
import uk.mrs.saralarm.ui.settings.extra_ui.rules.support.RulesChoice
import uk.mrs.saralarm.ui.settings.extra_ui.rules.support.RulesObject
import uk.mrs.saralarm.ui.settings.extra_ui.rules.support.SoundType
import uk.mrs.saralarm.ui.settings.extra_ui.support.ItemTouchViewHolder
import java.io.File


class RulesRecyclerViewAdapter(context: Context, val rulesFragment: RulesFragment, data: ArrayList<RulesObject>, private val root: View) :
    RecyclerView.Adapter<RulesRecyclerViewAdapter.ViewHolder?>() {
    var mContext: Context = context
    val mData: ArrayList<RulesObject> = data
    private val mInflater: LayoutInflater = LayoutInflater.from(context)
    val phoneUtil: PhoneNumberUtil = PhoneNumberUtil.getInstance()

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
            checkAndRemoveFile(adapterPosition)
            mData.removeAt(adapterPosition)
            notifyItemRemoved(adapterPosition)
        }
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
                    if (t.phrase == "") {
                        list.add(t)
                    }
                RulesChoice.SMS_NUMBER ->
                    if (t.smsNumber == "") {
                        list.add(t)
                    }
                RulesChoice.ALL -> {
                    if (t.smsNumber == "" && t.phrase == "") {
                        list.add(t)
                        break
                    }
                    if (t.smsNumber == "" && t.phrase != "") t.choice = RulesChoice.PHRASE else if (t.smsNumber != "" && t.phrase == "") t.choice = RulesChoice.SMS_NUMBER
                }
            }
        }
        if (list.isNotEmpty()) {
            mData.removeAll(list)
        }

        val sharedPrefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext)
        val editor: SharedPreferences.Editor = sharedPrefs.edit()
        if (mData.isEmpty())
            editor.putString("rulesJSON", "")
        else
            editor.putString("rulesJSON", Gson().toJson(mData))

        editor.apply()
        root.post { notifyDataSetChanged() }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view: View = mInflater.inflate(R.layout.settings_rules_recycler_view_row, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when (mData[holder.layoutPosition].choice) {
            RulesChoice.ALL -> {
                holder.itemView.rules_radio_group.check(R.id.rules_radio_both)
                holder.itemView.sms_numbers_rules_recycler_text_input.visibility = View.VISIBLE
                holder.itemView.phrase_rules_recycler_text_input.visibility = View.VISIBLE
            }
            RulesChoice.SMS_NUMBER -> {
                holder.itemView.rules_radio_group.check(R.id.rules_radio_sms_number)
                holder.itemView.sms_numbers_rules_recycler_text_input.visibility = View.VISIBLE
                holder.itemView.phrase_rules_recycler_text_input.visibility = View.GONE
            }
            RulesChoice.PHRASE -> {
                holder.itemView.rules_radio_group.check(R.id.rules_radio_phrase)
                holder.itemView.sms_numbers_rules_recycler_text_input.visibility = View.GONE
                holder.itemView.phrase_rules_recycler_text_input.visibility = View.VISIBLE
            }
        }

        if (mData[holder.layoutPosition].smsNumber.isNotBlank()) {
            try {
                if (!phoneUtil.isValidNumber(phoneUtil.parse(mData[holder.layoutPosition].smsNumber, "GB"))) {
                    holder.itemView.sms_numbers_rules_recycler_text_input.error = holder.itemView.context.getString(R.string.fragment_settings_rules_sms_number_invalid)
                } else {
                    holder.itemView.sms_numbers_rules_recycler_text_input.error = ""
                }
            } catch (e: NumberParseException) {
                holder.itemView.sms_numbers_rules_recycler_text_input.error = holder.itemView.context.getString(R.string.fragment_settings_rules_sms_number_invalid)
            }
        }

        holder.itemView.sms_numbers_rules_edit_text.setText(mData[holder.layoutPosition].smsNumber)
        holder.itemView.phrase_rules_edit_text.setText(mData[holder.layoutPosition].phrase)

        holder.itemView.customise_alarm_looping_check_box.isChecked = mData[holder.layoutPosition].customAlarmRulesObject.isLooping

        when (mData[holder.layoutPosition].customAlarmRulesObject.alarmSoundType) {
            SoundType.NONE -> {
                checkAndRemoveFile(holder.layoutPosition)
                holder.itemView.add_alarm_rules_text_view.text = "No Alarm Sound Set. Using Default."
                holder.itemView.add_alarm_rules_button.text = "Set Alarm Sound"
            }
            SoundType.SYSTEM -> {
                if (mData[holder.layoutPosition].customAlarmRulesObject.alarmFileName.isNotEmpty()) {
                    holder.itemView.add_alarm_rules_text_view.text = mData[holder.layoutPosition].customAlarmRulesObject.alarmFileName
                    holder.itemView.add_alarm_rules_button.text = "Reset Alarm Sound"
                }
            }
            SoundType.CUSTOM -> {
                if (mData[holder.layoutPosition].customAlarmRulesObject.alarmFileName.isNotEmpty()) {
                    holder.itemView.add_alarm_rules_text_view.text = mData[holder.layoutPosition].customAlarmRulesObject.alarmFileName
                    holder.itemView.add_alarm_rules_button.text = "Reset Alarm Sound"
                }
            }
        }
        holder.itemView.customise_alarm_rules_constraint_layout.visibility = View.GONE
        holder.itemView.customise_alarm_rules_text_view.drawableEnd = getDrawable(mContext, R.drawable.ic_baseline_expand_more_24)
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
        init {
            itemView.sms_numbers_rules_edit_text.inputType = 3
            itemView.sms_numbers_rules_edit_text.maxLines = 1


            itemView.sms_numbers_rules_edit_text.addTextChangedListener(object : TextWatcher {
                var smsNumberEditing = false

                override fun afterTextChanged(s: Editable) {
                    if (s.isNotBlank())
                        if (!smsNumberEditing) {
                            try {
                                val formattedNumber: Phonenumber.PhoneNumber = phoneUtil.parse(s.toString(), "GB")
                                if (!phoneUtil.isValidNumber(formattedNumber)) {
                                    itemView.sms_numbers_rules_recycler_text_input.error = itemView.context.getString(R.string.fragment_settings_rules_sms_number_invalid)
                                } else {
                                    smsNumberEditing = true
                                    val prevSelection: Int = itemView.sms_numbers_rules_edit_text.selectionStart
                                    val prevLength: Int = itemView.sms_numbers_rules_edit_text.length()
                                    itemView.sms_numbers_rules_edit_text.setText(phoneUtil.format(formattedNumber, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL))
                                    itemView.sms_numbers_rules_edit_text.setSelection(
                                        if (itemView.sms_numbers_rules_edit_text.length() - prevLength + prevSelection > 0) {
                                            itemView.sms_numbers_rules_edit_text.length() - prevLength + prevSelection
                                        } else {
                                            0
                                        }
                                    )
                                    itemView.sms_numbers_rules_recycler_text_input.error = ""

                                }
                            } catch (e: NumberParseException) {
                                itemView.sms_numbers_rules_recycler_text_input.error = itemView.context.getString(R.string.fragment_settings_rules_sms_number_invalid)
                            }
                            smsNumberEditing = false
                        }
                    if (adapterPosition >= 0 && adapterPosition < mData.size) {
                        mData[adapterPosition].smsNumber = itemView.sms_numbers_rules_edit_text.text.toString()
                    }
                }

                override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            })

            itemView.phrase_rules_edit_text.addTextChangedListener(object : TextWatcher {

                override fun afterTextChanged(s: Editable) {
                    if (adapterPosition >= 0 && adapterPosition < mData.size) {
                        mData[adapterPosition].phrase = itemView.phrase_rules_edit_text.text.toString()
                    }
                }

                override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}


                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            })

            itemView.rules_radio_group.setOnCheckedChangeListener { _, checkedId ->
                when (checkedId) {
                    R.id.rules_radio_both -> {
                        mData[adapterPosition].choice = RulesChoice.ALL
                        itemView.sms_numbers_rules_recycler_text_input.visibility = View.VISIBLE
                        itemView.phrase_rules_recycler_text_input.visibility = View.VISIBLE
                    }
                    R.id.rules_radio_phrase -> {
                        mData[adapterPosition].choice = RulesChoice.PHRASE
                        itemView.sms_numbers_rules_recycler_text_input.visibility = View.GONE
                        itemView.phrase_rules_recycler_text_input.visibility = View.VISIBLE
                    }
                    R.id.rules_radio_sms_number -> {
                        mData[adapterPosition].choice = RulesChoice.SMS_NUMBER
                        itemView.sms_numbers_rules_recycler_text_input.visibility = View.VISIBLE
                        itemView.phrase_rules_recycler_text_input.visibility = View.GONE
                    }
                }
            }

            itemView.customise_alarm_looping_check_box.setOnCheckedChangeListener { _, isChecked ->
                mData[adapterPosition].customAlarmRulesObject.isLooping = isChecked
            }

            itemView.customise_alarm_rules_text_view.setOnClickListener {
                TransitionManager.beginDelayedTransition(itemView.customise_alarm_rules_constraint_layout, AutoTransition())
                if (itemView.customise_alarm_rules_constraint_layout.visibility == View.GONE) {
                    itemView.customise_alarm_rules_constraint_layout.visibility = View.VISIBLE
                    itemView.customise_alarm_rules_text_view.drawableEnd = getDrawable(mContext, R.drawable.ic_baseline_expand_less_24)
                } else {
                    itemView.customise_alarm_rules_constraint_layout.visibility = View.GONE
                    itemView.customise_alarm_rules_text_view.drawableEnd = getDrawable(mContext, R.drawable.ic_baseline_expand_more_24)

                }

            }

            itemView.add_alarm_rules_button.setOnClickListener {
                if (ActivityCompat.checkSelfPermission(mContext, "android.permission.READ_EXTERNAL_STORAGE") == 0) {
                    if (itemView.add_alarm_rules_button.text == "Set Alarm Sound") {
                        when (mData[adapterPosition].choice) {
                            RulesChoice.PHRASE ->
                                if (mData[adapterPosition].phrase == "") {
                                    Snackbar.make(itemView, ("Unable to add custom sound. Please configure the SMS number/Phrase first." as CharSequence), Snackbar.LENGTH_LONG).show()
                                    return@setOnClickListener
                                }
                            RulesChoice.SMS_NUMBER ->
                                if (mData[adapterPosition].smsNumber == "") {
                                    Snackbar.make(itemView, ("Unable to add custom sound. Please configure the SMS number/Phrase first." as CharSequence), Snackbar.LENGTH_LONG).show()
                                    return@setOnClickListener
                                }
                            RulesChoice.ALL -> {
                                if (mData[adapterPosition].smsNumber == "" && mData[adapterPosition].phrase == "") {
                                    Snackbar.make(itemView, ("Unable to add custom sound. Please configure the SMS number/Phrase first." as CharSequence), Snackbar.LENGTH_LONG).show()
                                    return@setOnClickListener
                                }
                            }
                        }

                        val dialog = Dialog(mContext)
                        dialog.setContentView(R.layout.settings_rules_sound_dialog)
                        val window: Window = dialog.window!!
                        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                        window.setGravity(Gravity.CENTER)
                        dialog.show()

                        dialog.dialog_sound_picker_custom_button.setOnClickListener CustomSoundPickerButton@{
                            dialog.cancel()

                            rulesFragment.position = adapterPosition
                            val intent = Intent()
                            intent.action = Intent.ACTION_GET_CONTENT
                            intent.type = "audio/*"
                            rulesFragment.startActivityForResult(Intent.createChooser(intent, "Choose an Audio File"), 5)


                        }

                        dialog.dialog_sound_picker_system_button.setOnClickListener {
                            dialog.cancel()
                            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
                            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALL)
                            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Sound")
                            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, null as Uri?)
                            rulesFragment.position = adapterPosition
                            rulesFragment.startActivityForResult(intent, 6)
                        }
                    } else {
                        checkAndRemoveFile(adapterPosition)
                        notifyItemChanged(adapterPosition)
                    }
                } else {
                    Snackbar.make(itemView, ("No read permission granted." as CharSequence), Snackbar.LENGTH_LONG).show()
                }
            }


            itemView.add_alarm_colours_rules_button.setOnClickListener {
                val dialog = Dialog(mContext)
                dialog.setContentView(R.layout.colour_dialog_fragment)
                val window: Window = dialog.window!!
                window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                window.setGravity(Gravity.CENTER)
                dialog.show()

                dialog.colours_recycler_view.layoutManager = LinearLayoutManager(mContext)

                val colourAdapter = ColourRecyclerViewAdapter(mContext, mData[adapterPosition].customAlarmRulesObject.colorArray)

                dialog.colours_recycler_view.adapter = colourAdapter

                ItemTouchHelper(ColourDragAdapter(colourAdapter, 3, 0)).attachToRecyclerView(dialog.colours_recycler_view)

                dialog.colours_recycler_add_button.setOnClickListener {
                    colourAdapter.addItem(true)
                }
                dialog.colours_recycler_save_button.setOnClickListener {
                    colourAdapter.saveData()
                    dialog.dismiss()
                }
                colourAdapter.onSave = {
                    mData[adapterPosition].customAlarmRulesObject.colorArray = it
                }
            }
        }

        override fun onItemSelected() {
            val animator = ObjectAnimator.ofFloat(itemView.rules_recycler_card_view, "cardElevation", dipToPixels(2.0f), dipToPixels(10.0f))
            animator.interpolator = AccelerateInterpolator()
            animator.start()
        }

        override fun onItemClear() {
            val animator = ObjectAnimator.ofFloat(itemView.rules_recycler_card_view, "cardElevation", dipToPixels(10.0f), dipToPixels(2.0f))
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

    fun checkAndRemoveFile(adapterPosition: Int) {
        if (mData[adapterPosition].customAlarmRulesObject.alarmFileLocation != "") {
            var inUse = false
            for ((index, m) in mData.withIndex()) {
                if (m.customAlarmRulesObject.alarmFileName == mData[adapterPosition].customAlarmRulesObject.alarmFileName && index != adapterPosition) {
                    inUse = true
                }
            }
            if (!inUse) {
                val deleteFile = File(mData[adapterPosition].customAlarmRulesObject.alarmFileLocation)
                if (deleteFile.isFile && deleteFile.exists()) {
                    deleteFile.delete()
                }
            }
            mData[adapterPosition].customAlarmRulesObject.alarmSoundType = SoundType.NONE
            mData[adapterPosition].customAlarmRulesObject.alarmFileLocation = ""
            mData[adapterPosition].customAlarmRulesObject.alarmFileName = ""
            saveData()
        }
    }
}


var TextView.drawableEnd: Drawable?
    get() = compoundDrawablesRelative[2]
    set(value) = setDrawables(end = value)

fun TextView.setDrawables(
    start: Drawable? = null,
    top: Drawable? = null,
    end: Drawable? = null,
    bottom: Drawable? = null
) = setCompoundDrawablesRelativeWithIntrinsicBounds(start, top, end, bottom)