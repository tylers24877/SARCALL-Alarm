package uk.mrs.saralarm.ui.settings.preferences

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.widget.TextView
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder


class RedSummaryPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : Preference(context, attrs) {

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val summary = holder.findViewById(android.R.id.summary) as? TextView
        summary?.let {
            // Enable multiple line support
            it.isSingleLine = false
            it.maxLines = 10
            it.setTextColor(Color.RED)// Just need to be high enough I guess
        }
    }
}