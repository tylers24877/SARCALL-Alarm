package uk.mrs.saralarm.ui.settings.deepui.rules.support

import com.google.gson.annotations.SerializedName

data class RulesObject(var choice: RulesChoice = RulesChoice.ALL, var smsNumber: String = "", var phrase: String = "", var customAlarmRulesObject: CustomAlarmRulesObject = CustomAlarmRulesObject())

data class CustomAlarmRulesObject(var alarmFileName: String = "", var alarmFileLocation: String = "")

enum class RulesChoice {
    @SerializedName("sms_number")
    SMS_NUMBER, @SerializedName("phrase")
    PHRASE, @SerializedName("all")
    ALL
}