package uk.mrs.saralarm.ui.respond

import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.DialogInterface.BUTTON_POSITIVE
import android.content.SharedPreferences
import android.graphics.Rect
import android.os.Bundle
import android.telephony.SmsManager
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.view.*
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat
import kotlinx.android.synthetic.main.dialog_respond_sar_a.*
import kotlinx.android.synthetic.main.dialog_respond_sar_l.*
import kotlinx.android.synthetic.main.dialog_sign_off.*
import kotlinx.android.synthetic.main.fragment_respond.*
import kotlinx.android.synthetic.main.fragment_respond.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import uk.mrs.saralarm.R
import uk.mrs.saralarm.support.SARResponseCode
import java.lang.reflect.Type
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList
import kotlin.math.abs


class RespondFragment : Fragment() {
    private var respondViewModel: RespondViewModel? =null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        respondViewModel = ViewModelProvider(this).get(RespondViewModel::class.java)
        val root: View = inflater.inflate(R.layout.fragment_respond, container, false)
        if (!(ActivityCompat.checkSelfPermission(requireContext(), "android.permission.RECEIVE_SMS") == 0
                    && ActivityCompat.checkSelfPermission(requireContext(), "android.permission.WRITE_EXTERNAL_STORAGE") == 0
                    && ActivityCompat.checkSelfPermission(requireContext(), "android.permission.READ_EXTERNAL_STORAGE")== 0
                    && ActivityCompat.checkSelfPermission(requireContext(), "android.permission.SEND_SMS")== 0
                    && ActivityCompat.checkSelfPermission(requireContext(), "android.permission.READ_SMS") == 0))
        {
            requestPermissions(
                arrayOf(
                    "android.permission.RECEIVE_SMS",
                    "android.permission.WRITE_EXTERNAL_STORAGE",
                    "android.permission.READ_EXTERNAL_STORAGE",
                    "android.permission.SEND_SMS",
                    "android.permission.READ_SMS"
                ), 0
            )
        }
        root.respond_sar_a_button.setOnClickListener{
            dialogSARAOpen(context)
        }
        root.respond_sar_l_button.setOnClickListener {
            dialogSARLOpen(context)
        }
        root.respond_sar_n_button.setOnClickListener {
            dialogSARNOpen()
        }
        root.pullToRefresh.setOnRefreshListener {
            updateLatestSMS()
            root.pullToRefresh.isRefreshing = false
        }
        root.respond_sign_on.setOnClickListener {
            displaySignOnDialog()
        }
        root.respond_sign_off.setOnClickListener {
            displaySignOffDialog()
        }
        return root
    }

    override fun onResume() {
       updateLatestSMS()
        val pref: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())

        if (!pref.getBoolean("prefEnabled", false)) {
            requireView().InfoView.visibility = View.VISIBLE
            requireView().InfoView_txtview.text = "SARCALL Alarm not enabled. Tap to go to settings."
           requireView().InfoView.setOnClickListener{
               findNavController().navigate(R.id.action_navigation_respond_to_navigation_settings)
           }
        } else {
            val phoneNumberJSON: String? = pref.getString("rulesJSON", "")
            if (phoneNumberJSON.isNullOrBlank()) {
                requireView().InfoView.visibility = View.VISIBLE
                requireView().InfoView_txtview.text = "Rules are not configured correctly! Please click to check."
                requireView().InfoView.setOnClickListener{
                    findNavController().navigate(R.id.action_navigation_respond_to_navigation_settings)
                }
            } else {

                if (pref.getString("respondSMSNumbersJSON", "").isNullOrEmpty()) {
                    requireView().InfoView.visibility = View.VISIBLE
                    requireView().InfoView_txtview.text = "No SAR respond number configured. Please click to setup."
                    requireView().InfoView.setOnClickListener {
                        findNavController().navigate(R.id.action_navigation_respond_to_SMSNumbersFragment)
                    }
                } else {
                    requireView().InfoView.visibility = View.GONE
                }
            }
        }
        super.onResume()
    }

    private fun dialogSARAOpen(context: Context?) {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_respond_sar_a)
        val window: Window = dialog.window!!
        window.setLayout(MATCH_PARENT, WRAP_CONTENT)
        window.setGravity(Gravity.CENTER)
        dialog.show()
        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        var customMessageArray = ArrayList<String>()
        val json = pref.getString("customMessageJSON", "")
        if (json!!.isNotEmpty()) {
            val type: Type = object : TypeToken<ArrayList<String>?>() {}.type
            val fromJson: ArrayList<String> = Gson().fromJson(json, type)
            customMessageArray = fromJson
        }
        customMessageArray.removeAll(Collections.singleton(""))
        customMessageArray.add(0, "Enter Custom Message...")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_expandable_list_item_1, customMessageArray)
        adapter.setDropDownViewResource(android.R.layout.simple_expandable_list_item_1)

        dialog.sar_a_spinner.adapter = adapter


        dialog.sar_a_spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (dialog.sar_a_spinner.selectedItem.toString() == "Enter Custom Message...") {
                    dialog.respond_dialog_sar_a_message_editview.isEnabled = true
                    dialog.respond_dialog_sar_a_message_inputlayout.visibility = View.VISIBLE
                    dialog.respond_dialog_sar_a_message_title_txtview.visibility = View.VISIBLE
                    return
                }
                dialog.respond_dialog_sar_a_message_editview.isEnabled = false
                dialog.respond_dialog_sar_a_message_inputlayout.visibility = View.GONE
                dialog.respond_dialog_sar_a_message_title_txtview.visibility = View.GONE
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                dialog.respond_dialog_sar_a_message_editview.isEnabled = false
            }
        }
        dialog.respond_dialog_sar_a_constraint_layout.setOnTouchListener{ v, event ->
            when (event?.action) {
                MotionEvent.ACTION_DOWN ->
                    if (dialog.respond_dialog_sar_a_message_editview.isFocused) {
                        val outRect = Rect()
                        dialog.respond_dialog_sar_a_message_editview.getGlobalVisibleRect(outRect)
                        if (!outRect.contains(event.rawX.toInt(), event.rawY.toInt())) {
                            dialog.respond_dialog_sar_a_message_editview.clearFocus()
                            val systemService = v.context.getSystemService(Context.INPUT_METHOD_SERVICE)
                            if (systemService != null) {
                                (systemService as InputMethodManager).hideSoftInputFromWindow(v.windowToken, 0)
                            } else {
                                throw NullPointerException("null cannot be cast to non-null type android.view.inputmethod.InputMethodManager")
                            }
                        }
                    }
            }
            requireView().performClick()
            v?.onTouchEvent(event) ?: true
        }
        dialog.respond_dialog_sar_a_seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                if (dialog.respond_dialog_sar_a_message_editview.isFocused) {
                    dialog.respond_dialog_sar_a_message_editview.clearFocus()
                }
                val systemService = dialog.context.getSystemService(Context.INPUT_METHOD_SERVICE)
                val imm = systemService as InputMethodManager
                imm.hideSoftInputFromWindow(requireView().windowToken, 0)
                return
            }

            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (progress >= 0 && progress <= seekBar.max) {
                    val progressCal = progress * 5
                    val sb = SpannableStringBuilder()
                    sb.append("Estimated time to arrival: ")
                    sb.append(progressCal.toString())
                    sb.append(" minutes")
                    sb.setSpan(StyleSpan(1), 27, progressCal.toString().length + 27, 18)
                    dialog.respond_dialog_sar_a_seek_eta_txtview.text = sb
                }
            }
        })
        dialog.respond_dialog_sar_a_seek.progress = 5
        dialog.respond_dialog_sar_a_submit_button.setOnClickListener {
            val progressCal = dialog.respond_dialog_sar_a_seek.progress * 5
            if(dialog.sar_a_spinner.selectedItem=="Enter Custom Message...") {
                sendSMSResponse(requireContext(), SARResponseCode.SAR_A, dialog, progressCal, dialog.respond_dialog_sar_a_message_editview.text.toString())
            }else{
                sendSMSResponse(requireContext(), SARResponseCode.SAR_A, dialog, progressCal, dialog.sar_a_spinner.selectedItem.toString())
            }
        }
    }

    private fun dialogSARLOpen(context: Context?) {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_respond_sar_l)
        val window: Window = dialog.window!!
        window.setLayout(MATCH_PARENT, WRAP_CONTENT)
        window.setGravity(Gravity.CENTER)
        dialog.show()
        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        var customMessageArray = ArrayList<String>()
        val json = pref.getString("customMessageJSON", "")
        if (json!!.isNotEmpty()) {
            val type: Type = object : TypeToken<ArrayList<String>?>() {}.type
            val fromJson: ArrayList<String> = Gson().fromJson(json, type)
            customMessageArray = fromJson
        }
        customMessageArray.removeAll(Collections.singleton(""))
        customMessageArray.add(0, "Enter Custom Message...")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_expandable_list_item_1, customMessageArray)
        adapter.setDropDownViewResource(android.R.layout.simple_expandable_list_item_1)


        dialog.sar_l_spinner.adapter = adapter

        dialog.sar_l_spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (dialog.sar_l_spinner.selectedItem.toString() == "Enter Custom Message...") {
                    dialog.respond_dialog_sar_l_message_editview.isEnabled = true
                    dialog.respond_dialog_sar_l_message_inputlayout.visibility = View.VISIBLE
                    dialog.respond_dialog_sar_l_message_title_txtview.visibility = View.VISIBLE
                    return
                }
                dialog.respond_dialog_sar_l_message_editview.isEnabled = false
                dialog.respond_dialog_sar_l_message_inputlayout.visibility = View.GONE
                dialog.respond_dialog_sar_l_message_title_txtview.visibility = View.GONE
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                dialog.respond_dialog_sar_l_message_editview.isEnabled = false
            }
        }
        dialog.respond_dialog_sar_l_constraint_layout.setOnTouchListener { v, event ->
            when (event?.action) {
                MotionEvent.ACTION_DOWN ->
                    if (dialog.respond_dialog_sar_l_message_editview.isFocused) {
                        val outRect = Rect()
                        dialog.respond_dialog_sar_l_message_editview.getGlobalVisibleRect(outRect)
                        if (!outRect.contains(event.rawX.toInt(), event.rawY.toInt())) {
                            dialog.respond_dialog_sar_l_message_editview.clearFocus()
                            val systemService = v.context.getSystemService(Context.INPUT_METHOD_SERVICE)
                            if (systemService != null) {
                                (systemService as InputMethodManager).hideSoftInputFromWindow(v.windowToken, 0)
                            } else {
                                throw NullPointerException("null cannot be cast to non-null type android.view.inputmethod.InputMethodManager")
                            }
                        }
                    }
            }
            requireView().performClick()
            v?.onTouchEvent(event) ?: true
        }

        dialog.respond_dialog_sar_l_seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                if (dialog.respond_dialog_sar_l_message_editview.isFocused) {
                    dialog.respond_dialog_sar_l_message_editview.clearFocus()
                }
                val systemService = dialog.context.getSystemService(Context.INPUT_METHOD_SERVICE)
                val imm = systemService as InputMethodManager
                imm.hideSoftInputFromWindow(requireView().windowToken, 0)
                return
            }

            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (progress >= 0 && progress <= seekBar.max) {
                    val progressCal = progress * 5
                    val sb = SpannableStringBuilder()
                    sb.append("Estimated time to arrival: ")
                    sb.append(progressCal.toString())
                    sb.append(" minutes")
                    sb.setSpan(StyleSpan(1), 27, progressCal.toString().length + 27, 18)
                    if (progressCal != 0) {
                        dialog.respond_dialog_sar_l_seek_eta_txtview.text = sb
                    } else {
                        dialog.respond_dialog_sar_l_seek_eta_txtview.text = "Estimated time to arrival: N/A"
                    }
                }
            }
        })
        dialog.respond_dialog_sar_l_seek.progress = 5
        dialog.respond_dialog_sar_l_submit_button.setOnClickListener {
            val progressCal = dialog.respond_dialog_sar_l_seek.progress * 5
            if(dialog.sar_l_spinner.selectedItem=="Enter Custom Message...") {
                sendSMSResponse(requireContext(), SARResponseCode.SAR_L, dialog, progressCal, dialog.respond_dialog_sar_l_message_editview.text.toString())
            }else{
                sendSMSResponse(requireContext(), SARResponseCode.SAR_L, dialog, progressCal, dialog.sar_l_spinner.selectedItem.toString())
            }
        }

    }

    private fun dialogSARNOpen() {
        val dialogClickListener  =  DialogInterface.OnClickListener{ dialog, which ->
            if (which == BUTTON_POSITIVE) {
                sendSMSResponse(requireContext(), SARResponseCode.SAR_N, dialog, 0, null)
            }
        }
        AlertDialog.Builder(requireContext()).setMessage(R.string.SAR_N_dialog_title).setPositiveButton(R.string.sar_n_positive, dialogClickListener)
            .setNegativeButton(R.string.sar_n_negitive, dialogClickListener).show()
    }


    private fun sendSMSResponse(context: Context, sarResponseCode: SARResponseCode, dialog: DialogInterface?, value: Int, message: String?) {
        val sb: StringBuilder
        val smsManager: SmsManager = SmsManager.getDefault()
        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        val phoneUtil = PhoneNumberUtil.getInstance()
        try {
            val type: Type = object : TypeToken<java.util.ArrayList<String>?>() {}.type
            val phoneNumberSet: java.util.ArrayList<String>? = Gson().fromJson(pref.getString("respondSMSNumbersJSON", ""), type)
            if (phoneNumberSet.isNullOrEmpty()) {
                Snackbar.make(respond_constraintLayout, "Failed! No SARCALL number chosen. Please check/add number in settings.", Snackbar.LENGTH_LONG).show()
                return
            }
            when (sarResponseCode) {
                SARResponseCode.SAR_A -> {
                    if (value != 0) {
                        sb = StringBuilder()
                        sb.append("SAR A")
                        sb.append(value)
                        sb.append(' ')
                        sb.append(message)
                        for (phoneNumber in phoneNumberSet) {
                            smsManager.sendTextMessage(phoneUtil.format(phoneUtil.parse(phoneNumber, "GB"), PhoneNumberFormat.INTERNATIONAL), null, sb.toString(), null, null)
                        }
                    } else {
                        sb = StringBuilder()
                        sb.append("SAR A  ")
                        sb.append(message)
                        for (phoneNumber in phoneNumberSet) {
                            smsManager.sendTextMessage(phoneUtil.format(phoneUtil.parse(phoneNumber, "GB"), PhoneNumberFormat.INTERNATIONAL), null, sb.toString(), null, null)
                        }
                    }
                    Toast.makeText(context, "SAR A sent to SARCALL", Toast.LENGTH_LONG).show()
                    dialog?.cancel()
                }
                SARResponseCode.SAR_L -> {
                    if (value != 0) {
                        sb = StringBuilder()
                        sb.append("SAR L")
                        sb.append(value)
                        sb.append(' ')
                        sb.append(message)
                        for (phoneNumber in phoneNumberSet) {
                            smsManager.sendTextMessage(phoneUtil.format(phoneUtil.parse(phoneNumber, "GB"), PhoneNumberFormat.INTERNATIONAL), null, sb.toString(), null, null)
                        }

                    } else {
                        sb = StringBuilder()
                        sb.append("SAR L ")
                        sb.append(message)
                        for (phoneNumber in phoneNumberSet) {
                            smsManager.sendTextMessage(phoneUtil.format(phoneUtil.parse(phoneNumber, "GB"), PhoneNumberFormat.INTERNATIONAL), null, sb.toString(), null, null)
                        }
                    }

                    Toast.makeText(context, "SAR L sent to SARCALL", Toast.LENGTH_LONG).show()
                    dialog?.cancel()
                }
                SARResponseCode.SAR_N -> {
                    for (phoneNumber in phoneNumberSet) {
                        smsManager.sendTextMessage(phoneUtil.format(phoneUtil.parse(phoneNumber, "GB"), PhoneNumberFormat.INTERNATIONAL), null, "SAR N", null, null)
                    }
                    Toast.makeText(context, "SAR N sent to SARCALL", Toast.LENGTH_LONG).show()
                    dialog?.cancel()
                }
                SARResponseCode.SIGN_ON -> {
                    for (phoneNumber in phoneNumberSet) {
                        smsManager.sendTextMessage(phoneUtil.format(phoneUtil.parse(phoneNumber, "GB"), PhoneNumberFormat.INTERNATIONAL), null, "ON $message", null, null)
                    }
                    Toast.makeText(context, "Sign on SMS sent to SARCALL. Lookout for a confirmation SMS.", Toast.LENGTH_LONG).show()
                    dialog?.cancel()
                }
                SARResponseCode.SIGN_OFF -> {
                    for (phoneNumber in phoneNumberSet) {
                        if (value != 0) {
                            smsManager.sendTextMessage(
                                phoneUtil.format(phoneUtil.parse(phoneNumber, "GB"), PhoneNumberFormat.INTERNATIONAL), null, "OFF $message $value" + "d",
                                null, null
                            )
                        } else {
                            smsManager.sendTextMessage(
                                phoneUtil.format(phoneUtil.parse(phoneNumber, "GB"), PhoneNumberFormat.INTERNATIONAL), null, "OFF $message",
                                null, null
                            )
                        }
                    }
                    Toast.makeText(context, "Sign off SMS sent to SARCALL. Lookout for a confirmation SMS.", Toast.LENGTH_LONG).show()
                    dialog?.cancel()
                }
            }
        } catch (e7: NumberParseException) {
            Toast.makeText(context, "Failed! SARCALL SMS number is formatted wrong. Please check number in settings.", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Unknown error. Please try again or report issue.", Toast.LENGTH_LONG).show()
            FirebaseCrashlytics.getInstance().recordException(e)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 0) {
            val length = permissions.size
            for (i in 0 until length) {
                if (permissions[i].equals("android.permission.READ_SMS") && grantResults[i] == 0) {
                    updateLatestSMS()
                }
            }
        }
    }

    private fun updateLatestSMS() {
        if (ActivityCompat.checkSelfPermission(requireContext(), "android.permission.READ_SMS") == 0) {
            GlobalScope.launch(Dispatchers.Main) {
                try {
                    requireView().respond_sms_loading_bar.visibility = View.VISIBLE
                    requireView().respond_sms_preview_txtview.visibility = View.GONE
                    requireView().respond_preview_date_txtview.visibility = View.GONE

                    val (resultBody, resultDate) = respondViewModel!!.setPreviewAsync(requireContext()).await()
                    requireView().respond_sms_preview_txtview.text = resultBody
                    requireView().respond_preview_date_txtview.text = "Received: $resultDate"

                    requireView().respond_sms_loading_bar.visibility = View.GONE
                    requireView().respond_sms_preview_txtview.visibility = View.VISIBLE
                    requireView().respond_preview_date_txtview.visibility = View.VISIBLE
                } catch (e: IllegalStateException) {
                    FirebaseCrashlytics.getInstance().recordException(e)
                }
            }
        } else {
            try {
                requireView().respond_preview_date_txtview.text = ""
                requireView().respond_sms_preview_txtview.setText(R.string.response_permission_placeholder)
            } catch (e: IllegalStateException) {
                FirebaseCrashlytics.getInstance().recordException(e)
            }
        }
    }

    private fun displaySignOnDialog() {
        val dialogClickListener = DialogInterface.OnClickListener { _, which ->
            if (which == BUTTON_POSITIVE) {
                val pref: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
                val teamPrefix = pref.getString("prefTeamPrefix", "")
                if (teamPrefix.isNullOrBlank()) {
                    Snackbar.make(respond_constraintLayout, "Cannot sign on. No team prefix set in settings.", Snackbar.LENGTH_LONG).show()
                    return@OnClickListener
                }
                sendSMSResponse(requireContext(), SARResponseCode.SIGN_ON, null, 0, teamPrefix)
            }
        }
        AlertDialog.Builder(requireContext()).setTitle("Sign On").setMessage("Are you sure?").setPositiveButton("Yes", dialogClickListener)
            .setNegativeButton("No", dialogClickListener).show()
    }

    private fun displaySignOffDialog() {
        var daysBetween = 0

        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_sign_off)
        val window: Window = dialog.window!!
        window.setLayout(MATCH_PARENT, WRAP_CONTENT)
        window.setGravity(Gravity.CENTER)
        dialog.show()

        val pref: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())

        dialog.respond_dialog_sign_off_cancel_button.setOnClickListener {
            dialog.cancel()
        }

        dialog.respond_dialog_sign_off_set_date_button.setOnClickListener {
            val c = Calendar.getInstance()
            val datePickerDialog = DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    val cal = Calendar.getInstance()
                    cal[Calendar.MONTH] = month
                    cal[Calendar.DAY_OF_MONTH] = dayOfMonth
                    cal[Calendar.YEAR] = year
                    if (cal.before(c)) {
                        Toast.makeText(requireContext(), "Please choose a day past present day.", Toast.LENGTH_LONG).show()
                        return@DatePickerDialog
                    }

                    daysBetween = safeLongToInt(TimeUnit.MILLISECONDS.toDays(abs(cal.timeInMillis - c.timeInMillis)))

                    if (pref.getBoolean("prefDateWorkaround", true)) {
                        if (daysBetween >= 3) daysBetween++
                    }

                    val sb = SpannableStringBuilder()
                    sb.append("Duration: ")
                    if (daysBetween == 0) {
                        sb.append("∞")
                    } else {
                        sb.append("$daysBetween day" + if (daysBetween != 1) "s" else "")
                    }
                    sb.setSpan(StyleSpan(1), 10, daysBetween.toString().length + 10, SpannableStringBuilder.SPAN_INCLUSIVE_INCLUSIVE)
                    dialog.respond_dialog_sign_off_seek_dur_txtview.text = sb
                },
                c[Calendar.YEAR], c[Calendar.MONTH], c[Calendar.DAY_OF_MONTH]
            )
            datePickerDialog.datePicker.minDate = c.timeInMillis
            datePickerDialog.show()
        }


        dialog.respond_dialog_sign_off_submit_button.setOnClickListener {
            val teamPrefix = pref.getString("prefTeamPrefix", "")
            if (teamPrefix.isNullOrBlank()) {
                Snackbar.make(respond_constraintLayout, "Cannot sign off. No team prefix set in settings.", Snackbar.LENGTH_LONG).show()
                dialog.cancel()
                return@setOnClickListener
            }
            sendSMSResponse(requireContext(), SARResponseCode.SIGN_OFF, dialog, daysBetween, teamPrefix)
        }
    }

    private fun safeLongToInt(l: Long): Int {
        require(!(l < Int.MIN_VALUE || l > Int.MAX_VALUE)) { "$l cannot be cast to int without changing its value." }
        return l.toInt()
    }
}

