package uk.mrs.saralarm.ui.respond.support

import android.app.PendingIntent
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.telephony.SmsManager
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.preference.PreferenceManager
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import uk.mrs.saralarm.support.SARResponseCode
import uk.mrs.saralarm.ui.respond.support.RespondSMSBroadcastReceiver.Companion.RESPOND_SMS_BROADCAST_RECEIVER_SENT
import java.lang.reflect.Type
import java.util.*

object SMSSender {

    fun sendSMSResponse(context: Context, view: View, sarResponseCode: SARResponseCode, dialog: DialogInterface?, value: Int, message: String?) {
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
                        for (phoneNumber in phoneNumberSet) {
                            sendSMS(context, "SAR A $value $message", phoneUtil.format(phoneUtil.parse(phoneNumber, "GB"), PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL))
                        }
                    } else {
                        for (phoneNumber in phoneNumberSet) {
                            sendSMS(context, "SAR A $message", phoneUtil.format(phoneUtil.parse(phoneNumber, "GB"), PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL))
                        }
                    }
                    Toast.makeText(context, "SAR A sent to SARCALL", Toast.LENGTH_LONG).show()
                    dialog?.cancel()
                }
                SARResponseCode.SAR_L -> {
                    if (value != 0) {
                        for (phoneNumber in phoneNumberSet) {
                            sendSMS(context, "SAR L $value $message", phoneUtil.format(phoneUtil.parse(phoneNumber, "GB"), PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL))
                        }

                    } else {
                        for (phoneNumber in phoneNumberSet) {
                            sendSMS(context, "SAR L $message", phoneUtil.format(phoneUtil.parse(phoneNumber, "GB"), PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL))
                        }
                    }
                    Toast.makeText(context, "SAR L sent to SARCALL", Toast.LENGTH_LONG).show()
                    dialog?.cancel()
                }
                SARResponseCode.SAR_N -> {
                    for (phoneNumber in phoneNumberSet) {
                        sendSMS(context, "SAR N", phoneUtil.format(phoneUtil.parse(phoneNumber, "GB"), PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL))
                    }
                    Toast.makeText(context, "SAR N sent to SARCALL", Toast.LENGTH_LONG).show()
                    dialog?.cancel()
                }
                SARResponseCode.SAR_H -> {
                    for (phoneNumber in phoneNumberSet) {
                        sendSMS(context, "SAR H", phoneUtil.format(phoneUtil.parse(phoneNumber, "GB"), PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL))
                    }
                    Toast.makeText(context, "SAR H sent to SARCALL", Toast.LENGTH_LONG).show()
                    dialog?.cancel()
                }
                SARResponseCode.SIGN_ON -> {
                    for (phoneNumber in phoneNumberSet) {
                        sendSMS(context, "ON $message", phoneUtil.format(phoneUtil.parse(phoneNumber, "GB"), PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL))
                    }
                    Toast.makeText(context, "Sign on SMS sent to SARCALL. Lookout for a confirmation SMS.", Toast.LENGTH_LONG).show()
                    dialog?.cancel()
                }
                SARResponseCode.SIGN_OFF -> {
                    for (phoneNumber in phoneNumberSet) {
                        if (value != 0) {
                            sendSMS(context, "OFF $message $value" + "d", phoneUtil.format(phoneUtil.parse(phoneNumber, "GB"), PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL))
                        } else {
                            sendSMS(context, "OFF $message", phoneUtil.format(phoneUtil.parse(phoneNumber, "GB"), PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL))
                        }
                    }
                    Toast.makeText(context, "Sign off SMS sent to SARCALL. Lookout for a confirmation SMS.", Toast.LENGTH_LONG).show()
                    dialog?.cancel()
                }
            }
        } catch (e7: NumberParseException) {
            Toast.makeText(context, "Failed! SARCALL SMS number is formatted wrong. Please check number in settings.", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            print(e.message)
            Toast.makeText(context, "Unknown error. Please try again or report issue.", Toast.LENGTH_LONG).show()
        }
    }

    private fun sendSMS(context: Context, message: String, number: String) {
        if (ActivityCompat.checkSelfPermission(context, "android.permission.SEND_SMS") ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {

            val sentPendingIntents = ArrayList<PendingIntent>()
            val sentPI: PendingIntent = PendingIntent.getBroadcast(context, 0, Intent(RESPOND_SMS_BROADCAST_RECEIVER_SENT), PendingIntent.FLAG_IMMUTABLE)

            try {

                val sms =// if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
                // SmsManager.getSmsManagerForSubscriptionId(SmsManager.getDefaultSmsSubscriptionId())
                    //} else {
                    SmsManager.getDefault()
                // }
                val mSMSMessage = sms.divideMessage(message)
                for (i in 0 until mSMSMessage.size) {
                    sentPendingIntents.add(i, sentPI)
                }
                sms.sendMultipartTextMessage(number, null, mSMSMessage, sentPendingIntents, null)
            } catch (e: Exception) {
                Toast.makeText(context, "Sending SMS may have failed. Check your SMS App to confirm. Error code: Unknown", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(context, "Please accept the SMS permission, then try sending SMS again.", Toast.LENGTH_LONG).show()
        }
    }

}

