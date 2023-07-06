/*
 *  Copyright (C) Tyler Simmonds - All Rights Reserved
 *  Unauthorised copying of this file, via any medium is prohibited
 *  Written by Tyler Simmonds on behalf of SARCALL LTD, 2021
 *
 */

package uk.mrs.saralarm.support

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.hardware.display.DisplayManager
import android.telephony.PhoneNumberUtils
import android.telephony.SmsMessage
import android.view.Display
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.google.firebase.crashlytics.FirebaseCrashlytics
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
        val crash = FirebaseCrashlytics.getInstance()
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
                            crash.setCustomKey("RulesChoice", RulesChoice.ALL.name)
                            rulesBothSet.add(r)
                        } catch (e: NumberParseException) {
                            FirebaseCrashlytics.getInstance().recordException(e)
                        }
                    } else if (r.choice == RulesChoice.SMS_NUMBER && r.smsNumber.isNotBlank()) {
                        crash.setCustomKey("RulesChoice", RulesChoice.SMS_NUMBER.name)
                        rulesSMSSet.add(r)
                    } else if (r.choice == RulesChoice.PHRASE && r.phrase.isNotBlank()) {
                        crash.setCustomKey("RulesChoice", RulesChoice.PHRASE.name)
                        rulesPhraseSet.add(r)
                    }
                }
            }

            if (intent.action.equals("android.provider.Telephony.SMS_RECEIVED")) {
                val bundle = intent.extras
                val messages: Array<SmsMessage?>
                var strMessage = ""
                val smsNumber: String
                val format = bundle!!.getString("format")
                // Retrieve the SMS message received.
                val pdus = bundle["pdus"] as Array<*>?
                if (pdus != null) { // Check the Android version.
                    // Fill the msgs array.
                    messages = arrayOfNulls(pdus.size)
                    for (i in messages.indices) {
                        // If Android version M or newer:
                        messages[i] = SmsMessage.createFromPdu(pdus[i] as ByteArray, format)
                        // Build the message to show.
                        strMessage += messages[i]?.messageBody
                    }
                    smsNumber = messages[0]!!.displayOriginatingAddress
                    checkMessages(strMessage, smsNumber, rulesBothSet, rulesSMSSet, rulesPhraseSet, phoneUtil, context)
                }else{
                    crash.recordException(Exception("PDUS is null"))
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
        val crash = FirebaseCrashlytics.getInstance()

        val checkRulesBoth = checkRulesBoth(rulesBothSet, strMessage, smsNumber, phoneUtil)
        if (checkRulesBoth.chosen) {
            if (checkScreenState(context)) {
                startAlarmForegroundService(context, checkRulesBoth)
                crash.log("alarm_started_unlocked")
            } else {
                crash.log("alarm_started_locked")
                startAlarmForegroundService(context, checkRulesBoth)
            }
        } else {
            val checkRulesSMSNumber = checkRulesSMSNumber(rulesSMSSet, smsNumber, phoneUtil, strMessage)
            if (checkRulesSMSNumber.chosen) {
                if (checkScreenState(context)) {
                    startAlarmForegroundService(context, checkRulesSMSNumber)
                    crash.log("alarm_started_unlocked")
                } else {
                    crash.log("alarm_started_locked")

                    startAlarmForegroundService(context, checkRulesSMSNumber)
                }
            } else {
                val checkRulesPhrase = checkRulesPhrase(rulesPhraseSet, strMessage, smsNumber)
                if (checkRulesPhrase.chosen) {
                    if (checkScreenState(context)) {
                        startAlarmForegroundService(context, checkRulesPhrase)
                        crash.log("alarm_started_unlocked")
                    } else {
                        crash.log("alarm_started_locked")
                        startAlarmForegroundService(context, checkRulesPhrase)
                    }
                }
            }
        }
    }

    private fun checkRulesPhrase(SS: HashSet<RulesObject>, m: String, num: String): RuleAlarmData {
        for (s in SS) {
            val regex = Regex(Pattern.quote(s.phrase.replace("\\s", "")), RegexOption.IGNORE_CASE)
            if (regex.containsMatchIn(Pattern.quote(m.replace("\\s", "")))) {
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
                FirebaseCrashlytics.getInstance().recordException(e)
            }
        }
        return RuleAlarmData(false)
    }

    private fun checkRulesBoth(SS: Set<RulesObject>, body: String, receivedNumber: String, phoneUtil: PhoneNumberUtil): RuleAlarmData {
        for (s in SS) {
            try {
                val regex = Regex(Pattern.quote(s.phrase.replace("\\s", "")), RegexOption.IGNORE_CASE)
                if (regex.containsMatchIn(Pattern.quote(body.replace("\\s", "")))) {
                    val formattedNumber: Phonenumber.PhoneNumber = phoneUtil.parse(s.smsNumber, "GB")
                    if (PhoneNumberUtils.compare(phoneUtil.format(formattedNumber, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL), receivedNumber)) {
                        return RuleAlarmData(
                            true, s.customAlarmRulesObject.alarmSoundType, s.customAlarmRulesObject.alarmFileLocation, s.customAlarmRulesObject.isLooping,
                            s.customAlarmRulesObject.colorArray, body, receivedNumber
                        )
                    }
                }
            } catch (e: NumberParseException) {
                FirebaseCrashlytics.getInstance().recordException(e)
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
            FirebaseCrashlytics.getInstance().log("Starting Foreground service")
            val serviceIntent = Intent(context, AlarmForegroundNotification::class.java)
            serviceIntent.putExtra("ruleAlarmData", ruleAlarmData)
            ContextCompat.startForegroundService(context, serviceIntent)
        }
    }
}

