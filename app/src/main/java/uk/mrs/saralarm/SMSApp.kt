package uk.mrs.saralarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Build.VERSION
import android.os.PowerManager
import android.telephony.PhoneNumberUtils
import android.telephony.SmsMessage
import android.view.Display
import androidx.preference.PreferenceManager
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.Phonenumber
import uk.mrs.saralarm.ActivationNotification.notify
import uk.mrs.saralarm.ui.settings.deepui.rules.support.RulesChoice
import uk.mrs.saralarm.ui.settings.deepui.rules.support.RulesObject
import java.lang.reflect.Type
import java.util.*
import java.util.regex.Pattern
import kotlin.collections.HashSet


class SMSApp : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pref: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        if (pref.getBoolean("prefEnabled", true)) {
            val phoneUtil = PhoneNumberUtil.getInstance()

            val typeRuleObject: Type = object : TypeToken<ArrayList<RulesObject>?>() {}.type
            val rulesFromJson: ArrayList<RulesObject>? = Gson().fromJson(pref.getString("rulesJSON", ""), typeRuleObject)

            val rulesBothSet: HashSet<RulesObject> = HashSet()
            val rulesSMSSet: HashSet<RulesObject> = HashSet()
            val rulesPhraseSet: HashSet<RulesObject> = HashSet()

            if (rulesFromJson.isNullOrEmpty()) {
                return
            } else {
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
                val msgs: Array<SmsMessage?>
                var strMessage = ""
                var smsNumber = ""
                val format = bundle!!.getString("format")
                // Retrieve the SMS message received.
                val pdus = bundle["pdus"] as Array<*>?
                if (pdus != null) { // Check the Android version.
                    // Fill the msgs array.
                    msgs = arrayOfNulls(pdus.size)
                    for (i in msgs.indices) {
                        if (VERSION.SDK_INT >= Build.VERSION_CODES.M) { // If Android version M or newer:
                            msgs[i] = SmsMessage.createFromPdu(pdus[i] as ByteArray, format)
                        } else { // If Android version L or older:
                            msgs[i] = SmsMessage.createFromPdu(pdus[i] as ByteArray)
                        }
                        // Build the message to show.
                        strMessage += msgs[i]?.messageBody
                    }
                    smsNumber = msgs[0]!!.displayOriginatingAddress
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
        if (checkRulesBoth.first) {
            if (checkScreenState(context)) {
                ActivationNotification.notifyPostAlarm(context)
                FirebaseAnalytics.getInstance(context.applicationContext).logEvent("alarm_started_unlocked", null)

            } else {
                notify(context, checkRulesBoth.second, strMessage, smsNumber, checkRulesBoth.third)
            }
        } else {
            val checkRulesSMSNumber = checkRulesSMSNumber(rulesSMSSet, smsNumber, phoneUtil)
            if (checkRulesSMSNumber.first) {
                if (checkScreenState(context)) {
                    ActivationNotification.notifyPostAlarm(context)
                    FirebaseAnalytics.getInstance(context.applicationContext).logEvent("alarm_started_unlocked", null)


                } else {
                    notify(context, checkRulesSMSNumber.second, strMessage, smsNumber, checkRulesSMSNumber.third)
                }
            } else {
                val checkRulesPhrase = checkRulesPhrase(rulesPhraseSet, strMessage)
                if (checkRulesPhrase.first) {
                    if (checkScreenState(context)) {
                        ActivationNotification.notifyPostAlarm(context)
                        FirebaseAnalytics.getInstance(context.applicationContext).logEvent("alarm_started_unlocked", null)

                    } else {
                        notify(context, checkRulesPhrase.second, strMessage, smsNumber, checkRulesPhrase.third)
                    }
                }
            }
        }
    }
}

private fun checkRulesPhrase(SS: HashSet<RulesObject>, m: String): Triple<Boolean, String, Boolean> {
    for (s in SS) {
        if (Pattern.compile(s.phrase, Pattern.CASE_INSENSITIVE + Pattern.LITERAL).matcher(m).find()) {
            return Triple(true, s.customAlarmRulesObject.alarmFileLocation, s.customAlarmRulesObject.isLooping)
        }
    }
    return Triple(false, "", true)
}

private fun checkRulesSMSNumber(rulesSMSSet: HashSet<RulesObject>, phoneNumberC: String, phoneUtil: PhoneNumberUtil): Triple<Boolean, String, Boolean> {
    for (s in rulesSMSSet) {
        try {
            val formattedNumber: Phonenumber.PhoneNumber = phoneUtil.parse(s.smsNumber, "GB")
            if (PhoneNumberUtils.compare(phoneUtil.format(formattedNumber, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL), phoneNumberC)) {
                return Triple(true, s.customAlarmRulesObject.alarmFileLocation, s.customAlarmRulesObject.isLooping)
            }
        } catch (e: NumberParseException) {
        }
    }
    return Triple(false, "", true)
}

private fun checkRulesBoth(SS: Set<RulesObject>, body: String, receivedNumber: String, phoneUtil: PhoneNumberUtil): Triple<Boolean, String, Boolean> {
    for (s in SS) {
        try {
            if (Pattern.compile(s.phrase, Pattern.CASE_INSENSITIVE + Pattern.LITERAL).matcher(body).find()) {
                val formattedNumber: Phonenumber.PhoneNumber = phoneUtil.parse(s.smsNumber, "GB")
                if (PhoneNumberUtils.compare(phoneUtil.format(formattedNumber, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL), receivedNumber)) {
                    return Triple(true, s.customAlarmRulesObject.alarmFileLocation, s.customAlarmRulesObject.isLooping)
                }
            }
        } catch (e: NumberParseException) {
        }
    }
    return Triple(false, "", true)
}

private fun checkScreenState(context: Context): Boolean {
    return if (VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
        val dm = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        for (display in dm.displays) {
            if (display.state == Display.STATE_ON) {
                return true
            }
        }
        false
    } else {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        pm.isScreenOn
    }
}

