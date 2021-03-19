package uk.mrs.saralarm.ui.respond

import android.app.Dialog
import android.content.*
import android.content.DialogInterface.BUTTON_POSITIVE
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.MaterialFadeThrough
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import uk.mrs.saralarm.R
import uk.mrs.saralarm.databinding.DialogRespondSignOnOffMulitpleTeamsBinding
import uk.mrs.saralarm.databinding.FragmentRespondBinding
import uk.mrs.saralarm.support.SARResponseCode
import uk.mrs.saralarm.support.notification.SilencedForegroundNotification
import uk.mrs.saralarm.ui.respond.dialogs.DialogSARA
import uk.mrs.saralarm.ui.respond.dialogs.DialogSARH
import uk.mrs.saralarm.ui.respond.dialogs.DialogSARL
import uk.mrs.saralarm.ui.respond.dialogs.DialogSARN
import uk.mrs.saralarm.ui.respond.dialogs.multiple_teams.MultipleTeamsDialogAdapter
import uk.mrs.saralarm.ui.respond.support.RespondSMSBroadcastReceiver
import uk.mrs.saralarm.ui.respond.support.RespondSMSBroadcastReceiver.Companion.RESPOND_SMS_BROADCAST_RECEIVER_SENT
import uk.mrs.saralarm.ui.respond.support.RespondUtil
import uk.mrs.saralarm.ui.respond.support.SMSSender.sendSMSResponse
import java.util.*


class RespondFragment : Fragment(), RespondBroadcastListener {

    private var _binding: FragmentRespondBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private val respondBroadcastReceiver: RespondBroadcastReceiver = RespondBroadcastReceiver(this)
    private val respondSMSBroadcastReceiver: RespondSMSBroadcastReceiver = RespondSMSBroadcastReceiver()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRespondBinding.inflate(inflater, container, false)

        if (!(ActivityCompat.checkSelfPermission(requireContext(), "android.permission.RECEIVE_SMS") == 0
                    && ActivityCompat.checkSelfPermission(requireContext(), "android.permission.READ_EXTERNAL_STORAGE") == 0
                    && ActivityCompat.checkSelfPermission(requireContext(), "android.permission.SEND_SMS") == 0
                    && ActivityCompat.checkSelfPermission(requireContext(), "android.permission.READ_SMS") == 0)
        ) {
            requestPermissions(
                arrayOf(
                    "android.permission.RECEIVE_SMS",
                    "android.permission.READ_EXTERNAL_STORAGE",
                    "android.permission.SEND_SMS",
                    "android.permission.READ_SMS"
                ), 0
            )
        }
        binding.apply {
            respondSarAButton.setOnClickListener {
                DialogSARA.open(requireContext(), root)
            }
            respondSarLButton.setOnClickListener {
                DialogSARL.open(requireContext(), root)
            }
            respondSarNButton.setOnClickListener {
                DialogSARN.open(requireContext(), root)
            }
            respondSarHButton.setOnClickListener {
                DialogSARH.open(requireContext(), root)
            }
            pullToRefresh.setOnRefreshListener {
                updateLatestSMS()
                pullToRefresh.isRefreshing = false
            }
            respondSignOn.setOnClickListener {
                displaySignOnDialog()
            }
            respondSignOff.setOnClickListener {
                displaySignOffDialog()
            }
        }

