/*
 *  Copyright (C) Tyler Simmonds - All Rights Reserved
 *  Unauthorised copying of this file, via any medium is prohibited
 *  Written by Tyler Simmonds on behalf of SARCALL LTD, 2021
 *
 */

package uk.mrs.saralarm.ui.settings

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.*
import androidx.annotation.Keep
import androidx.core.content.pm.PackageInfoCompat.getLongVersionCode
import androidx.core.os.bundleOf
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreferenceCompat
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.MaterialFadeThrough
import com.google.android.material.transition.MaterialSharedAxis
import com.google.firebase.crashlytics.FirebaseCrashlytics
import uk.mrs.saralarm.R
import uk.mrs.saralarm.databinding.FragmentSettingsBinding
import uk.mrs.saralarm.support.WidgetProvider


class SettingsFragment : Fragment(), MenuProvider {

    private var _binding: FragmentSettingsBinding? = null

    // This property is only valid between onCreateView and
// onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)

        @Keep
        when (requireArguments().getSerializable("sub_category") as SubCategory) {
            SubCategory.NONE ->
                parentFragmentManager.beginTransaction().replace(R.id.settings_content_frame, PrefsFragment()).commit()
            SubCategory.ABOUT_CATEGORY ->
                parentFragmentManager.beginTransaction().replace(R.id.settings_content_frame, AboutPrefsFragment()).commit()
        }


        enterTransition = MaterialFadeThrough()
        exitTransition = MaterialSharedAxis(MaterialSharedAxis.X, /* forward = */ true)
        reenterTransition = MaterialSharedAxis(MaterialSharedAxis.X, /* forward = */ false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        // You can hide the state of the menu item here if you call getActivity().supportInvalidateOptionsMenu(); somewhere in your code
        val menuItem: MenuItem = menu.findItem(R.id.action_bar_settings)
        menuItem.isVisible = false
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return false
    }
    @Keep
    enum class SubCategory { NONE, ABOUT_CATEGORY }
}

class PrefsFragment : PreferenceFragmentCompat(), Preference.OnPreferenceChangeListener, OnSharedPreferenceChangeListener, Preference.OnPreferenceClickListener {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preference, rootKey)

        preferenceManager.sharedPreferences?.registerOnSharedPreferenceChangeListener(this)

        (findPreference("prefEnabled") as Preference?)!!.onPreferenceChangeListener = this


        (findPreference("AboutCategory") as Preference?)!!.setOnPreferenceClickListener {
            findNavController().navigate(R.id.action_navigation_sub_category, bundleOf("sub_category" to SettingsFragment.SubCategory.ABOUT_CATEGORY))
            return@setOnPreferenceClickListener true
        }
    }

    override fun onSharedPreferenceChanged(sP: SharedPreferences, key: String) {
        if (key == "prefEnabled" && context != null) {
            val intent = Intent(context, WidgetProvider::class.java)
            intent.action = "android.appwidget.action.APPWIDGET_UPDATE"
            val ids = AppWidgetManager.getInstance(context).getAppWidgetIds(ComponentName(requireContext(), WidgetProvider::class.java))
            if (ids != null) {
                if (ids.isNotEmpty()) {
                    intent.putExtra("appWidgetIds", ids)
                    requireContext().sendBroadcast(intent)
                }
            }
        }
        if (sP.getBoolean("prefEnabled", false)) {
            if (sP.getString("rulesJSON", "")!!.isEmpty()) {
                findPreference<SwitchPreferenceCompat>("prefEnabled")!!.isChecked = false
                Snackbar.make(requireView(), ("SARCALL Alarm is disabled! Please add a rule, then re-enable." as CharSequence), Snackbar.LENGTH_LONG).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        preferenceManager.sharedPreferences?.registerOnSharedPreferenceChangeListener(this)
        findPreference<SwitchPreferenceCompat>("prefEnabled")!!.isChecked = PreferenceManager.getDefaultSharedPreferences(requireContext()).getBoolean("prefEnabled", false)
    }

    override fun onPause() {
        preferenceManager.sharedPreferences?.unregisterOnSharedPreferenceChangeListener(this)
        super.onPause()
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
        val sP: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        if (preference.key == "prefEnabled" && (newValue as Boolean)) {
            if (sP.getString("rulesJSON", "")!!.isEmpty()) {
                Snackbar.make(requireView(), ("Error. Please add a rule first."), Snackbar.LENGTH_LONG).show()
                return false
            }
        }
        return true
    }

    override fun onPreferenceClick(preference: Preference): Boolean {
        //findNavController(this).navigate(R.id.action_navigation_settings_to_customiseAlarmFragment)
        return true
    }
}

class AboutPrefsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.about_preference, rootKey)

        preferenceManager.findPreference<Preference>("backgroundWorkerCount")?.summary =
            preferenceManager.sharedPreferences?.getInt("WorkerCount", 0).toString()
        preferenceManager.findPreference<Preference>("backgroundWorkerTime")?.summary =
            preferenceManager.sharedPreferences?.getString("WorkerTime", "None yet...")

        try {
            preferenceManager.findPreference<Preference>("appVersion")?.summary = appVersion()
        } catch (e: PackageManager.NameNotFoundException) {
            FirebaseCrashlytics.getInstance().recordException(e)
        }
    }

    @Throws(PackageManager.NameNotFoundException::class)
    fun appVersion(): String {
        val pInfo: PackageInfo =
            requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
        return pInfo.versionName + " (" + getLongVersionCode(pInfo) + ")"
    }
}