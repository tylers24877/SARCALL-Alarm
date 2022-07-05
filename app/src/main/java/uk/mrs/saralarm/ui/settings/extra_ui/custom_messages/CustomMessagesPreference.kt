/*
 *  Copyright (C) Tyler Simmonds - All Rights Reserved
 *  Unauthorised copying of this file, via any medium is prohibited
 *  Written by Tyler Simmonds on behalf of SARCALL LTD, 2021
 *
 */

package uk.mrs.saralarm.ui.settings.extra_ui.custom_messages

import android.content.Context
import android.util.AttributeSet
import androidx.navigation.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import uk.mrs.saralarm.R


class CustomMessagesPreference @JvmOverloads constructor(context: Context?, attrs: AttributeSet? = null, defStyleAttr: Int = R.attr.dialogPreferenceStyle) : Preference(context!!, attrs, defStyleAttr) {

    init {
        widgetLayoutResource = R.layout.settings_custom_preference
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        setOnPreferenceClickListener {
            holder.itemView.findNavController().navigate(R.id.action_navigation_setting_to_customMessagesFragment)
            true
        }
    }
}