        context?.registerReceiver(respondBroadcastReceiver, IntentFilter("uk.mrs.saralarm.RespondFragment.SilencedForegroundNotificationClosed"))
        context?.registerReceiver(respondSMSBroadcastReceiver, IntentFilter(RESPOND_SMS_BROADCAST_RECEIVER_SENT))
        enterTransition = MaterialFadeThrough()
        exitTransition = MaterialFadeThrough()
        return binding.root
    }

    override fun onResume() {
        binding.apply {
            updateLatestSMS()
            val pref: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
            if (!pref.getBoolean("prefEnabled", false)) {
                binding.InfoView.visibility = View.VISIBLE
                binding.InfoViewTxtview.text = getString(R.string.fragment_respond_info_view_not_enabled_title)
                InfoView.setOnClickListener {
                    findNavController().navigate(R.id.action_global_navigation_settings)
                }
            } else if (SilencedForegroundNotification.isServiceAlive(requireContext(), SilencedForegroundNotification::class.java)) {
                InfoView.visibility = View.VISIBLE
                //requireView().InfoView.setBackgroundColor(Color.argb(255,100,255,3))
                InfoViewTxtview.text = getString(R.string.fragment_respond_info_view_silenced_title)
                InfoView.setOnClickListener {
                    val intent = Intent(context, SilencedForegroundNotification::class.java)
                    intent.action = "uk.mrs.saralarm.silenceForeground.stop"
                    context?.startService(intent)
                }
            } else if (pref.getString("rulesJSON", "").isNullOrBlank()) {
                InfoView.visibility = View.VISIBLE
                InfoViewTxtview.text = getString(R.string.fragment_respond_info_view_rules_not_configured_title)
                InfoView.setOnClickListener {
                    findNavController().navigate(R.id.action_global_navigation_settings)
                }
            } else if (pref.getString("respondSMSNumbersJSON", "").isNullOrEmpty()) {
                InfoView.visibility = View.VISIBLE
                InfoViewTxtview.text = getString(R.string.fragment_respond_info_no_sar_respond_number_title)
                InfoView.setOnClickListener {
                    findNavController().navigate(R.id.action_navigation_respond_to_SMSNumbersFragment)
                }
            } else {
                InfoView.visibility = View.GONE
            }
            //Hide or show the optional features
            if (pref.getBoolean("visualShowSARH", true)) {
                respondSarHButton.visibility = View.VISIBLE
            } else {
                respondSarHButton.visibility = View.GONE
            }
            if (pref.getBoolean("visualShowSignOnOff", true)) {
                respondSignOnOffSplitView.visibility = View.VISIBLE
                respondSignOn.visibility = View.VISIBLE
                respondSignOff.visibility = View.VISIBLE
            } else {
                respondSignOnOffSplitView.visibility = View.GONE
                respondSignOn.visibility = View.GONE
                respondSignOff.visibility = View.GONE
            }
        }
        super.onResume()
    }

    override fun onDestroy() {
        try {
            context?.unregisterReceiver(respondBroadcastReceiver)
        } catch (e: IllegalStateException) {
        } catch (e: IllegalArgumentException) {
        }
        try {
            context?.unregisterReceiver(respondSMSBroadcastReceiver)
        } catch (e: IllegalStateException) {
        } catch (e: IllegalArgumentException) {
        }
        super.onDestroy()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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
                binding.apply {
                    try {
                        respondSmsLoadingBar.visibility = View.VISIBLE
                        respondSmsPreviewTxtview.visibility = View.GONE
                        respondPreviewDateTxtview.visibility = View.GONE

                        val (resultBody, resultDate) = RespondUtil.setPreviewAsync(requireContext()).await()
                        respondSmsPreviewTxtview.text = resultBody
                        respondPreviewDateTxtview.text = getString(R.string.fragment_respond_preview_date_received, resultDate)

                        respondSmsLoadingBar.visibility = View.GONE
                        respondSmsPreviewTxtview.visibility = View.VISIBLE
                        respondPreviewDateTxtview.visibility = View.VISIBLE
                    } catch (e: IllegalStateException) {
                    }
                }
            }
        } else {
            try {
                binding.respondSmsPreviewTxtview.setText(R.string.fragment_response_permission_placeholder)
                binding.respondPreviewDateTxtview.text = ""
            } catch (e: IllegalStateException) {
            }
        }
    }

    private fun displaySignOnDialog() {
        val pref: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val teamPrefixArray: ArrayList<String>? = Gson().fromJson(pref.getString("respondTeamPrefixJSON", ""), object : TypeToken<ArrayList<String>?>() {}.type)
        teamPrefixArray?.removeAll(Collections.singleton(""))
        if (teamPrefixArray.isNullOrEmpty()) {
            Snackbar.make(binding.respondConstraintLayout, getString(R.string.fragment_respond_dialog_sign_on_no_team_prefix_toast), Snackbar.LENGTH_LONG).show()
            return
        }
        if (teamPrefixArray.size > 1) {
            val pairedArrayList = ArrayList<Pair<Boolean, String>>()
            for (teamPrefix in teamPrefixArray) {
                pairedArrayList.add(Pair(false, teamPrefix))
            }
            val dialogBinding: DialogRespondSignOnOffMulitpleTeamsBinding = DialogRespondSignOnOffMulitpleTeamsBinding.inflate(layoutInflater)
            val dialog = Dialog(requireContext())
            dialog.setContentView(dialogBinding.root)
            val window: Window = dialog.window!!
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            window.setGravity(Gravity.CENTER)
            dialog.show()
            dialogBinding.apply {
                respondDialogSignOnOffMultipleTeamsSelectButton.isEnabled = false

                respondDialogSignOnOffMultipleTeamsRecycler.layoutManager = LinearLayoutManager(context)
                val adapter = MultipleTeamsDialogAdapter(requireContext(), pairedArrayList, dialogBinding)
                respondDialogSignOnOffMultipleTeamsRecycler.adapter = adapter

                respondDialogSignOnOffMultipleTeamsSelectButton.setOnClickListener {
                    for (eachPair in adapter.data) {
                        if (eachPair.first) {
                            sendSMSResponse(requireContext(), requireView(), SARResponseCode.SIGN_ON, dialog, 0, eachPair.second)
                        }
                    }
                }
            }
        } else {
            val onePrefixDialogClickListener = DialogInterface.OnClickListener { _, which ->
                if (which == BUTTON_POSITIVE) {
                    sendSMSResponse(requireContext(), requireView(), SARResponseCode.SIGN_ON, null, 0, teamPrefixArray[0])
                }
            }
            AlertDialog.Builder(requireContext()).setTitle(getString(R.string.fragment_respond_dialog_sign_on_title))
                .setMessage(getString(R.string.fragment_respond_dialog_sign_on_message)).setPositiveButton(getString(R.string.yes), onePrefixDialogClickListener)
                .setNegativeButton(getString(R.string.no), onePrefixDialogClickListener).show()
        }
    }

    private fun displaySignOffDialog() {
        val pref: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val teamPrefixArray: ArrayList<String>? = Gson().fromJson(pref.getString("respondTeamPrefixJSON", ""), object : TypeToken<ArrayList<String>?>() {}.type)
        teamPrefixArray?.removeAll(Collections.singleton(""))
        if (teamPrefixArray.isNullOrEmpty()) {
            Snackbar.make(binding.respondConstraintLayout, getString(R.string.fragment_respond_dialog_sign_off_no_team_prefix_toast), Snackbar.LENGTH_LONG).show()
            return
        }
        if (teamPrefixArray.size > 1) {
            val pairedArrayList = ArrayList<Pair<Boolean, String>>()
            for (teamPrefix in teamPrefixArray) {
                pairedArrayList.add(Pair(false, teamPrefix))
            }
            val dialogBinding: DialogRespondSignOnOffMulitpleTeamsBinding = DialogRespondSignOnOffMulitpleTeamsBinding.inflate(layoutInflater)
            val dialog = Dialog(requireContext())
            dialog.setContentView(dialogBinding.root)
            val window: Window = dialog.window!!
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            window.setGravity(Gravity.CENTER)
            dialog.show()
            dialogBinding.apply {
                respondDialogSignOnOffMultipleTeamsSelectButton.isEnabled = false

                respondDialogSignOnOffMultipleTeamsRecycler.layoutManager = LinearLayoutManager(context)
                val adapter = MultipleTeamsDialogAdapter(requireContext(), pairedArrayList, dialogBinding)
                respondDialogSignOnOffMultipleTeamsRecycler.adapter = adapter

                respondDialogSignOnOffMultipleTeamsSelectButton.setOnClickListener {
                    for (eachPair in adapter.data) {
                        if (eachPair.first) {
                            sendSMSResponse(requireContext(), requireView(), SARResponseCode.SIGN_OFF, dialog, 0, eachPair.second)
                        }
                    }
                }
            }
        } else {
            val onePrefixDialogClickListener = DialogInterface.OnClickListener { _, which ->
                if (which == BUTTON_POSITIVE) {
                    sendSMSResponse(requireContext(), requireView(), SARResponseCode.SIGN_OFF, null, 0, teamPrefixArray[0])
                }
            }
            AlertDialog.Builder(requireContext()).setTitle(getString(R.string.fragment_respond_dialog_sign_off_title))
                .setMessage(getString(R.string.fragment_respond_dialog_sign_off_message)).setPositiveButton(getString(R.string.yes), onePrefixDialogClickListener)
                .setNegativeButton(getString(R.string.no), onePrefixDialogClickListener).show()
        }
    }

    override fun silencedForegroundNotificationClosed() {
        try {
            binding.InfoView.visibility = View.GONE
        } catch (e: IllegalStateException) {
        }
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