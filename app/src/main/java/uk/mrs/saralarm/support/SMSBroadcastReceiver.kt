package uk.mrs.saralarm.support

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Build.VERSION
import android.telephony.PhoneNumberUtils
import android.telephony.SmsMessage
import android.view.Display
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.Phonenumber
import uk.mrs.saralarm.support.notification.AlarmForegroundNotification
import uk.mrs.saralarm.support.notification.SilencedForegroundNotification
import uk.mrs.saralarm.ui.settings.extra_ui.rules.support.RulesChoice
import uk.mrs.saralarm.ui.settings.extra_ui.rules.support.RulesObject
import java.util.regex.Pattern


class SMSBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pref: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        if (pref.getBoolean("prefEnabled", true)) {
            val phoneUtil = PhoneNumberUtil.getInstance()

            val rulesFromJson: ArrayList<RulesObject>? = Gson().fromJson(
                pref.getString("rulesJSON", ""),
                object : TypeToken<ArrayList<RulesObject>?>() {}.type
            )

            val rulesBothSet = HashSet<RulesObject>()
            val rulesSMSSet = HashSet<RulesObject>()
            val rulesPhraseSet = HashSet<RulesObject>()

            if (rulesFromJson.isNullOrEmpty()) return else {
                for (r in rulesFromJson) {
                    if (r.choice == RulesChoice.ALL && r.smsNumber.isNotBlank() && r.phrase.isNotBlank()) {
                        try {
                            rulesBothSet.add(r)
                        } catch (e: NumberParseException) {
                        }
                    } else if (r.choice == RulesChoice.SMS_NUMBER && r.smsNumber.isNotBlank()) {
                        rulesSMSSet.add(r)
                    } else if (r.choice == RulesChoice.PHRASE && r.phrase.isNotBlank()) {
                        rulesPhraseSet.add(r)
                    }
                }
            }

