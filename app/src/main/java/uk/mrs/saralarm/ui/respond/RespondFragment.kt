package uk.mrs.saralarm.ui.respond

import android.content.*
import android.content.DialogInterface.BUTTON_POSITIVE
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
import uk.mrs.saralarm.support.notification.SilencedForegroundNotification
import uk.mrs.saralarm.ui.respond.dialogs.DialogSARA
import uk.mrs.saralarm.ui.respond.dialogs.DialogSARH
import uk.mrs.saralarm.ui.respond.dialogs.DialogSARL
import uk.mrs.saralarm.ui.respond.dialogs.DialogSARN
import uk.mrs.saralarm.ui.respond.support.RespondSMSBroadcastReceiver
import uk.mrs.saralarm.ui.respond.support.RespondSMSBroadcastReceiver.Companion.RESPOND_SMS_BROADCAST_RECEIVER_SENT
import uk.mrs.saralarm.ui.respond.support.RespondUtil
import uk.mrs.saralarm.ui.respond.support.SMSSender.sendSMSResponse


class RespondFragment : Fragment(), RespondBroadcastListener {

    private val respondBroadcastReceiver: RespondBroadcastReceiver = RespondBroadcastReceiver(this)
    private val respondSMSBroadcastReceiver: RespondSMSBroadcastReceiver = RespondSMSBroadcastReceiver()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val root: View = inflater.inflate(R.layout.fragment_respond, container, false)
        if (!(ActivityCompat.checkSelfPermission(requireContext(), "android.permission.RECEIVE_SMS") == 0
                    && ActivityCompat.checkSelfPermission(requireContext(), "android.permission.WRITE_EXTERNAL_STORAGE") == 0
                    && ActivityCompat.checkSelfPermission(requireContext(), "android.permission.READ_EXTERNAL_STORAGE") == 0
                    && ActivityCompat.checkSelfPermission(requireContext(), "android.permission.SEND_SMS") == 0
                    && ActivityCompat.checkSelfPermission(requireContext(), "android.permission.READ_SMS") == 0)
        ) {
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

        context?.registerReceiver(
            respondBroadcastReceiver,
            IntentFilter("uk.mrs.saralarm.RespondFragment.SilencedForegroundNotificationClosed")
        )
        context?.registerReceiver(respondSMSBroadcastReceiver, IntentFilter(RESPOND_SMS_BROADCAST_RECEIVER_SENT))
        return root
    }

    override fun onResume() {
        updateLatestSMS()
        val pref: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        if (!pref.getBoolean("prefEnabled", false)) {
            requireView().InfoView.visibility = View.VISIBLE
            requireView().InfoView_txtview.text = "SARCALL Alarm not enabled.\nTap to go to settings."
            requireView().InfoView.setOnClickListener {
                findNavController().navigate(R.id.action_navigation_respond_to_navigation_settings)
            }
        } else if (SilencedForegroundNotification.isServiceAlive(requireContext(), SilencedForegroundNotification::class.java)) {
            requireView().InfoView.visibility = View.VISIBLE
            //requireView().InfoView.setBackgroundColor(Color.argb(255,100,255,3))
            requireView().InfoView_txtview.text = "Alarm is currently silenced.\nPlease see notifcation or click to cancel."
            requireView().InfoView.setOnClickListener {
                val intent = Intent(context, SilencedForegroundNotification::class.java)
                intent.action = "uk.mrs.saralarm.silenceForeground.stop"
                context?.startService(intent)
            }
        } else if (pref.getString("rulesJSON", "").isNullOrBlank()) {
            requireView().InfoView.visibility = View.VISIBLE
            requireView().InfoView_txtview.text = "Rules are not configured correctly.\nPlease click to check."
            requireView().InfoView.setOnClickListener {
                findNavController().navigate(R.id.action_navigation_respond_to_navigation_settings)
            }
        } else if (pref.getString("respondSMSNumbersJSON", "").isNullOrEmpty()) {
            requireView().InfoView.visibility = View.VISIBLE
            requireView().InfoView_txtview.text = "No SAR respond number configured. Please click to setup."
            requireView().InfoView.setOnClickListener {
                findNavController().navigate(R.id.action_navigation_respond_to_SMSNumbersFragment)
            }
        } else {
            requireView().InfoView.visibility = View.GONE
        }
        //Hide or show the optional features
        if (pref.getBoolean("visualShowSARH", true)) {
            requireView().respond_sar_h_button.visibility = View.VISIBLE
        } else {
            requireView().respond_sar_h_button.visibility = View.GONE
        }
        if (pref.getBoolean("visualShowSignOnOff", true)) {
            requireView().respond_sign_on_off_split_view.visibility = View.VISIBLE
            requireView().respond_sign_on.visibility = View.VISIBLE
            requireView().respond_sign_off.visibility = View.VISIBLE
        } else {
            requireView().respond_sign_on_off_split_view.visibility = View.GONE
            requireView().respond_sign_on.visibility = View.GONE
            requireView().respond_sign_off.visibility = View.GONE
        }
        super.onResume()
    }

    override fun onDestroy() {
        context?.unregisterReceiver(respondBroadcastReceiver)
        context?.unregisterReceiver(respondSMSBroadcastReceiver)
        super.onDestroy()
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

                    val (resultBody, resultDate) = RespondUtil.setPreviewAsync(requireContext()).await()
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
                requireView().respond_sms_preview_txtview.setText(R.string.fragment_response_permission_placeholder)
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
    }

    override fun silencedForegroundNotificationClosed() {
        requireView().InfoView.visibility = View.GONE
    }
}

class RespondBroadcastReceiver(private val listener: RespondBroadcastListener) : BroadcastReceiver() {

    override fun onReceive(context: Context, i: Intent) {
        when (i.action) {
            "uk.mrs.saralarm.RespondFragment.SilencedForegroundNotificationClosed" -> {
                listener.silencedForegroundNotificationClosed()
            }
        }
    }
}

interface RespondBroadcastListener {
    fun silencedForegroundNotificationClosed()
}