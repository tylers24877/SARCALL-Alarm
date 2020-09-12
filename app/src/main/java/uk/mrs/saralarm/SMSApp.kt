package uk.mrs.saralarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Build.VERSION
import android.os.PowerManager
import android.provider.Telephony
import android.telephony.PhoneNumberUtils
import android.telephony.SmsMessage
import android.view.Display
import androidx.preference.PreferenceManager
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

            if (VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                if (intent.action.equals("android.provider.Telephony.SMS_RECEIVED")) {
                    for (smsMessage in Telephony.Sms.Intents.getMessagesFromIntent(intent)) {
                        checkMessages(pref, smsMessage, rulesBothSet, rulesSMSSet, rulesPhraseSet, phoneUtil, context)
                    }
                }
            } else {
                //get bundle of extras from intent.
                val bundle = intent.extras
                //check whether the bundle is not null
                if (bundle != null) {
                    //if true, get the string of the text message content.
                    val plusObj = (bundle["pdus"] as Array<*>?)!!
                    for (aPlusObj in plusObj) {
                        checkMessages(pref, SmsMessage.createFromPdu(aPlusObj as ByteArray), rulesBothSet, rulesSMSSet, rulesPhraseSet, phoneUtil, context)
                    }
                }
            }
        }
    }

    private fun checkMessages(
        pref: SharedPreferences,
        smsMessage: SmsMessage,
        rulesBothSet: HashSet<RulesObject>,
        rulesSMSSet: HashSet<RulesObject> = HashSet(),
        rulesPhraseSet: HashSet<RulesObject>,
        phoneUtil: PhoneNumberUtil,
        context: Context
    ) {
        when {
            checkRulesBoth(rulesBothSet, smsMessage.messageBody, smsMessage.originatingAddress.toString(), phoneUtil) -> {
                if (checkScreenState(context)) {
                    ActivationNotification.notifyPostAlarm(context)
                } else {
                    notify(context)
                }
            }
            checkRulesSMSNumber(rulesSMSSet, smsMessage.originatingAddress.toString(), phoneUtil) -> {
                if (checkScreenState(context)) {
                    ActivationNotification.notifyPostAlarm(context)
                } else {
                    notify(context)
                }
            }
            checkRulesPhrase(rulesPhraseSet, smsMessage.messageBody) -> {
                if (checkScreenState(context)) {
                    ActivationNotification.notifyPostAlarm(context)
                } else {
                    notify(context)
                }
            }
        }

    }

    private fun checkRulesPhrase(SS: HashSet<RulesObject>, m: String): Boolean {
        for (s in SS) {
            if (Pattern.compile(s.phrase, Pattern.CASE_INSENSITIVE + Pattern.LITERAL).matcher(m).find()) {
                return true
            }
        }
        return false
    }

    private fun checkRulesSMSNumber(rulesSMSSet: HashSet<RulesObject>, phoneNumberC: String, phoneUtil: PhoneNumberUtil): Boolean {
        for (s in rulesSMSSet) {
            try {
                val formattedNumber: Phonenumber.PhoneNumber = phoneUtil.parse(s.smsNumber, "GB")
                if (PhoneNumberUtils.compare(phoneUtil.format(formattedNumber, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL), phoneNumberC)) {
                    return true
                }
            } catch (e: NumberParseException) {
            }
        }
        return false
    }

    private fun checkRulesBoth(SS: Set<RulesObject>, body: String, receivedNumber: String, phoneUtil: PhoneNumberUtil): Boolean {
        for (s in SS) {
            try {
                if (Pattern.compile(s.phrase, Pattern.CASE_INSENSITIVE + Pattern.LITERAL).matcher(body).find()) {
                    val formattedNumber: Phonenumber.PhoneNumber = phoneUtil.parse(s.smsNumber, "GB")
                    if (PhoneNumberUtils.compare(phoneUtil.format(formattedNumber, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL), receivedNumber)) {
                        return true
                    }
                }
            } catch (e: NumberParseException) {
            }
        }
        return false
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
}
