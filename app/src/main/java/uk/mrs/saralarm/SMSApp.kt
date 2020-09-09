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
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber
import uk.mrs.saralarm.ActivationNotification.notify
import uk.mrs.saralarm.ui.settings.deepui.phone_numbers.support.SMSNumberObject
import java.lang.reflect.Type
import java.util.*
import kotlin.collections.HashSet


class SMSApp : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pref: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        if (pref.getBoolean("prefEnabled", true)) {
            val phoneUtil = PhoneNumberUtil.getInstance()
            val phoneNumberSet = HashSet<PhoneNumber>()

            val type: Type = object : TypeToken<ArrayList<SMSNumberObject>?>() {}.type
            val fromJson: ArrayList<SMSNumberObject> = Gson().fromJson(pref.getString("SMSNumbersJSON", ""), type)

            val it: Iterator<*> = fromJson.iterator()
            while (it.hasNext()) {
                try {
                    phoneNumberSet.add(phoneUtil.parse((it.next() as SMSNumberObject).phoneNumber, "GB"))
                } catch (e: NumberParseException) {}
            }

            if (VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT ) {
                if (intent.action.equals("android.provider.Telephony.SMS_RECEIVED")) {
                    for (smsMessage in Telephony.Sms.Intents.getMessagesFromIntent(intent)) {
                       checkMessages(pref,smsMessage,phoneNumberSet,phoneUtil,context)
                    }
                }
            }else {
                //get bundle of extras from intent.
                val bundle = intent.extras
                //check whether the bundle is not null
                if (bundle != null) {
                    //if true, get the string of the text message content.
                    val plusObj = (bundle["pdus"] as Array<*>?)!!
                    for (aPlusObj in plusObj) {
                        checkMessages(pref,SmsMessage.createFromPdu(aPlusObj as ByteArray),phoneNumberSet,phoneUtil,context)
                    }
                }
            }
        }
    }

    private fun checkMessages(pref: SharedPreferences, smsMessage : SmsMessage,phoneNumberSet : Set<PhoneNumber>,phoneUtil: PhoneNumberUtil, context: Context){

        val skip: Boolean
        val usePhoneNumber = pref.getBoolean("prefUsePhoneNumber", false)
        val customTrigger = pref.getString("prefUseCustomTrigger", "")

        //if use Custom Trigger Message without phone number
        if (!usePhoneNumber) {
            if (!customTrigger.isNullOrBlank()) {
                if (smsMessage.messageBody.toLowerCase(Locale.getDefault()).replace("\\s+".toRegex(), "")
                        .contains(customTrigger.toLowerCase(Locale.getDefault()).replace("\\s+".toRegex(), ""))
                ) {
                    if (checkScreenState(context)) {
                        ActivationNotification.notifyPostAlarm(context)
                    } else {
                        notify(context)
                    }
                }
            }
        } else {
            skip = if (!customTrigger.isNullOrBlank())
                !smsMessage.messageBody.toLowerCase(Locale.getDefault()).replace("\\s+".toRegex(), "")
                    .contains(customTrigger.toLowerCase(Locale.getDefault()).replace("\\s+".toRegex(), ""))
            else false

            if (!skip) {
                if (checkSMSNumberSet(phoneNumberSet, smsMessage.displayOriginatingAddress, phoneUtil)) {
                    if (checkScreenState(context)) {
                        ActivationNotification.notifyPostAlarm(context)
                    } else {
                        notify(context)
                    }
                }
            }
        }
    }

    private fun checkSMSNumberSet(SS: Set<PhoneNumber>, m: String, phoneUtil: PhoneNumberUtil): Boolean {
        for (s in SS) {
            if (PhoneNumberUtils.compare(phoneUtil.format(s, PhoneNumberFormat.INTERNATIONAL), m)) {
                return true
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
