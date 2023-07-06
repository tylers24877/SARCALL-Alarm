/*
 *  Copyright (C) Tyler Simmonds - All Rights Reserved
 *  Unauthorised copying of this file, via any medium is prohibited
 *  Written by Tyler Simmonds on behalf of SARCALL LTD, 2021
 *
 */

package uk.mrs.saralarm.ui.respond

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import com.google.android.material.transition.MaterialFadeThrough
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.what3words.androidwrapper.What3WordsV3
import com.what3words.javawrapper.response.APIResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import uk.me.jstott.jcoord.OSRef
import uk.mrs.saralarm.R
import uk.mrs.saralarm.databinding.FragmentRespondBinding
import uk.mrs.saralarm.support.Permissions
import uk.mrs.saralarm.support.UpdateUtil
import uk.mrs.saralarm.support.notification.SilencedForegroundNotification
import uk.mrs.saralarm.ui.respond.dialogs.DialogSARA
import uk.mrs.saralarm.ui.respond.dialogs.DialogSARH
import uk.mrs.saralarm.ui.respond.dialogs.DialogSARL
import uk.mrs.saralarm.ui.respond.dialogs.DialogSARN
import uk.mrs.saralarm.ui.respond.dialogs.DialogSignOff
import uk.mrs.saralarm.ui.respond.dialogs.DialogSignOn
import uk.mrs.saralarm.ui.respond.support.NationalGrid
import uk.mrs.saralarm.ui.respond.support.RespondSMSBroadcastReceiver
import uk.mrs.saralarm.ui.respond.support.RespondSMSBroadcastReceiver.Companion.RESPOND_SMS_BROADCAST_RECEIVER_SENT
import uk.mrs.saralarm.ui.respond.support.RespondUtil
import java.util.*
import java.util.regex.Pattern


class RespondFragment : Fragment(), RespondBroadcastListener {

    private var _binding: FragmentRespondBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private val respondBroadcastReceiver: RespondBroadcastReceiver = RespondBroadcastReceiver(this)
    private val respondSMSBroadcastReceiver: RespondSMSBroadcastReceiver = RespondSMSBroadcastReceiver()

    private lateinit var clickPrefs: SharedPreferences
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRespondBinding.inflate(inflater, container, false)

