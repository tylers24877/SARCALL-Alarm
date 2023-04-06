/*
 *  Copyright (C) Tyler Simmonds - All Rights Reserved
 *  Unauthorised copying of this file, via any medium is prohibited
 *  Written by Tyler Simmonds on behalf of SARCALL LTD, 2021
 *
 */

package uk.mrs.saralarm.ui.respond.support

import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.os.Build
import android.provider.Telephony
import android.telephony.PhoneNumberUtils
import android.telephony.SmsManager
import android.widget.Toast
import androidx.preference.PreferenceManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.Phonenumber
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import uk.mrs.saralarm.R
import uk.mrs.saralarm.ui.settings.extra_ui.rules.support.RulesChoice
import uk.mrs.saralarm.ui.settings.extra_ui.rules.support.RulesObject
import java.lang.reflect.Type
import java.text.DateFormat
import java.util.*
import java.util.regex.Pattern

object RespondUtil {

    @DelicateCoroutinesApi
    fun setPreviewAsync(context: Context): Deferred<Pair<String, String>> {
        return GlobalScope.async {
            val pref = PreferenceManager.getDefaultSharedPreferences(context)
            val phoneUtil = PhoneNumberUtil.getInstance()
            val typeRuleObject: Type = object : TypeToken<ArrayList<RulesObject>?>() {}.type
            val rulesFromJson: ArrayList<RulesObject>? = Gson().fromJson(pref.getString("rulesJSON", ""), typeRuleObject)

            val rulesBothSet: HashSet<RulesObject> = HashSet()
            val rulesSMSSet: HashSet<RulesObject> = HashSet()
            val rulesPhraseSet: HashSet<RulesObject> = HashSet()

            var setBodyAndDate: Pair<String, String> = Pair("No Messages", "")

            if (rulesFromJson.isNullOrEmpty()) {
                Pair(context.getString(R.string.fragment_respond_preview_setup_needed_warning), "")
            } else {
                for (r in rulesFromJson) {
                    if (r.choice == RulesChoice.ALL && r.smsNumber.isNotBlank() && r.phrase.isNotBlank()) {
                        try {
                            rulesBothSet.add(r)
                        } catch (_: NumberParseException) {
                        }
                    } else if (r.choice == RulesChoice.SMS_NUMBER && r.smsNumber.isNotBlank()) {
                        rulesSMSSet.add(r)
                    } else if (r.choice == RulesChoice.PHRASE && r.phrase.isNotBlank()) {
                        rulesPhraseSet.add(r)
                    }
                }
                val projection = arrayOf(Telephony.Sms.Inbox.TYPE, Telephony.Sms.Inbox.DATE, Telephony.Sms.Inbox.BODY, Telephony.Sms.Inbox.ADDRESS) //specify columns you want to return
                val c: Cursor? = context.contentResolver.query(Telephony.Sms.Inbox.CONTENT_URI, projection, null, null, Telephony.Sms.Inbox.DEFAULT_SORT_ORDER)
                if (c != null) {

                    if (c.moveToFirst()) {
                        work@ do {
                            if (c.getString(c.getColumnIndexOrThrow(Telephony.Sms.Inbox.TYPE)).toInt() == 1) {

                                try {
                                    val smsDate: String = c.getString(c.getColumnIndexOrThrow(Telephony.Sms.Inbox.DATE))
                                    val body: String = c.getString(c.getColumnIndexOrThrow(Telephony.Sms.Inbox.BODY))
                                    val phoneNumberC: String = c.getString(c.getColumnIndexOrThrow(Telephony.Sms.Inbox.ADDRESS))
                                    val date = Date(smsDate.toLong())

                                    if (checkRulesBoth(rulesBothSet, body, phoneNumberC, phoneUtil)) {
                                        setBodyAndDate = Pair(body, DateFormat.getDateTimeInstance().format(date))
                                        break@work
                                    } else if (checkRulesSMSNumber(rulesSMSSet, phoneNumberC, phoneUtil)) {
                                        setBodyAndDate = Pair(body, DateFormat.getDateTimeInstance().format(date))
                                        break@work
                                    } else if (checkRulesPhrase(rulesPhraseSet, body)) {
                                        setBodyAndDate = Pair(body, DateFormat.getDateTimeInstance().format(date))
                                        break@work
                                    }
                                } catch (e: Exception) {
                                    setBodyAndDate = Pair("An error occurred at: " + c.position, "")
                                    break@work
                                }

                            }

                        } while (c.moveToNext())
                    }
                    c.close()
                }
                setBodyAndDate
            }
        }
    }