            if (intent.action.equals("android.provider.Telephony.SMS_RECEIVED")) {
                val bundle = intent.extras
                val messages: Array<SmsMessage?>
                var strMessage = ""
                var smsNumber: String
                val format = bundle!!.getString("format")
                // Retrieve the SMS message received.
                val pdus = bundle["pdus"] as Array<*>?
                if (pdus != null) { // Check the Android version.
                    // Fill the msgs array.
                    messages = arrayOfNulls(pdus.size)
                    for (i in messages.indices) {
                        if (VERSION.SDK_INT >= Build.VERSION_CODES.M) { // If Android version M or newer:
                            messages[i] = SmsMessage.createFromPdu(pdus[i] as ByteArray, format)
                        } else { // If Android version L or older:
                            messages[i] = SmsMessage.createFromPdu(pdus[i] as ByteArray)
                        }
                        // Build the message to show.
                        strMessage += messages[i]?.messageBody
                    }
                    smsNumber = messages[0]!!.displayOriginatingAddress
                    checkMessages(strMessage, smsNumber, rulesBothSet, rulesSMSSet, rulesPhraseSet, phoneUtil, context)
                }
            }
        }
    }

    private fun checkMessages(
        strMessage: String,
        smsNumber: String,
        rulesBothSet: HashSet<RulesObject>,
        rulesSMSSet: HashSet<RulesObject> = HashSet(),
        rulesPhraseSet: HashSet<RulesObject>,
        phoneUtil: PhoneNumberUtil,
        context: Context
    ) {

        val checkRulesBoth = checkRulesBoth(rulesBothSet, strMessage, smsNumber, phoneUtil)
        if (checkRulesBoth.chosen) {
            if (checkScreenState(context)) {
                startAlarmForegroundService(context, checkRulesBoth)
                FirebaseAnalytics.getInstance(context.applicationContext).logEvent("alarm_started_unlocked", null)
            } else {
                FirebaseAnalytics.getInstance(context.applicationContext).logEvent("alarm_started_locked", null)
                startAlarmForegroundService(context, checkRulesBoth)
            }
        } else {
            val checkRulesSMSNumber = checkRulesSMSNumber(rulesSMSSet, smsNumber, phoneUtil, strMessage)
            if (checkRulesSMSNumber.chosen) {
                if (checkScreenState(context)) {
                    startAlarmForegroundService(context, checkRulesSMSNumber)
                    FirebaseAnalytics.getInstance(context.applicationContext).logEvent("alarm_started_unlocked", null)
                } else {
                    FirebaseAnalytics.getInstance(context.applicationContext).logEvent("alarm_started_locked", null)
                    startAlarmForegroundService(context, checkRulesSMSNumber)
                }
            } else {
                val checkRulesPhrase = checkRulesPhrase(rulesPhraseSet, strMessage, smsNumber)
                if (checkRulesPhrase.chosen) {
                    if (checkScreenState(context)) {
                        startAlarmForegroundService(context, checkRulesPhrase)
                        FirebaseAnalytics.getInstance(context.applicationContext).logEvent("alarm_started_unlocked", null)
                    } else {
                        FirebaseAnalytics.getInstance(context.applicationContext).logEvent("alarm_started_locked", null)
                        startAlarmForegroundService(context, checkRulesPhrase)
                    }
                }
            }
        }
    }
    private fun checkRulesPhrase(SS: HashSet<RulesObject>, m: String, num: String): RuleAlarmData {
        for (s in SS) {
            if (Pattern.compile(s.phrase, Pattern.CASE_INSENSITIVE + Pattern.LITERAL).matcher(m).find()) {
                return RuleAlarmData(
                    true, s.customAlarmRulesObject.alarmSoundType, s.customAlarmRulesObject.alarmFileLocation, s.customAlarmRulesObject.isLooping,
                    s.customAlarmRulesObject.colorArray, m, num
                )
            }
        }
        return RuleAlarmData(false)
    }

    private fun checkRulesSMSNumber(rulesSMSSet: HashSet<RulesObject>, phoneNumberC: String, phoneUtil: PhoneNumberUtil, m: String): RuleAlarmData {
        for (s in rulesSMSSet) {
            try {
                val formattedNumber: Phonenumber.PhoneNumber = phoneUtil.parse(s.smsNumber, "GB")
                if (PhoneNumberUtils.compare(phoneUtil.format(formattedNumber, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL), phoneNumberC)) {
                    return RuleAlarmData(
                        true, s.customAlarmRulesObject.alarmSoundType, s.customAlarmRulesObject.alarmFileLocation, s.customAlarmRulesObject.isLooping,
                        s.customAlarmRulesObject.colorArray, m, phoneNumberC
                    )
                }
            } catch (e: NumberParseException) {
            }
        }
        return RuleAlarmData(false)
    }

    private fun checkRulesBoth(SS: Set<RulesObject>, body: String, receivedNumber: String, phoneUtil: PhoneNumberUtil): RuleAlarmData {
        for (s in SS) {
            try {
                if (Pattern.compile(s.phrase, Pattern.CASE_INSENSITIVE + Pattern.LITERAL).matcher(body).find()) {
                    val formattedNumber: Phonenumber.PhoneNumber = phoneUtil.parse(s.smsNumber, "GB")
                    if (PhoneNumberUtils.compare(phoneUtil.format(formattedNumber, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL), receivedNumber)) {
                        return RuleAlarmData(
                            true, s.customAlarmRulesObject.alarmSoundType, s.customAlarmRulesObject.alarmFileLocation, s.customAlarmRulesObject.isLooping,
                            s.customAlarmRulesObject.colorArray, body, receivedNumber
                        )
                    }
                }
            } catch (e: NumberParseException) {
            }
        }
        return RuleAlarmData(false)
    }

    private fun checkScreenState(context: Context): Boolean {
            val dm = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
            for (display in dm.displays) {
                if (display.state == Display.STATE_ON) {
                    return true
                }
            }
        return false
    }

    private fun startAlarmForegroundService(context: Context, ruleAlarmData: RuleAlarmData) {
        //check if the silence foreground service is active, else start alarm
        if (!SilencedForegroundNotification.isServiceAlive(context, SilencedForegroundNotification::class.java)) {
            val serviceIntent = Intent(context, AlarmForegroundNotification::class.java)
            serviceIntent.putExtra("ruleAlarmData", ruleAlarmData)
            ContextCompat.startForegroundService(context, serviceIntent)
        }
    }
}

