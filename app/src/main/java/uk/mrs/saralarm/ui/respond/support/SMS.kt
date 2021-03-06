package uk.mrs.saralarm.ui.respond.support

import android.content.Context
import android.content.DialogInterface
import android.telephony.SmsManager
import android.view.View
import android.widget.Toast
import androidx.preference.PreferenceManager
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import uk.mrs.saralarm.support.SARResponseCode
import java.lang.reflect.Type
import java.util.*

object SMS {

    fun sendSMSResponse(context: Context, view: View, sarResponseCode: SARResponseCode, dialog: DialogInterface?, value: Int, message: String?) {
        val sb: StringBuilder
        val smsManager: SmsManager = SmsManager.getDefault()
        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        val phoneUtil = PhoneNumberUtil.getInstance()
        try {
            val type: Type = object : TypeToken<ArrayList<String>?>() {}.type
            val phoneNumberSet: ArrayList<String>? = Gson().fromJson(pref.getString("respondSMSNumbersJSON", ""), type)
            if (phoneNumberSet.isNullOrEmpty()) {
                Snackbar.make(view, "Failed! No SARCALL number chosen. Please check/add number in settings.", Snackbar.LENGTH_LONG).show()
                dialog?.cancel()
                return
            }
            when (sarResponseCode) {
                SARResponseCode.SAR_A -> {
                    if (value != 0) {
                        sb = StringBuilder()
                        sb.append("SAR A")
                        sb.append(value)
                        sb.append(' ')
                        sb.append(message)
                        for (phoneNumber in phoneNumberSet) {
                            smsManager.sendTextMessage(phoneUtil.format(phoneUtil.parse(phoneNumber, "GB"), PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL), null, sb.toString(), null, null)
                        }
                    } else {
                        sb = StringBuilder()
                        sb.append("SAR A  ")
                        sb.append(message)
                        for (phoneNumber in phoneNumberSet) {
                            smsManager.sendTextMessage(phoneUtil.format(phoneUtil.parse(phoneNumber, "GB"), PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL), null, sb.toString(), null, null)
                        }
                    }
                    Toast.makeText(context, "SAR A sent to SARCALL", Toast.LENGTH_LONG).show()
                    dialog?.cancel()
                }
                SARResponseCode.SAR_L -> {
                    if (value != 0) {
                        sb = StringBuilder()
                        sb.append("SAR L")
                        sb.append(value)
                        sb.append(' ')
                        sb.append(message)
                        for (phoneNumber in phoneNumberSet) {
                            smsManager.sendTextMessage(phoneUtil.format(phoneUtil.parse(phoneNumber, "GB"), PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL), null, sb.toString(), null, null)
                        }

                    } else {
                        sb = StringBuilder()
                        sb.append("SAR L ")
                        sb.append(message)
                        for (phoneNumber in phoneNumberSet) {
                            smsManager.sendTextMessage(phoneUtil.format(phoneUtil.parse(phoneNumber, "GB"), PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL), null, sb.toString(), null, null)
                        }
                    }

                    Toast.makeText(context, "SAR L sent to SARCALL", Toast.LENGTH_LONG).show()
                    dialog?.cancel()
                }
                SARResponseCode.SAR_N -> {
                    for (phoneNumber in phoneNumberSet) {
                        smsManager.sendTextMessage(phoneUtil.format(phoneUtil.parse(phoneNumber, "GB"), PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL), null, "SAR N", null, null)
                    }
                    Toast.makeText(context, "SAR N sent to SARCALL", Toast.LENGTH_LONG).show()
                    dialog?.cancel()
                }
                SARResponseCode.SAR_H -> {
                    for (phoneNumber in phoneNumberSet) {
                        smsManager.sendTextMessage(phoneUtil.format(phoneUtil.parse(phoneNumber, "GB"), PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL), null, "SAR H", null, null)
                    }
                    Toast.makeText(context, "SAR H sent to SARCALL", Toast.LENGTH_LONG).show()
                    dialog?.cancel()
                }
                SARResponseCode.SIGN_ON -> {
                    for (phoneNumber in phoneNumberSet) {
                        smsManager.sendTextMessage(phoneUtil.format(phoneUtil.parse(phoneNumber, "GB"), PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL), null, "ON $message", null, null)
                    }
                    Toast.makeText(context, "Sign on SMS sent to SARCALL. Lookout for a confirmation SMS.", Toast.LENGTH_LONG).show()
                    dialog?.cancel()
                }
                SARResponseCode.SIGN_OFF -> {
                    for (phoneNumber in phoneNumberSet) {
                        if (value != 0) {
                            smsManager.sendTextMessage(
                                phoneUtil.format(phoneUtil.parse(phoneNumber, "GB"), PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL), null, "OFF $message $value" + "d",
                                null, null
                            )
                        } else {
                            smsManager.sendTextMessage(
                                phoneUtil.format(phoneUtil.parse(phoneNumber, "GB"), PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL), null, "OFF $message",
                                null, null
                            )
                        }
                    }
                    Toast.makeText(context, "Sign off SMS sent to SARCALL. Lookout for a confirmation SMS.", Toast.LENGTH_LONG).show()
                    dialog?.cancel()
                }
            }
        } catch (e7: NumberParseException) {
            Toast.makeText(context, "Failed! SARCALL SMS number is formatted wrong. Please check number in settings.", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Unknown error. Please try again or report issue.", Toast.LENGTH_LONG).show()
            FirebaseCrashlytics.getInstance().recordException(e)
        }
    }

}