    private fun checkRulesPhrase(SS: HashSet<RulesObject>, m: String): Boolean {
        for (s in SS) {
            if (Pattern.compile(s.phrase.replace("\\s".toRegex(), ""), Pattern.CASE_INSENSITIVE + Pattern.LITERAL).matcher(m.replace("\\s".toRegex(), "")).find()) {
                return true
            }
        }
        return false
    }

    private val parsedNumbersCache = HashMap<String, Phonenumber.PhoneNumber>()
    private fun checkRulesSMSNumber(rulesSMSSet: Set<RulesObject>, phoneNumberC: String, phoneUtil: PhoneNumberUtil): Boolean {
        for (s in rulesSMSSet) {
            val smsNumber = s.smsNumber
            val parsedNumber: Phonenumber.PhoneNumber = (parsedNumbersCache[smsNumber] ?: try {
                val parsedNumber = phoneUtil.parse(smsNumber, "GB")
                parsedNumbersCache[smsNumber] = parsedNumber
                parsedNumber
            } catch (_: NumberParseException) {
            }) as Phonenumber.PhoneNumber
            parsedNumber.let {
                val formattedNumber = phoneUtil.format(it, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (PhoneNumberUtils.areSamePhoneNumber(formattedNumber, phoneNumberC,"gb")) {
                        return true
                    }
                } else if (PhoneNumberUtils.compare(formattedNumber, phoneNumberC)) {
                    return true
                }
            }
        }
        return false
    }


    private fun checkRulesBoth(SS: Set<RulesObject>, body: String, receivedNumber: String, phoneUtil: PhoneNumberUtil): Boolean {
        for (s in SS) {
            try {
                if (Pattern.compile(s.phrase.replace("\\s".toRegex(), ""), Pattern.CASE_INSENSITIVE + Pattern.LITERAL).matcher(body.replace("\\s".toRegex(), "")).find()) {
                    val formattedNumber: Phonenumber.PhoneNumber = phoneUtil.parse(s.smsNumber, "GB")
                    if (PhoneNumberUtils.compare(phoneUtil.format(formattedNumber, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL), receivedNumber)) {
                        return true
                    }
                }
            } catch (_: NumberParseException) {
            }
        }
        return false
    }
}

class RespondSMSBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(arg0: Context, arg1: Intent?) {
        when (resultCode) {
            Activity.RESULT_OK -> Toast.makeText(arg0, "SMS sent", Toast.LENGTH_SHORT).show()

            SmsManager.RESULT_ERROR_GENERIC_FAILURE -> alertSMSFailed(arg0, "Generic failure")

            SmsManager.RESULT_ERROR_NO_SERVICE -> alertSMSFailed(arg0, "No service")

            SmsManager.RESULT_ERROR_NULL_PDU -> alertSMSFailed(arg0, "PDU error")

            SmsManager.RESULT_ERROR_RADIO_OFF -> alertSMSFailed(arg0, "Radio off")
        }
    }

    private fun alertSMSFailed(mContext: Context, resultCode: String) {
        Toast.makeText(mContext, "Sending SMS may have failed. Check your SMS App to confirm. Error code: $resultCode", Toast.LENGTH_LONG).show()
    }

    companion object {
        const val RESPOND_SMS_BROADCAST_RECEIVER_SENT = "uk.mrs.saralarm.ui.respond.support.sms_sent"
    }
}

