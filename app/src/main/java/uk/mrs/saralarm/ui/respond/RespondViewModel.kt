package uk.mrs.saralarm.ui.respond

import android.content.Context
import android.database.Cursor
import android.provider.Telephony
import android.telephony.PhoneNumberUtils
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.preference.PreferenceManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.Phonenumber
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import uk.mrs.saralarm.R
import uk.mrs.saralarm.ui.settings.deepui.phone_numbers.support.SMSNumberObject
import java.lang.reflect.Type
import java.text.DateFormat
import java.util.*
import kotlin.collections.HashSet

class RespondViewModel : ViewModel() {
    private val mEta = MutableLiveData<Int>()
    val eta: LiveData<Int>
        get() = mEta

    fun setEta(eta: Int) {
        mEta.value = Integer.valueOf(eta)
    }
    fun setPreviewAsync(context: Context) : Deferred<Pair<String,String>> {
        return GlobalScope.async {
            val pref = PreferenceManager.getDefaultSharedPreferences(context)
            var skip: Boolean
            var skipEmpty = false
            val phoneUtil = PhoneNumberUtil.getInstance()
            val usePhoneNumber = pref.getBoolean("prefUsePhoneNumber", false)
            val customTrigger = pref.getString("prefUseCustomTrigger", "")
            val phoneNumberSet = HashSet<Phonenumber.PhoneNumber>()

            var setBodyDate: Pair<String,String> = Pair("","")

            val type: Type = object : TypeToken<ArrayList<SMSNumberObject>?>() {}.type
            val fromJson: ArrayList<SMSNumberObject>? = Gson().fromJson(pref.getString("SMSNumbersJSON", ""), type)

            if (customTrigger.isNullOrBlank() && !usePhoneNumber) {
                Pair(context.getString(R.string.setup_needed), "")
            }else {

                if (!fromJson.isNullOrEmpty()) {
                    val it: Iterator<*> = fromJson.iterator()
                    while (it.hasNext()) {
                        try {
                            phoneNumberSet.add(phoneUtil.parse((it.next() as SMSNumberObject).phoneNumber, "GB"))
                        } catch (e: NumberParseException) {
                        }
                    }
                }

                val c: Cursor? = context.contentResolver.query(Telephony.Sms.CONTENT_URI, null, null, null, null)
                if (c != null) {

                    if (c.moveToFirst()) {
                        work@ do {
                            if (c.getString(c.getColumnIndexOrThrow("type")).toInt() == 1) {
                                val smsDate: String = c.getString(c.getColumnIndexOrThrow("date"))
                                val body: String = c.getString(c.getColumnIndexOrThrow("body"))
                                val phoneNumberC: String = c.getString(c.getColumnIndexOrThrow("address"))
                                val date = Date(smsDate.toLong())

                                //if use Custom Trigger Message without phone number
                                if (!usePhoneNumber) {
                                    if (!customTrigger.isNullOrBlank()) {
                                        if (body.toLowerCase(Locale.getDefault()).replace("\\s+".toRegex(), "")
                                                .contains(customTrigger.toLowerCase(Locale.getDefault()).replace("\\s+".toRegex(), ""))
                                        ) {
                                            setBodyDate = Pair(body, DateFormat.getDateTimeInstance().format(date))
                                            skipEmpty = true
                                            break@work
                                        }
                                    }
                                } else {
                                    skip = if (!customTrigger.isNullOrBlank())
                                        !body.toLowerCase(Locale.getDefault()).replace("\\s+".toRegex(), "")
                                            .contains(customTrigger.toLowerCase(Locale.getDefault()).replace("\\s+".toRegex(), ""))
                                    else false

                                    if (!skip) {
                                        if (checkSMSNumberSet(phoneNumberSet, phoneNumberC, phoneUtil)) {
                                            setBodyDate = Pair(body, DateFormat.getDateTimeInstance().format(date))
                                            skipEmpty = true
                                            break@work
                                        }
                                    }
                                }
                            }
                        } while (c.moveToNext())
                        if (!skipEmpty) {
                            setBodyDate = Pair("No Messsages", "")
                        }
                    }
                    c.close()
                }
                setBodyDate
            }
        }
    }
    private fun checkSMSNumberSet(SS: Set<Phonenumber.PhoneNumber>, m: String, phoneUtil: PhoneNumberUtil): Boolean {
        for (s in SS) {
            if (PhoneNumberUtils.compare(phoneUtil.format(s, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL), m)) {
                return true
            }
        }
        return false
    }
}
