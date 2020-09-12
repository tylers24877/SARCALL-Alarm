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
import uk.mrs.saralarm.ui.settings.deepui.rules.support.RulesChoice
import uk.mrs.saralarm.ui.settings.deepui.rules.support.RulesObject
import java.lang.reflect.Type
import java.text.DateFormat
import java.util.*
import java.util.regex.Pattern
import kotlin.collections.HashSet

class RespondViewModel : ViewModel() {
    private val mEta = MutableLiveData<Int>()
    val eta: LiveData<Int>
        get() = mEta

    fun setEta(eta: Int) {
        mEta.value = Integer.valueOf(eta)
    }

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
                Pair(context.getString(R.string.setup_needed), "")
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

                val c: Cursor? = context.contentResolver.query(Telephony.Sms.CONTENT_URI, null, null, null, null)
                if (c != null) {

                    if (c.moveToFirst()) {
                        work@ do {
                            if (c.getString(c.getColumnIndexOrThrow("type")).toInt() == 1) {
                                val smsDate: String = c.getString(c.getColumnIndexOrThrow("date"))
                                val body: String = c.getString(c.getColumnIndexOrThrow("body"))
                                val phoneNumberC: String = c.getString(c.getColumnIndexOrThrow("address"))
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
}