        val permissionsToRequestNotGranted = Permissions.checkPermissions(requireContext())
        if (permissionsToRequestNotGranted.isNotEmpty()) {
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                if (permissions["android.permission.READ_SMS"] == true) {
                    runBlocking { updateLatestSMS() }
                }
            }.launch(permissionsToRequestNotGranted.toTypedArray())
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
                runBlocking { updateLatestSMS() }
                pullToRefresh.isRefreshing = false
            }
            respondSignOn.setOnClickListener {
                DialogSignOn.open(requireContext(), root, binding)
            }
            respondSignOff.setOnClickListener {
                DialogSignOff.open(requireContext(), root, binding)
            }
        }

        context?.registerReceiver(respondBroadcastReceiver, IntentFilter("uk.mrs.saralarm.RespondFragment.SilencedForegroundNotificationClosed"))
        context?.registerReceiver(respondSMSBroadcastReceiver, IntentFilter(RESPOND_SMS_BROADCAST_RECEIVER_SENT))
        enterTransition = MaterialFadeThrough()
        exitTransition = MaterialFadeThrough()

        clickPrefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return binding.root
    }

    override fun onResume() {
        binding.apply {
            runBlocking { updateLatestSMS() }
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
        } catch (_: IllegalStateException) {
        } catch (_: IllegalArgumentException) {
        }
        try {
            context?.unregisterReceiver(respondSMSBroadcastReceiver)
        } catch (_: IllegalStateException) {
        } catch (_: IllegalArgumentException) {
        }
        super.onDestroy()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun updateLatestSMS() {
        if (ActivityCompat.checkSelfPermission(requireContext(), "android.permission.READ_SMS") == 0) {
            lifecycleScope.launch {
                binding.apply {
                    respondSmsLoadingBar.visibility = View.VISIBLE
                    respondSmsPreviewTxtview.visibility = View.GONE
                    respondPreviewDateTxtview.visibility = View.GONE

                    RespondUtil.setPreviewFlow(requireContext())
                        .onStart { emit(Pair("", "")) }
                        .catch { emit(Pair(getString(R.string.fragment_respond_preview_setup_needed_warning), "")) }
                        .collect { (resultBody, resultDate) ->
                            val regexOSGB = Pattern.compile("\\s([STNHstnh][A-Za-z]\\s?)(\\d{5}\\s?\\d{5}|\\d{4}\\s?\\d{4}|\\d{3}\\s?\\d{3})")
                            val regexW3W = Pattern.compile("(?:\\p{L}\\p{M}*)+[.｡。･・︒។։။۔።।](?:\\p{L}\\p{M}*)+[.｡。･・︒។։။۔።।](?:\\p{L}\\p{M}*)+")

                            try {
                                val str = SpannableString(resultBody)
                                val matcher = regexOSGB.matcher(resultBody)
                                val matcherW3W = regexW3W.matcher(resultBody)
                                var matchStart: Int
                                var matchEnd: Int

                                while (matcher.find()){
                                    matchStart = matcher.start(1)
                                    matchEnd = matcher.end()

                                    val match = resultBody.substring(matchStart, matchEnd).uppercase(Locale.ROOT)
                                    try {
                                        val northernAndEastings = NationalGrid.fromNationalGrid(match)
                                        val clickableSpan: ClickableSpan = object : ClickableSpan() {
                                            override fun onClick(widget: View) {
                                                val param = Bundle()
                                                param.putString("null","null")
                                                FirebaseAnalytics.getInstance(requireContext()).logEvent("grid_reference_clicked", param)
                                                val os1 = OSRef(northernAndEastings[0], northernAndEastings[1])
                                                val ll1 = os1.toLatLng()
                                                val lat = ll1.latitude
                                                val lon = ll1.longitude
                                                val matchPlain = match.replace(" ", "+")

                                                val gmmIntentUri = Uri.parse("geo:0,0?q=$lat,$lon($matchPlain)")
                                                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                                               mapIntent.setPackage("com.google.android.apps.maps")
                                               startActivity(mapIntent)
                                            }

                                            override fun updateDrawState(ds: TextPaint) {
                                                super.updateDrawState(ds)
                                                ds.isUnderlineText = false
                                            }
                                        }
                                        str.setSpan(clickableSpan, matchStart, matchEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                                    }catch (_:java.lang.IllegalArgumentException ){ }
                                }

                                while (matcherW3W.find()){
                                    matchStart = matcherW3W.start(0)
                                    matchEnd = matcherW3W.end()

                                    val match = resultBody.substring(matchStart, matchEnd).uppercase(Locale.ROOT)
                                    val clickableSpan: ClickableSpan = object : ClickableSpan() {
                                        override fun onClick(widget: View) {
                                            val param = Bundle()
                                            param.putString("null","null")
                                            FirebaseAnalytics.getInstance(requireContext())
                                                .logEvent("W3W_clicked", param)
                                            val updateUtil = UpdateUtil()

                                            updateUtil.remoteLiveDataW3WClickLimit.observe(requireActivity()) { clickLimit ->
                                                val currentTime = System.currentTimeMillis()
                                                val elapsedTime = currentTime - clickPrefs.getLong(KEY_LAST_CLICK_TIME, 0L)

                                                if (elapsedTime <= 3600000) { // 3600000 milliseconds = 1 hour
                                                    val clickCount = clickPrefs.getInt(KEY_CLICK_COUNT, 0) + 1
                                                    if (clickCount > clickLimit.toInt()) {
                                                        // Perform action when the limit is reached
                                                        // Example: show a toast message
                                                        Toast.makeText(context, "Click limit exceeded!", Toast.LENGTH_SHORT).show()
                                                        return@observe
                                                    }
                                                    clickPrefs.edit()
                                                        .putInt(KEY_CLICK_COUNT, clickCount)
                                                        .apply()
                                                } else {
                                                    clickPrefs.edit()
                                                        .putInt(KEY_CLICK_COUNT, 1)
                                                        .apply()
                                                }

                                                clickPrefs.edit()
                                                    .putLong(KEY_LAST_CLICK_TIME, currentTime)
                                                    .apply()

                                                updateUtil.remoteLiveDataW3W.observe(requireActivity()) {
                                                    val wrapper = What3WordsV3(it, requireContext())
                                                    CoroutineScope(Dispatchers.IO).launch {
                                                        //use wrapper.convertTo3wa() with Dispatcher.IO - background thread
                                                        val result = wrapper.convertToCoordinates(match).execute()
                                                        CoroutineScope(Dispatchers.Main).launch {
                                                            //use Dispatcher.Main to update your views with the results if needed - Main thread
                                                            if (result.isSuccessful) {
                                                                val gmmIntentUri =
                                                                    Uri.parse("geo:0,0?q=${result.coordinates.lat},${result.coordinates.lng}($match)")

                                                                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                                                                mapIntent.setPackage("com.google.android.apps.maps")
                                                                startActivity(mapIntent)
                                                            } else {
                                                                when (result.error) {
                                                                    APIResponse.What3WordsError.BAD_WORDS -> {
                                                                        Toast.makeText(requireContext(),"This is not a valid W3W.",Toast.LENGTH_SHORT).show()
                                                                    }
                                                                    APIResponse.What3WordsError.NETWORK_ERROR -> {
                                                                        Toast.makeText(requireContext(),"An internet connection is needed to open W3W.",Toast.LENGTH_SHORT).show()
                                                                    }
                                                                    else -> {
                                                                        Toast.makeText(requireContext(), result.error.message,Toast.LENGTH_LONG).show()
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                                updateUtil.remoteConfigurationW3W()
                                            }
                                            updateUtil.remoteConfigurationW3WClickLimit()
                                        }

                                        override fun updateDrawState(ds: TextPaint) {
                                            super.updateDrawState(ds)
                                            ds.isUnderlineText = false
                                        }
                                    }
                                    str.setSpan(clickableSpan, matchStart, matchEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                                }
                                respondSmsPreviewTxtview.text = str
                                respondSmsPreviewTxtview.movementMethod = LinkMovementMethod.getInstance()
                            }catch (e: Exception){
                                e.printStackTrace()
                            }

                            respondPreviewDateTxtview.text = getString(R.string.fragment_respond_preview_date_received, resultDate)
                        }

                    respondSmsLoadingBar.visibility = View.GONE
                    respondSmsPreviewTxtview.visibility = View.VISIBLE
                    respondPreviewDateTxtview.visibility = View.VISIBLE
                }
            }
        } else {
            binding.apply {
                respondSmsPreviewTxtview.setText(R.string.fragment_response_permission_placeholder)
                respondPreviewDateTxtview.text = ""
                respondSmsLoadingBar.visibility = View.GONE
                respondSmsPreviewTxtview.visibility = View.VISIBLE
                respondPreviewDateTxtview.visibility = View.VISIBLE
            }
        }
    }


    override fun silencedForegroundNotificationClosed() {
        try {
            binding.InfoView.visibility = View.GONE
        } catch (e: IllegalStateException) {
            FirebaseCrashlytics.getInstance().recordException(e)
        }
    }

    companion object {
        private const val PREFS_NAME = "ClickPrefs"
        private const val KEY_CLICK_COUNT = "clickCount"
        private const val KEY_LAST_CLICK_TIME = "lastClickTime"
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