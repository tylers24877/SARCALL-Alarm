package uk.mrs.saralarm.ui.respond

import android.content.DialogInterface
import android.content.DialogInterface.BUTTON_POSITIVE
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.android.synthetic.main.fragment_respond.*
import kotlinx.android.synthetic.main.fragment_respond.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import uk.mrs.saralarm.R
import uk.mrs.saralarm.support.SARResponseCode
import uk.mrs.saralarm.ui.respond.dialogs.DialogSARA
import uk.mrs.saralarm.ui.respond.dialogs.DialogSARH
import uk.mrs.saralarm.ui.respond.dialogs.DialogSARL
import uk.mrs.saralarm.ui.respond.dialogs.DialogSARN
import uk.mrs.saralarm.ui.respond.support.RespondRoutine
import uk.mrs.saralarm.ui.respond.support.SMS.sendSMSResponse


class RespondFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
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
        root.respond_sar_a_button.setOnClickListener {
            DialogSARA.open(requireContext(), root)
        }
        root.respond_sar_l_button.setOnClickListener {
            DialogSARL.open(requireContext(), root)
        }
        root.respond_sar_n_button.setOnClickListener {
            DialogSARN.open(requireContext(), root)
        }
        root.respond_sar_h_button.setOnClickListener {
            DialogSARH.open(requireContext(), root)
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

                    val (resultBody, resultDate) = RespondRoutine.setPreviewAsync(requireContext()).await()
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
                sendSMSResponse(requireContext(), requireView(), SARResponseCode.SIGN_ON, null, 0, teamPrefix)
            }
        }
        AlertDialog.Builder(requireContext()).setTitle("Sign On").setMessage("Are you sure?").setPositiveButton("Yes", dialogClickListener)
            .setNegativeButton("No", dialogClickListener).show()
    }

    private fun displaySignOffDialog() {
        //Temporary fix until bug in sarcall is fixed
        val dialogClickListener = DialogInterface.OnClickListener { _, which ->
            if (which == BUTTON_POSITIVE) {
                val pref: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
                val teamPrefix = pref.getString("prefTeamPrefix", "")
                if (teamPrefix.isNullOrBlank()) {
                    Snackbar.make(respond_constraintLayout, "Cannot sign off. No team prefix set in settings.", Snackbar.LENGTH_LONG).show()
                    return@OnClickListener
                }
                sendSMSResponse(requireContext(), requireView(), SARResponseCode.SIGN_OFF, null, 0, teamPrefix)
            }
        }
        AlertDialog.Builder(requireContext()).setTitle("Sign Off").setMessage("Are you sure?").setPositiveButton("Yes", dialogClickListener)
            .setNegativeButton("No", dialogClickListener).show()

        /*
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

                    if(pref.getBoolean("prefDateWorkaround",true)) {
                        if(daysBetween>=3) daysBetween++
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
         */
    }

    private fun safeLongToInt(l: Long): Int {
        require(!(l < Int.MIN_VALUE || l > Int.MAX_VALUE)) { "$l cannot be cast to int without changing its value." }
        return l.toInt()
    }
}

