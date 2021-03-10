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
import kotlinx.android.synthetic.main.activity_main.*
import uk.mrs.saralarm.support.UpdateWorker
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity() {

    @SuppressLint("InlinedApi")
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //Load preferences
        val pref = PreferenceManager.getDefaultSharedPreferences(this)


        //Check if application has NOT been used before.
        //If true, starts the SetupActivity for user to configure permissions on. It will also kill this activity.
        //If false, continue.
        if (!pref.getBoolean("startedBefore", false)) {
            startActivity(Intent(this, SetupActivity::class.java))
            finish()
            return
        }

        //Loads the layout XML for this Activity and sets the layout as the current view.
        setContentView(R.layout.activity_main)

        //Sets the ResponseToolbar ID in the layout XML as the ActionBar for this activity. This tells the android API where the toolbar is.
        setSupportActionBar(response_toolbar)

        //Build the app bar for the activity. The IDs set here are the top level fragments.
        val appBarConfiguration: AppBarConfiguration = AppBarConfiguration.Builder(R.id.navigation_respond, R.id.navigation_settings).build()
        //Load the navigation controller for the fragments from the layout XML.
        val navController: NavController = Navigation.findNavController(this, R.id.nav_host_fragment)
        //Configures the ActionBar set up earlier to work with the Nav Controller. EG changing actionbar titles to match the current page.
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration)
        //Set the BottomNavigationView to the Nav Controller.
        NavigationUI.setupWithNavController(nav_view, navController)

        //Check if what SDK the app is running on. If Version M(23) or higher...
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //Load Package Name.
            val packageName = packageName
            //Load the Power Service from the system.
            val systemService = getSystemService(Context.POWER_SERVICE)
            //Checks it does not exist.
            if (systemService == null) {
                //Throw an error if it doesn't
                throw NullPointerException("null cannot be cast to non-null type android.os.PowerManager")
            //If the service does exist, check if the app is NOT ignoring battery optimizations.
            } else if (!(systemService as PowerManager).isIgnoringBatteryOptimizations(packageName)) {
                //Create a new intent.
                val i = Intent()
                //Set the action of the intent to request to ignore battery optimizations
                i.action = "android.settings.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS"
                i.data = Uri.parse("package:$packageName")
                //Starts request to ask user.
                startActivity(i)
            }
        }

        //Check if alarm is enabled. If true,
        if (pref.getBoolean("prefEnabled", false)) {
            //Check if the Rules are configured. If empty...
            val string = pref.getString("rulesJSON", "")
            if (string!!.isEmpty()) {
                //Set the alarm to disabled.
                pref.edit().putBoolean("prefEnabled", false).apply()
            }
        }

        //Check if app is newly open rather then a OnCreate refresh.
        if (savedInstanceState == null) {
            checkForUpdate(pref)
            createBackgroundJobs()
        }
        checkOverlay()
    }

    /**
     * Create/update background jobs for app updater.
     * Checks 12 hourly-ish
     */
    private fun createBackgroundJobs() {

        //Delete old versions of the update checker.
        WorkManager.getInstance(this).cancelUniqueWork("SARCALL_CHECK_UPDATE_V7") //version 1.5.2 beta
        WorkManager.getInstance(this).cancelUniqueWork("SARCALL_CHECK_UPDATE_V8") //version 1.6.0 beta

        val build: PeriodicWorkRequest = PeriodicWorkRequest.Builder(UpdateWorker::class.java, 24, TimeUnit.HOURS, 30, TimeUnit.MINUTES)
            .addTag("SARCALL_CHECK_UPDATE_V9_TAG").build()
        //Enqueue a task using Work Manager for 12 hourly-ish checks
        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork("SARCALL_CHECK_UPDATE_V9", ExistingPeriodicWorkPolicy.KEEP, build)
    }

    /**
     * Checks for app updates and actions accordingly.
     * @param pref SharedPreferences for the application.
     */
    private fun checkForUpdate(pref: SharedPreferences) {
        //Check if app is set to use beta channel. If true...
        if (pref.getBoolean("betaChannel", false)) {
            //Check for updates on the beta URL @Github
            AppUpdater(this).setUpdateFrom(UpdateFrom.XML).setDisplay(Display.DIALOG)
                .setUpdateXML("https://raw.githubusercontent.com/tylers24877/MRT-SAR-Alarm/master/update_beta.xml")
                .setCancelable(false).start()
        } else {
            //Check for updates on the stable URL @Github
            AppUpdater(this).setUpdateFrom(UpdateFrom.XML).setDisplay(Display.DIALOG)
                .setUpdateXML("https://raw.githubusercontent.com/tylers24877/MRT-SAR-Alarm/master/update.xml")
                .setCancelable(false).start()
        }
    }

    /**
     * Check if app is allowed to draw on top of other apps. If it cannot, permission from the user will be requested.
     */
    @SuppressLint("InlinedApi", "MissingPermission")
    private fun checkOverlay() {
        //Check if cannot draw on top. If true...
        if (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                !Settings.canDrawOverlays(this)
            } else false
        ) {
            //Create a dialog informing the user.
            val dialogClickListener: DialogInterface.OnClickListener = DialogInterface.OnClickListener { _, which ->
                //When button is clicked...
                when (which) {
                    //Positive
                    DialogInterface.BUTTON_POSITIVE -> {
                        //Start an activity to display the permission settings to enable to draw on top.
                        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                        startActivityForResult(intent, 505)

                        //Log to firebase that the user is accepting.
                        //FirebaseAnalytics.getInstance(applicationContext).logEvent("overlay_accept", null)
                    }
                }
            }

            //Build and display the dialog.
            val builder: AlertDialog.Builder = AlertDialog.Builder(this)
            builder.setMessage(getString(R.string.main_activity_display_on_top_permission_request))
                .setPositiveButton(getString(R.string.main_activity_display_on_top_positive_button), dialogClickListener)
                .setCancelable(false)
                .show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.main_activity_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
            }
            R.id.ActionBar_Help -> {
                Navigation.findNavController(this, R.id.nav_host_fragment).navigate(R.id.action_global_helpFragment)
            }
        }
        return true
    }
    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        //Check if the request code matches the request from the CheckOverlay function
        if (requestCode == 505) {
            //Check if the user accepted the permission. If not start the process again.
            if (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    !Settings.canDrawOverlays(this)
                } else false
            ) {
                checkOverlay()
            }
        }
    }
}