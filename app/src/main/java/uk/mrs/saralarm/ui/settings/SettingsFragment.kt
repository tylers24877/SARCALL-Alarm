package uk.mrs.saralarm.ui.settings

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.NavHostFragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreferenceCompat
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.view.*
import uk.mrs.saralarm.R
import uk.mrs.saralarm.Widget


class SettingsFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        parentFragmentManager.beginTransaction().replace(R.id.settings_content_frame, PrefsFragment()).commit()
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }
    class PrefsFragment : PreferenceFragmentCompat(), Preference.OnPreferenceChangeListener, OnSharedPreferenceChangeListener,Preference.OnPreferenceClickListener {

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preference, rootKey)

            preferenceManager.sharedPreferences.registerOnSharedPreferenceChangeListener(this)

            (findPreference("prefEnabled") as Preference?)!!.onPreferenceChangeListener = this
            (findPreference("teamleaderMode") as Preference?)!!.onPreferenceChangeListener = this


        }

        override fun onSharedPreferenceChanged(sP: SharedPreferences, key: String) {
            if (key =="prefEnabled" && context != null) {
                val intent = Intent(context, Widget::class.java)
                intent.action = "android.appwidget.action.APPWIDGET_UPDATE"
                val ids = AppWidgetManager.getInstance(context).getAppWidgetIds(ComponentName(requireContext(), Widget::class.java))
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
            preferenceManager.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
            findPreference<SwitchPreferenceCompat>("prefEnabled")!!.isChecked = PreferenceManager.getDefaultSharedPreferences(requireContext()).getBoolean("prefEnabled", false)
        }

        override fun onPause() {
            preferenceManager.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
            super.onPause()
        }

        override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
            val sP: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
            if (preference.key == "prefEnabled" && (newValue as Boolean)) {
                if (sP.getString("rulesJSON", "")!!.isEmpty()) {
                    Snackbar.make(requireView(), ("Error. Please add a rule first."), Snackbar.LENGTH_LONG).show()
                    return false
                }
            } else
                if (preference.key == "teamleaderMode") {
                    requireView().rootView.nav_view.menu.findItem(R.id.navigation_teamleader).isVisible = newValue as Boolean
                }
            return true
        }

    override fun onPreferenceClick(it: Preference?): Boolean {
        findNavController(this).navigate(R.id.action_navigation_settings_to_customiseAlarmFragment)
        return true
    }

}
}