/*
 *  Copyright (C) Tyler Simmonds - All Rights Reserved
 *  Unauthorised copying of this file, via any medium is prohibited
 *  Written by Tyler Simmonds on behalf of SARCALL LTD, 2021
 *
 */

package uk.mrs.saralarm.ui.settings.extra_ui.rules.support

import com.google.errorprone.annotations.Keep
import com.google.gson.annotations.SerializedName
import java.io.Serializable
@Keep
data class RulesObject(
    var choice: RulesChoice = RulesChoice.ALL,
    var smsNumber: String = "",
    var phrase: String = "",
    var customAlarmRulesObject: CustomAlarmRulesObject = CustomAlarmRulesObject()
)
@Keep
data class CustomAlarmRulesObject(
    var alarmFileName: String = "",
    var alarmFileLocation: String = "",
    var isLooping: Boolean = true,
    var colorArray: ArrayList<String> = ArrayList(),
    var alarmSoundType: SoundType = SoundType.NONE

)
@Keep
enum class SoundType : Serializable {
    @SerializedName("system")
    SYSTEM,

    @SerializedName("custom")
    CUSTOM,

    @SerializedName("none")
    NONE
}
@Keep
enum class RulesChoice : Serializable {
    @SerializedName("sms_number")
    SMS_NUMBER,

    @SerializedName("phrase")
    PHRASE,

    @SerializedName("all")
    ALL
}