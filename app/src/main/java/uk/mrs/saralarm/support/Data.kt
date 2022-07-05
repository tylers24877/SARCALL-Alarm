/*
 *  Copyright (C) Tyler Simmonds - All Rights Reserved
 *  Unauthorised copying of this file, via any medium is prohibited
 *  Written by Tyler Simmonds on behalf of SARCALL LTD, 2021
 *
 */

package uk.mrs.saralarm.support

import uk.mrs.saralarm.ui.settings.extra_ui.rules.support.SoundType
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
