package uk.mrs.saralarm.support

import java.io.Serializable


data class RuleAlarmData(
    val chosen: Boolean = false,
    val soundFile: String = "",
    val isLooping: Boolean = true,
    val colorArrayList: ArrayList<String> = ArrayList(),
    val alarmPreviewSMSBody: String = "",
    val alarmPreviewSMSNumber: String = ""
) : Serializable