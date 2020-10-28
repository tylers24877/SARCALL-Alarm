package uk.mrs.saralarm.ui.settings.deepui.rules.support

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class RulesObject(
    var choice: RulesChoice = RulesChoice.ALL,
    var smsNumber: String = "", var phrase: String = "",
    var customAlarmRulesObject: CustomAlarmRulesObject = CustomAlarmRulesObject()
)

data class CustomAlarmRulesObject(
    var alarmSoundType: SoundType = SoundType.CUSTOM,
    var alarmFileName: String = "",
    var alarmFileLocation: String = "", var isLooping: Boolean = true,
    var colorArray: ArrayList<String> = ArrayList()
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