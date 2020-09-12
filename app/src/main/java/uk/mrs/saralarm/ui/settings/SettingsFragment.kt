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

            val findPreference1: Preference? = findPreference("prefEnabled")
            findPreference1!!.onPreferenceChangeListener = this

        }


       /* override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            if (data != null && requestCode == 5 && resultCode == -1) {
                val file = File(getRealPathFromURI(data.data))
                try {
                    val requireContext: Context = requireContext()
                    Intrinsics.checkNotNullExpressionValue(requireContext, "requireContext()")
                    val filesDir: File = requireContext.getFilesDir()
                    Intrinsics.checkNotNullExpressionValue(filesDir, "requireContext().filesDir")
                    companion.copy(file, filesDir)
                    val sP: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
                    val edit = sP.edit()
                    val sb = StringBuilder()
                    val requireContext2: Context = requireContext()
                    Intrinsics.checkNotNullExpressionValue(requireContext2, "requireContext()")
                    sb.append(requireContext2.getFilesDir().toString())
                    sb.append(File.separator)
                    sb.append(file.getName())
                    edit.putString("prefSoundLocation", sb.toString()).apply()
                    println(sP.getString("prefSoundLocation", "DOES NOT EXIST"))
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }*/

       /* private fun getRealPathFromURI(contentURI: Uri?): String? {
            val requireContext: Context = requireContext()
            Intrinsics.checkNotNullExpressionValue(requireContext, "requireContext()")
            val contentResolver: ContentResolver = requireContext.getContentResolver()
            Intrinsics.checkNotNull(contentURI)
            val cursor = contentResolver.query(contentURI!!, null as Array<String?>?, null as String?, null as Array<String?>?, null as String?)
                ?: return contentURI.path
            cursor.moveToFirst()
            val result = cursor.getString(cursor.getColumnIndex("_data"))
            cursor.close()
            return result
        }*/

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
                    Snackbar.make(requireView(), ("SARCALL Alarm is disabled! Please choose an activation method, then re-enable." as CharSequence), Snackbar.LENGTH_LONG).show()
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
                    Snackbar.make(requireView(), ("Error. Please choose an activation method first."), Snackbar.LENGTH_LONG).show()
                    return false
                }
            }
            return true
        }

    override fun onPreferenceClick(it: Preference?): Boolean {
        findNavController(this).navigate(R.id.action_navigation_settings_to_customiseAlarmFragment)
        return true
    }

}
}