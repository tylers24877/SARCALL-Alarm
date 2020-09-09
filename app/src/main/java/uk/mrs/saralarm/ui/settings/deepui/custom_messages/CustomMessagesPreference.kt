package uk.mrs.saralarm.ui.settings.deepui.custom_messages

import android.content.Context
import android.util.AttributeSet
import androidx.navigation.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import uk.mrs.saralarm.R


class CustomMessagesPreference @JvmOverloads constructor(context: Context?, attrs: AttributeSet? =null, defStyleAttr: Int = R.attr.dialogPreferenceStyle) : Preference(context, attrs, defStyleAttr){

    init{
        widgetLayoutResource = R.layout.settings_custom_preference
    }
    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        setOnPreferenceClickListener{
            holder.itemView.findNavController().navigate(R.id.action_navigation_setting_to_customMessagesFragment)
            true
        }
    }
}
