package uk.mrs.saralarm.ui.respond

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
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat
import kotlinx.android.synthetic.main.dialog_respond_sar_a.*
import kotlinx.android.synthetic.main.dialog_respond_sar_l.*
import kotlinx.android.synthetic.main.fragment_respond.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import uk.mrs.saralarm.R
import uk.mrs.saralarm.support.SARResponseCode
import java.lang.reflect.Type
import java.util.*
import kotlin.collections.ArrayList


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
        root.respond_sar_l_button.setOnClickListener{
            dialogSARLOpen(context)
        }
        root.respond_sar_n_button.setOnClickListener{
            dialogSARNOpen()
        }
        root.pullToRefresh.setOnRefreshListener {
            updateLatestSMS()
            root.pullToRefresh.isRefreshing = false
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
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, customMessageArray)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        dialog.sar_a_spinner.adapter = adapter


        dialog.sar_a_spinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener{
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
                    val respondViewModel: RespondViewModel = getRespondViewModel()
                    respondViewModel.setEta(progressCal)
                    val sb = SpannableStringBuilder()
                    sb.append("Estimated time to arrival: ")
                    sb.append(progressCal.toString())
                    sb.append(" minutes")
                    sb.setSpan(StyleSpan(1), 27, progressCal.toString().length + 27, 18)
                    dialog.respond_dialog_sar_a_seek_eta_txtview.text = sb

                    seekBar.secondaryProgress = progress
                }
            }
        })
        dialog.respond_dialog_sar_a_seek.progress = 5
        dialog.respond_dialog_sar_a_submit_button.setOnClickListener {
            val progressCal = dialog.respond_dialog_sar_a_seek.progress * 5
            if(dialog.sar_a_spinner.selectedItem=="Enter Custom Message...") {
                sendSMSResponse(requireContext(), dialog, SARResponseCode.SAR_A, progressCal, dialog.respond_dialog_sar_a_message_editview.text.toString())
            }else{
                sendSMSResponse(requireContext(), dialog, SARResponseCode.SAR_A, progressCal, dialog.sar_a_spinner.selectedItem.toString())
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
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, customMessageArray)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)


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
                    val respondViewModel: RespondViewModel = getRespondViewModel()
                    respondViewModel.setEta(progressCal)
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
                    seekBar.secondaryProgress = progress
                }
            }
        })
        dialog.respond_dialog_sar_l_seek.progress = 5
        dialog.respond_dialog_sar_l_submit_button.setOnClickListener {
            val progressCal = dialog.respond_dialog_sar_l_seek.progress * 5
            if(dialog.sar_l_spinner.selectedItem=="Enter Custom Message...") {
                sendSMSResponse(requireContext(), dialog, SARResponseCode.SAR_L, progressCal, dialog.respond_dialog_sar_l_message_editview.text.toString())
            }else{
                sendSMSResponse(requireContext(), dialog, SARResponseCode.SAR_L, progressCal, dialog.sar_l_spinner.selectedItem.toString())
            }
        }

    }

    private fun dialogSARNOpen() {
        val dialogClickListener  =  DialogInterface.OnClickListener{ dialog, which ->
            if (which == BUTTON_POSITIVE) {
                sendSMSResponse(requireContext(), dialog, SARResponseCode.SAR_N, 0, null)
            }
        }
        AlertDialog.Builder(requireContext()).setMessage(R.string.SAR_N_dialog_title).setPositiveButton(R.string.sar_n_positive, dialogClickListener)
            .setNegativeButton(R.string.sar_n_negitive, dialogClickListener).show()
    }


    private fun sendSMSResponse(context: Context, dialog: DialogInterface, sarResponseCode: SARResponseCode, eta: Int, message: String?) {
        val sb: StringBuilder
        val smsManager: SmsManager = SmsManager.getDefault()
        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        val phoneUtil = PhoneNumberUtil.getInstance()
        try {
            val type: Type = object : TypeToken<java.util.ArrayList<String>?>() {}.type
            val phoneNumberSet: java.util.ArrayList<String>? = Gson().fromJson(pref.getString("respondSMSNumbersJSON", ""), type)
            if (phoneNumberSet.isNullOrEmpty()) {
                Toast.makeText(context, "Failed! No SARCALL number chosen. Please check/add number in settings.", Toast.LENGTH_LONG).show()
                return
            }

            when (sarResponseCode) {
                SARResponseCode.SAR_A -> {
                    if (eta != 0) {
                        sb = StringBuilder()
                        sb.append("SAR A")
                        sb.append(eta)
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
                    dialog.cancel()
                }
                SARResponseCode.SAR_L -> {
                    if (eta != 0) {
                        sb = StringBuilder()
                        sb.append("SAR L")
                        sb.append(eta)
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
                    dialog.cancel()
                }
                SARResponseCode.SAR_N -> {
                    for (phoneNumber in phoneNumberSet) {
                        smsManager.sendTextMessage(phoneUtil.format(phoneUtil.parse(phoneNumber, "GB"), PhoneNumberFormat.INTERNATIONAL), null, "SAR N", null, null)
                    }
                    Toast.makeText(context, "SAR N sent to SARCALL", Toast.LENGTH_LONG).show()
                    dialog.cancel()
                }
            }
        } catch (e7: NumberParseException) {
            Toast.makeText(context, "Failed! SARCALL SMS number is formatted wrong. Please check number in settings.", Toast.LENGTH_LONG).show()
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
    fun getRespondViewModel(): RespondViewModel {
        return respondViewModel!!
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
                    requireView().respond_preview_date_txtview.text = resultDate

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
}

