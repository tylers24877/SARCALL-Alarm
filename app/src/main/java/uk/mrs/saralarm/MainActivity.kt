package uk.mrs.saralarm

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.preference.PreferenceManager
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.github.javiersantos.appupdater.AppUpdater
import com.github.javiersantos.appupdater.enums.Display
import com.github.javiersantos.appupdater.enums.UpdateFrom
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.activity_main.*
import uk.mrs.saralarm.support.UpdateWorker
import uk.mrs.saralarm.ui.settings.deepui.rules.support.RulesChoice
import uk.mrs.saralarm.ui.settings.deepui.rules.support.RulesObject
import java.lang.reflect.Type
import java.util.concurrent.TimeUnit
import kotlin.jvm.internal.Intrinsics


class MainActivity : AppCompatActivity() {


    @SuppressLint("InlinedApi")
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pref = PreferenceManager.getDefaultSharedPreferences(this)
        if (!pref.getBoolean("startedBefore", false)) {
            startActivity(Intent(this, SetupActivity::class.java))
            finish()
            return
        }
        setContentView(R.layout.activity_main)
        setSupportActionBar(ResponseToolbar)
        val appBarConfiguration: AppBarConfiguration = AppBarConfiguration.Builder(R.id.navigation_respond, R.id.navigation_settings).build()

