package uk.mrs.saralarm.ui.settings.extra_ui.rules.support

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class RulesObject(
    var choice: RulesChoice = RulesChoice.ALL,
    var smsNumber: String = "",
    var phrase: String = "",
    var customAlarmRulesObject: CustomAlarmRulesObject = CustomAlarmRulesObject()
)

data class CustomAlarmRulesObject(
    var alarmFileName: String = "",
    var alarmFileLocation: String = "",
    var isLooping: Boolean = true,
    var colorArray: ArrayList<String> = ArrayList(),
    var alarmSoundType: SoundType = SoundType.NONE

)
enum class SoundType : Serializable {
    @SerializedName("system")
    SYSTEM,
    @SerializedName("custom")
    CUSTOM,
    @SerializedName("none")
    NONE
}
enum class RulesChoice : Serializable {
    @SerializedName("sms_number")
    SMS_NUMBER,

    @SerializedName("phrase")
    PHRASE,

    @SerializedName("all")
    ALL
}