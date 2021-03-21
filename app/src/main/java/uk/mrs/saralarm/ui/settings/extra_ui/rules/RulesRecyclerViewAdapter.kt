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
import uk.mrs.saralarm.R
import uk.mrs.saralarm.databinding.*
import uk.mrs.saralarm.ui.settings.extra_ui.rules.colour.ColourDragAdapter
import uk.mrs.saralarm.ui.settings.extra_ui.rules.colour.ColourRecyclerViewAdapter
import uk.mrs.saralarm.ui.settings.extra_ui.rules.support.RulesChoice
import uk.mrs.saralarm.ui.settings.extra_ui.rules.support.RulesObject
import uk.mrs.saralarm.ui.settings.extra_ui.rules.support.SoundType
import uk.mrs.saralarm.ui.settings.extra_ui.support.DragListener
import uk.mrs.saralarm.ui.settings.extra_ui.support.ItemTouchViewHolder
import java.io.File


class RulesRecyclerViewAdapter(val context: Context, val rulesFragment: RulesFragment,
                               val data: ArrayList<RulesObject>,
                               private val binding: SettingsRulesFragmentBinding
) :
    RecyclerView.Adapter<RulesRecyclerViewAdapter.ViewHolder?>(), DragListener {

    var undoSnackBar: Snackbar? = null
    val phoneUtil: PhoneNumberUtil = PhoneNumberUtil.getInstance()

    override fun getItemCount(): Int {
        return data.size
    }

    override fun swapItems(fromPosition: Int, toPosition: Int) {
        val original = data[fromPosition]
        data.removeAt(fromPosition)
        data.add(toPosition, original)

        notifyItemMoved(fromPosition, toPosition)
    }

    override fun removeItems(adapterPosition: Int, allowUndo: Boolean) {
        if (adapterPosition >= 0 && adapterPosition < data.size) {
            if (allowUndo) {
                val temp = data[adapterPosition]

                undoSnackBar = Snackbar.make(binding.root, "Deleted", Snackbar.LENGTH_LONG).apply {
                    setAction("Undo") {
                        data.add(adapterPosition, temp)
                        notifyDataSetChanged()
                    }
                    duration = 9000
                }
                undoSnackBar?.anchorView = binding.rulesFab
                undoSnackBar?.show()
            }
            data.removeAt(adapterPosition)
            notifyItemRemoved(adapterPosition)
        }
    }

    fun addItem() {
        undoSnackBar?.dismiss()
        data.add(RulesObject())
        notifyDataSetChanged()
    }

    fun saveData() {
        val list = ArrayList<RulesObject>()

        val it: Iterator<RulesObject> = data.iterator()

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
            data.removeAll(list)
        }

        val sharedPrefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val editor: SharedPreferences.Editor = sharedPrefs.edit()
        if (data.isEmpty())
            editor.putString("rulesJSON", "")
        else
            editor.putString("rulesJSON", Gson().toJson(data))

        editor.apply()
        binding.root.post { notifyDataSetChanged() }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = SettingsRulesRecyclerViewRowBinding.inflate(LayoutInflater.from(context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when (data[holder.layoutPosition].choice) {
            RulesChoice.ALL -> {
                holder.rowBinding.rulesRadioGroup.check(R.id.rules_radio_both)
                holder.rowBinding.smsNumbersRulesRecyclerTextInput.visibility = View.VISIBLE
                holder.rowBinding.phraseRulesRecyclerTextInput.visibility = View.VISIBLE
            }
            RulesChoice.SMS_NUMBER -> {
                holder.rowBinding.rulesRadioGroup.check(R.id.rules_radio_sms_number)
                holder.rowBinding.smsNumbersRulesRecyclerTextInput.visibility = View.VISIBLE
                holder.rowBinding.phraseRulesRecyclerTextInput.visibility = View.GONE
            }
            RulesChoice.PHRASE -> {
                holder.rowBinding.rulesRadioGroup.check(R.id.rules_radio_phrase)
                holder.rowBinding.smsNumbersRulesRecyclerTextInput.visibility = View.GONE
                holder.rowBinding.phraseRulesRecyclerTextInput.visibility = View.VISIBLE
            }
        }

        if (data[holder.layoutPosition].smsNumber.isNotBlank()) {
            try {
                if (!phoneUtil.isValidNumber(phoneUtil.parse(data[holder.layoutPosition].smsNumber, "GB"))) {
                    holder.rowBinding.smsNumbersRulesRecyclerTextInput.error = holder.itemView.context.getString(R.string.fragment_settings_rules_sms_number_invalid)
                } else {
                    holder.rowBinding.smsNumbersRulesRecyclerTextInput.error = ""
                }
            } catch (e: NumberParseException) {
                holder.rowBinding.smsNumbersRulesRecyclerTextInput.error = holder.itemView.context.getString(R.string.fragment_settings_rules_sms_number_invalid)
            }
        }

        holder.rowBinding.smsNumbersRulesEditText.setText(data[holder.layoutPosition].smsNumber)
        holder.rowBinding.phraseRulesEditText.setText(data[holder.layoutPosition].phrase)

        holder.rowBinding.customiseAlarmLoopingCheckBox.isChecked = data[holder.layoutPosition].customAlarmRulesObject.isLooping

        when (data[holder.layoutPosition].customAlarmRulesObject.alarmSoundType) {
            SoundType.NONE -> {
                checkAndRemoveFile(holder.layoutPosition)
                holder.rowBinding.addAlarmRulesTextView.text = "No Alarm Sound Set. Using Default."
                holder.rowBinding.addAlarmRulesButton.text = "Set Alarm Sound"
            }
            SoundType.SYSTEM -> {
                if (data[holder.layoutPosition].customAlarmRulesObject.alarmFileName.isNotEmpty()) {
                    holder.rowBinding.addAlarmRulesTextView.text = data[holder.layoutPosition].customAlarmRulesObject.alarmFileName
                    holder.rowBinding.addAlarmRulesButton.text = "Reset Alarm Sound"
                }
            }
            SoundType.CUSTOM -> {
                if (data[holder.layoutPosition].customAlarmRulesObject.alarmFileName.isNotEmpty()) {
                    holder.rowBinding.addAlarmRulesTextView.text = data[holder.layoutPosition].customAlarmRulesObject.alarmFileName
                    holder.rowBinding.addAlarmRulesButton.text = "Reset Alarm Sound"
                }
            }
        }
        holder.rowBinding.customiseAlarmRulesConstraintLayout.visibility = View.GONE
        holder.rowBinding.customiseAlarmRulesTextView.drawableEnd = getDrawable(context, R.drawable.ic_baseline_expand_more_24)
    }

    private fun getDrawable(context: Context, id: Int): Drawable? {
        val version = Build.VERSION.SDK_INT
        return if (version >= 21) {
            ContextCompat.getDrawable(context, id)
        } else {
            ResourcesCompat.getDrawable(context.resources, id, null)
        }
    }

    inner class ViewHolder(val rowBinding: SettingsRulesRecyclerViewRowBinding) : RecyclerView.ViewHolder(rowBinding.root), ItemTouchViewHolder, View.OnClickListener {
        init {
            rowBinding.apply {
                smsNumbersRulesEditText.inputType = 3
                smsNumbersRulesEditText.maxLines = 1


                smsNumbersRulesEditText.addTextChangedListener(object : TextWatcher {
                    var smsNumberEditing = false

                    override fun afterTextChanged(s: Editable) {
                        if (s.isNotBlank())
                            if (!smsNumberEditing) {
                                try {
                                    val formattedNumber: Phonenumber.PhoneNumber = phoneUtil.parse(s.toString(), "GB")
                                    if (!phoneUtil.isValidNumber(formattedNumber)) {
                                        smsNumbersRulesRecyclerTextInput.error = context.getString(R.string.fragment_settings_rules_sms_number_invalid)
                                    } else {
                                        smsNumberEditing = true
                                        val prevSelection: Int = smsNumbersRulesEditText.selectionStart
                                        val prevLength: Int = smsNumbersRulesEditText.length()
                                        smsNumbersRulesEditText.setText(phoneUtil.format(formattedNumber, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL))
                                        smsNumbersRulesEditText.setSelection(
                                            if (smsNumbersRulesEditText.length() - prevLength + prevSelection > 0) {
                                                smsNumbersRulesEditText.length() - prevLength + prevSelection
                                            } else {
                                                0
                                            }
                                        )
                                        smsNumbersRulesRecyclerTextInput.error = ""

                                    }
                                } catch (e: NumberParseException) {
                                    smsNumbersRulesRecyclerTextInput.error = context.getString(R.string.fragment_settings_rules_sms_number_invalid)
                                }
                                smsNumberEditing = false
                            }
                        if (adapterPosition >= 0 && adapterPosition < data.size) {
                            data[adapterPosition].smsNumber = smsNumbersRulesEditText.text.toString()
                        }
                    }

                    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

                    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
                })

                phraseRulesEditText.addTextChangedListener(object : TextWatcher {

                    override fun afterTextChanged(s: Editable) {
                        if (adapterPosition >= 0 && adapterPosition < data.size) {
                            data[adapterPosition].phrase = phraseRulesEditText.text.toString()
                        }
                    }

                    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}


                    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
                })

                rulesRadioGroup.setOnCheckedChangeListener { _, checkedId ->
                    when (checkedId) {
                        R.id.rules_radio_both -> {
                            data[adapterPosition].choice = RulesChoice.ALL
                            smsNumbersRulesRecyclerTextInput.visibility = View.VISIBLE
                            phraseRulesRecyclerTextInput.visibility = View.VISIBLE
                        }
                        R.id.rules_radio_phrase -> {
                            data[adapterPosition].choice = RulesChoice.PHRASE
                            smsNumbersRulesRecyclerTextInput.visibility = View.GONE
                            phraseRulesRecyclerTextInput.visibility = View.VISIBLE
                        }
                        R.id.rules_radio_sms_number -> {
                            data[adapterPosition].choice = RulesChoice.SMS_NUMBER
                            smsNumbersRulesRecyclerTextInput.visibility = View.VISIBLE
                            phraseRulesRecyclerTextInput.visibility = View.GONE
                        }
                    }
                }

                customiseAlarmLoopingCheckBox.setOnCheckedChangeListener { _, isChecked ->
                    data[adapterPosition].customAlarmRulesObject.isLooping = isChecked
                }

                customiseAlarmRulesTextView.setOnClickListener {
                    TransitionManager.beginDelayedTransition(customiseAlarmRulesConstraintLayout, AutoTransition())
                    if (customiseAlarmRulesConstraintLayout.visibility == View.GONE) {
                        customiseAlarmRulesConstraintLayout.visibility = View.VISIBLE
                        customiseAlarmRulesTextView.drawableEnd = getDrawable(context, R.drawable.ic_baseline_expand_less_24)
                    } else {
                        customiseAlarmRulesConstraintLayout.visibility = View.GONE
                        customiseAlarmRulesTextView.drawableEnd = getDrawable(context, R.drawable.ic_baseline_expand_more_24)

                    }

                }

                addAlarmRulesButton.setOnClickListener {
                    if (ActivityCompat.checkSelfPermission(context, "android.permission.READ_EXTERNAL_STORAGE") == 0) {
                        if (addAlarmRulesButton.text == "Set Alarm Sound") {
                            when (data[adapterPosition].choice) {
                                RulesChoice.PHRASE ->
                                    if (data[adapterPosition].phrase == "") {
                                        Snackbar.make(this.root, ("Unable to add custom sound. Please configure the SMS number/Phrase first." as CharSequence), Snackbar.LENGTH_LONG).show()
                                        return@setOnClickListener
                                    }
                                RulesChoice.SMS_NUMBER ->
                                    if (data[adapterPosition].smsNumber == "") {
                                        Snackbar.make(this.root, ("Unable to add custom sound. Please configure the SMS number/Phrase first." as CharSequence), Snackbar.LENGTH_LONG).show()
                                        return@setOnClickListener
                                    }
                                RulesChoice.ALL -> {
                                    if (data[adapterPosition].smsNumber == "" && data[adapterPosition].phrase == "") {
                                        Snackbar.make(this.root, ("Unable to add custom sound. Please configure the SMS number/Phrase first." as CharSequence), Snackbar.LENGTH_LONG).show()
                                        return@setOnClickListener
                                    }
                                }
                            }

                            val dialog = Dialog(context)
                            val dialogBinding: SettingsRulesSoundDialogBinding = SettingsRulesSoundDialogBinding.inflate(LayoutInflater.from(context))
                            dialog.setContentView(dialogBinding.root)
                            val window: Window = dialog.window!!
                            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                            window.setGravity(Gravity.CENTER)
                            dialog.show()
                            dialogBinding.apply {
                                dialogSoundPickerCustomButton.setOnClickListener CustomSoundPickerButton@{
                                    dialog.cancel()

                                    rulesFragment.position = adapterPosition
                                    val intent = Intent()
                                    intent.action = Intent.ACTION_GET_CONTENT
                                    intent.type = "audio/*"
                                    rulesFragment.startActivityForResult(Intent.createChooser(intent, "Choose an Audio File"), 5)


                                }

                                dialogSoundPickerSystemButton.setOnClickListener {
                                    dialog.cancel()
                                    val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
                                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALL)
                                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Sound")
                                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, null as Uri?)
                                    rulesFragment.position = adapterPosition
                                    rulesFragment.startActivityForResult(intent, 6)
                                }
                            }
                        } else {
                            checkAndRemoveFile(adapterPosition)
                            notifyItemChanged(adapterPosition)
                        }
                    } else {
                        Snackbar.make(rowBinding.root, ("No read permission granted." as CharSequence), Snackbar.LENGTH_LONG).show()
                    }
                }


                addAlarmColoursRulesButton.setOnClickListener {
                    val dialog = Dialog(context)
                    val dialogBinding: ColourDialogFragmentBinding = ColourDialogFragmentBinding.inflate(LayoutInflater.from(context))
                    dialog.setContentView(dialogBinding.root)
                    val window: Window = dialog.window!!
                    window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    window.setGravity(Gravity.CENTER)
                    dialog.show()
                    dialogBinding.apply {
                        coloursRecyclerView.layoutManager = LinearLayoutManager(context)

                        val colourAdapter = ColourRecyclerViewAdapter(context, data[adapterPosition].customAlarmRulesObject.colorArray)

                        coloursRecyclerView.adapter = colourAdapter

                        ItemTouchHelper(ColourDragAdapter(colourAdapter, 3, 0)).attachToRecyclerView(coloursRecyclerView)

                        coloursRecyclerAddButton.setOnClickListener {
                            colourAdapter.addItem(true)
                        }
                        coloursRecyclerSaveButton.setOnClickListener {
                            colourAdapter.saveData()
                            dialog.dismiss()
                        }
                        colourAdapter.onSave = {
                            data[adapterPosition].customAlarmRulesObject.colorArray = it
                        }
                    }
                }
            }
        }

        override fun onItemSelected() {
            val animator = ObjectAnimator.ofFloat(rowBinding.rulesRecyclerCardView, "cardElevation", dipToPixels(2.0f), dipToPixels(10.0f))
            animator.interpolator = AccelerateInterpolator()
            animator.start()
        }

        override fun onItemClear() {
            val animator = ObjectAnimator.ofFloat(rowBinding.rulesRecyclerCardView, "cardElevation", dipToPixels(10.0f), dipToPixels(2.0f))
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

    fun checkAndRemoveFile(adapterPosition: Int) {
        if (data[adapterPosition].customAlarmRulesObject.alarmFileLocation != "") {
            var inUse = false
            for ((index, m) in data.withIndex()) {
                if (m.customAlarmRulesObject.alarmFileName == data[adapterPosition].customAlarmRulesObject.alarmFileName && index != adapterPosition) {
                    inUse = true
                }
            }
            if (!inUse) {
                val deleteFile = File(data[adapterPosition].customAlarmRulesObject.alarmFileLocation)
                if (deleteFile.isFile && deleteFile.exists()) {
                    deleteFile.delete()
                }
            }
            data[adapterPosition].customAlarmRulesObject.alarmSoundType = SoundType.NONE
            data[adapterPosition].customAlarmRulesObject.alarmFileLocation = ""
            data[adapterPosition].customAlarmRulesObject.alarmFileName = ""
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