        val navController: NavController = Navigation.findNavController(this, R.id.nav_host_fragment)

        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration)
        NavigationUI.setupWithNavController(nav_view, navController)
        if (Build.VERSION.SDK_INT >= 23) {
            val intent = Intent()
            val packageName = packageName
            val systemService = getSystemService(Context.POWER_SERVICE)
            if (systemService == null) {
                throw NullPointerException("null cannot be cast to non-null type android.os.PowerManager")
            } else if (!(systemService as PowerManager).isIgnoringBatteryOptimizations(packageName)) {
                intent.action = "android.settings.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS"
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            }
        }
        if (savedInstanceState == null) {
            if (pref.getBoolean("betaChannel", false)) {
                AppUpdater(this).setUpdateFrom(UpdateFrom.XML).setDisplay(Display.DIALOG)
                    .setUpdateXML("https://raw.githubusercontent.com/tylers24877/MRT-SAR-Alarm/master/update_beta.xml")
                    .setCancelable(false).start()
            } else {
                AppUpdater(this).setUpdateFrom(UpdateFrom.XML).setDisplay(Display.DIALOG)
                    .setUpdateXML("https://raw.githubusercontent.com/tylers24877/MRT-SAR-Alarm/master/update.xml")
                    .setCancelable(false).start()
            }
        }
        if (savedInstanceState == null) {
            WorkManager.getInstance(this).cancelUniqueWork("SARCALL_CHECK_UPDATE_V4") //version 1.5.2 beta
            WorkManager.getInstance(this).cancelUniqueWork("SARCALL_CHECK_UPDATE_V5") //version 1.5.2 beta
            WorkManager.getInstance(this).cancelUniqueWork("SARCALL_CHECK_UPDATE_V6") //version 1.5.2 beta

            val build: PeriodicWorkRequest = PeriodicWorkRequest.Builder(UpdateWorker::class.java, 12, TimeUnit.HOURS, 30, TimeUnit.MINUTES)
                .addTag("SARCALL_CHECK_UPDATE_V7_TAG").build()
            WorkManager.getInstance(this)
                .enqueueUniquePeriodicWork("SARCALL_CHECK_UPDATE_V7", ExistingPeriodicWorkPolicy.KEEP, build)
        }
        upgradePreferences(pref)
        checkOverlay()

        if (pref.getBoolean("prefEnabled", false)) {
            val string = pref.getString("rulesJSON", "")
            if (string!!.isEmpty()) {
                pref.edit().putBoolean("prefEnabled", false).apply()
            }
        }
    }

    private fun checkOverlay() {
        if (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                !Settings.canDrawOverlays(this)
            } else false
        ) {
            val dialogClickListener: DialogInterface.OnClickListener = DialogInterface.OnClickListener { _, which ->
                when (which) {
                    DialogInterface.BUTTON_POSITIVE -> {
                        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                        startActivityForResult(intent, 505)
                        FirebaseAnalytics.getInstance(applicationContext).logEvent("overlay_accept", null)
                    }
                }
            }
            val builder: AlertDialog.Builder = AlertDialog.Builder(this)
            builder.setMessage(
                "The 'Display on top' permission is needed for 'SARCALL Alarm' to activate the alarm." +
                        "\nPlease enable the permission on the app settings page."
            )
                .setPositiveButton("Continue Setup", dialogClickListener)
                .setCancelable(false)
                .show()
        }
    }
    private fun upgradePreferences(pref: SharedPreferences) {
        val edit = pref.edit()
        val rulesNewObjectArray: ArrayList<RulesObject>
        val smsNumberNewObjectArray: ArrayList<String>
        val oldSMSNumber = pref.getString("prefSetPhoneNumber", "")
        val oldTrigger = pref.getString("prefUseCustomTrigger", "")

        if (!oldSMSNumber.isNullOrBlank()) {
            val jsonNumber = pref.getString("respondSMSNumbersJSON", "")
            if (jsonNumber.isNullOrBlank()) {
                smsNumberNewObjectArray = ArrayList()
                smsNumberNewObjectArray.add(oldSMSNumber)
            } else {
                val type: Type = object : TypeToken<ArrayList<String>?>() {}.type
                val fromJson: ArrayList<String> = Gson().fromJson(jsonNumber, type)
                fromJson.add(oldSMSNumber)
                smsNumberNewObjectArray = fromJson
            }
            edit.putString("respondSMSNumbersJSON", Gson().toJson(smsNumberNewObjectArray))
        }
        if (!oldSMSNumber.isNullOrBlank() && !oldTrigger.isNullOrBlank()) {
            val json = pref.getString("rulesJSON", "")
            if (json.isNullOrBlank()) {
                rulesNewObjectArray = ArrayList()
                rulesNewObjectArray.add(RulesObject(smsNumber = oldSMSNumber, phrase = oldTrigger))
            } else {
                val type: Type = object : TypeToken<ArrayList<RulesObject>?>() {}.type
                val fromJson: ArrayList<RulesObject> = Gson().fromJson(json, type)
                fromJson.add(RulesObject(smsNumber = oldSMSNumber, phrase = oldTrigger))
                rulesNewObjectArray = fromJson
            }
            edit.putString("rulesJSON", Gson().toJson(rulesNewObjectArray))
                .remove("prefSetPhoneNumber").remove("prefUseCustomTrigger")
        } else if (!oldSMSNumber.isNullOrBlank()) {
            val json = pref.getString("rulesJSON", "")
            if (json.isNullOrBlank()) {
                rulesNewObjectArray = ArrayList()
                rulesNewObjectArray.add(RulesObject(choice = RulesChoice.SMS_NUMBER, smsNumber = oldSMSNumber))
            } else {
                val type: Type = object : TypeToken<ArrayList<RulesObject>?>() {}.type
                val fromJson: ArrayList<RulesObject> = Gson().fromJson(json, type)
                fromJson.add(RulesObject(choice = RulesChoice.SMS_NUMBER, smsNumber = oldSMSNumber))
                rulesNewObjectArray = fromJson
            }
            edit.putString("rulesJSON", Gson().toJson(rulesNewObjectArray)).remove("prefSetPhoneNumber").remove("prefUseCustomTrigger")
        } else if (!oldTrigger.isNullOrBlank()) {
            val json = pref.getString("rulesJSON", "")
            if (json.isNullOrBlank()) {
                rulesNewObjectArray = ArrayList()
                rulesNewObjectArray.add(RulesObject(choice = RulesChoice.PHRASE, phrase = oldTrigger))
            } else {
                val type: Type = object : TypeToken<ArrayList<RulesObject>?>() {}.type
                val fromJson: ArrayList<RulesObject> = Gson().fromJson(json, type)
                fromJson.add(RulesObject(choice = RulesChoice.PHRASE, phrase = oldTrigger))
                rulesNewObjectArray = fromJson
            }
            edit.putString("rulesJSON", Gson().toJson(rulesNewObjectArray)).remove("prefSetPhoneNumber").remove("prefUseCustomTrigger")
        }
        edit.apply()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        Intrinsics.checkNotNullExpressionValue(inflater, "menuInflater")
        inflater.inflate(R.menu.main_activity_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
        }
        return true
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 505) {
            if (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    !Settings.canDrawOverlays(this)
                } else false
            ) {
                checkOverlay()
            }
        }
    }
}