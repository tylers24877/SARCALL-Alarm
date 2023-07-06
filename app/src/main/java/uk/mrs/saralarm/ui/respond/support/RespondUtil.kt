/*
 *  Copyright (C) Tyler Simmonds - All Rights Reserved
 *  Unauthorised copying of this file, via any medium is prohibited
 *  Written by Tyler Simmonds on behalf of SARCALL LTD, 2021
 *
 */

package uk.mrs.saralarm.ui.respond.support

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
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.Phonenumber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import uk.mrs.saralarm.ui.settings.extra_ui.rules.support.RulesChoice
import uk.mrs.saralarm.ui.settings.extra_ui.rules.support.RulesObject
import java.lang.reflect.Type
import java.text.DateFormat
import java.util.*
import java.util.regex.Pattern

object RespondUtil {

    suspend fun setPreviewFlow(context: Context): Flow<Pair<String, String>> = flow {
        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        val phoneUtil = PhoneNumberUtil.getInstance()
        val typeRuleObject: Type = object : TypeToken<ArrayList<RulesObject>?>() {}.type
        val rulesFromJson: ArrayList<RulesObject>? = Gson().fromJson(pref.getString("rulesJSON", ""), typeRuleObject)

        val rulesBothSet: HashSet<RulesObject> = HashSet()
        val rulesSMSSet: HashSet<RulesObject> = HashSet()
        val rulesPhraseSet: HashSet<RulesObject> = HashSet()

        var setBodyAndDate: Pair<String, String> = Pair("No Messages", "")

        if (!rulesFromJson.isNullOrEmpty()) {
            for (r in rulesFromJson) {
                when (r.choice) {
                    RulesChoice.ALL -> {
                        if (r.smsNumber.isNotBlank() && r.phrase.isNotBlank()) {
                            try {
                                rulesBothSet.add(r)
                            } catch (e: NumberParseException) {
                                FirebaseCrashlytics.getInstance().recordException(e)
                            }
                        }
                    }
                    RulesChoice.SMS_NUMBER -> {
                        if (r.smsNumber.isNotBlank()) {
                            rulesSMSSet.add(r)
                        }
                    }
                    RulesChoice.PHRASE -> {
                        if (r.phrase.isNotBlank()) {
                            rulesPhraseSet.add(r)
                        }
                    }
                }
            }
            val projection = arrayOf(
                Telephony.Sms.Inbox.TYPE,
                Telephony.Sms.Inbox.DATE,
                Telephony.Sms.Inbox.BODY,
                Telephony.Sms.Inbox.ADDRESS
            )
            val cursor: Cursor? = withContext(Dispatchers.IO) {
                context.contentResolver.query(
                    Telephony.Sms.Inbox.CONTENT_URI,
                    projection,
                    null,
                    null,
                    Telephony.Sms.Inbox.DEFAULT_SORT_ORDER
                )
            }
            cursor?.use {
                if (it.moveToFirst()) {
                    do {
                        if (it.getString(it.getColumnIndexOrThrow(Telephony.Sms.Inbox.TYPE)).toInt() == 1) {
                            try {
                                val smsDate: String =
                                    it.getString(it.getColumnIndexOrThrow(Telephony.Sms.Inbox.DATE))
                                val body: String =
                                    it.getString(it.getColumnIndexOrThrow(Telephony.Sms.Inbox.BODY))
                                val phoneNumberC: String =
                                    it.getString(it.getColumnIndexOrThrow(Telephony.Sms.Inbox.ADDRESS))
                                val date = Date(smsDate.toLong())

                                when {
                                    checkRulesBoth(rulesBothSet, body, phoneNumberC, phoneUtil) -> {
                                        setBodyAndDate =
                                            Pair(body, DateFormat.getDateTimeInstance().format(date))
                                        emit(setBodyAndDate)
                                        return@flow
                                    }
                                    checkRulesSMSNumber(rulesSMSSet, phoneNumberC, phoneUtil) -> {
                                        setBodyAndDate =
                                            Pair(body, DateFormat.getDateTimeInstance().format(date))
                                        emit(setBodyAndDate)
                                        return@flow
                                    }
                                    checkRulesPhrase(rulesPhraseSet, body) -> {
                                        setBodyAndDate =
                                            Pair(body, DateFormat.getDateTimeInstance().format(date))
                                        emit(setBodyAndDate)
                                        return@flow
                                    }
                                }
                            } catch (e: Exception) {
                                FirebaseCrashlytics.getInstance().recordException(e)
                                setBodyAndDate =
                                    Pair("An error occurred at: " + it.position, "")
                                emit(setBodyAndDate)
                                return@flow
                            }
                        }
                    } while (it.moveToNext())
                }
            }
        }

        emit(setBodyAndDate)
    }.flowOn(Dispatchers.Default)


    private fun checkRulesPhrase(rulesSet: Set<RulesObject>, message: String): Boolean {
        for (rule in rulesSet) {
            val regex = Regex(Pattern.quote(rule.phrase.replace("\\s", "")), RegexOption.IGNORE_CASE)
            if (regex.containsMatchIn(Pattern.quote(message.replace("\\s", "")))) {
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
            } catch (e: NumberParseException) {
                FirebaseCrashlytics.getInstance().recordException(e)
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
                val regex = Regex(Pattern.quote(s.phrase.replace("\\s", "")), RegexOption.IGNORE_CASE)
                if (regex.containsMatchIn(Pattern.quote(body.replace("\\s", "")))) {
                    val formattedNumber: Phonenumber.PhoneNumber = phoneUtil.parse(s.smsNumber, "GB")
                    if (PhoneNumberUtils.compare(phoneUtil.format(formattedNumber, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL), receivedNumber)) {
                        return true
                    }
                }
            } catch (e: NumberParseException) {
                FirebaseCrashlytics.getInstance().recordException(e)
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

