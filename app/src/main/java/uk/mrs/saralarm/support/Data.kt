package uk.mrs.saralarm.support

import uk.mrs.saralarm.ui.settings.deepui.rules.support.SoundType
import java.io.Serializable


data class RuleAlarmData(
    val chosen: Boolean = false,
    val soundType: SoundType = SoundType.NONE,
    val soundFile: String = "",
    val isLooping: Boolean = true,
    val colorArrayList: ArrayList<String> = ArrayList(),
    val alarmPreviewSMSBody: String = "",
    val alarmPreviewSMSNumber: String = ""
) : Serializable
