package uk.mrs.saralarm

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import uk.mrs.saralarm.databinding.ActivitySetupBinding
import uk.mrs.saralarm.support.Permissions
import uk.mrs.saralarm.support.Util


class SetupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySetupBinding
    private lateinit var onTopPermissionLauncher : ActivityResultLauncher<Intent>
    private lateinit var onBatteryCheckLauncher : ActivityResultLauncher<Intent>
    private lateinit var onPermissionRequestLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var onUnusedAppLauncher: ActivityResultLauncher<Intent>
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySetupBinding.inflate(layoutInflater)
        //Loads the layout XML for this Activity and sets the layout as the current view.
        setContentView(binding.root)

        //Sets the SetupToolbar ID in the layout XML as the ActionBar for this activity. This tells the android API where the toolbar is.
        setSupportActionBar(binding.setupToolbar)

        onTopPermissionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (Util.canDrawOverlays(this)) {
                    checkBattery()
                } else {
                    checkOverlayAndMoveOn()
                }
        }
        onBatteryCheckLauncher =  registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            Permissions.checkIfUnusedAppRestrictionsEnabled(this,onUnusedAppLauncher,::startApp)
        }
        onUnusedAppLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            startApp()
        }
        onPermissionRequestLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            checkOverlayAndMoveOn()
        }

        //Setup OnClick Listener for the setup button. When clicked...
        binding.setupPermissionButton.setOnClickListener {
            //Check if the user has the necessary permissions
            val permissionsToRequestNotGranted = Permissions.checkPermissions(this)
            if (permissionsToRequestNotGranted.isNotEmpty()) {
                onPermissionRequestLauncher.launch(permissionsToRequestNotGranted.toTypedArray())
            }else checkOverlayAndMoveOn()
        }
    }

    /**
     * Check if app is allowed to draw on top of other apps. If it cannot, permission from the user will be requested.
     * Once granted, Move onto checking battery optimisation.
     */
    @SuppressLint("InlinedApi")
    private fun checkOverlayAndMoveOn() {
        if (
            !Util.canDrawOverlays(this)
        ) {
            val dialogClickListener: DialogInterface.OnClickListener = DialogInterface.OnClickListener { _, which ->
                when (which) {
                    //If Positive button pressed.
                    DialogInterface.BUTTON_POSITIVE -> {
                        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                        //Start an activity to display the permission settings to enable to draw on top.
                        onTopPermissionLauncher.launch(intent)
                    }
                    DialogInterface.BUTTON_NEGATIVE -> {
                        //Move onto checking battery.
                        checkBattery()
                    }
                }
            }
            //Build and display the dialog.
            with(AlertDialog.Builder(this)) {
                setMessage(
                    "The 'Display on top' permission is needed for 'SARCALL Alarm' to activate the alarm." +
                            "\nPlease enable the permission on the app settings page."
                )
                setPositiveButton("Okay, take me to settings", dialogClickListener).setCancelable(false)
                setOnCancelListener { checkOverlayAndMoveOn() }
                show()
            }
        } else
            checkBattery() //Move onto checking battery, as overlay granted.
    }

    /**
     * Check if battery optimisation is ignored, if not request it. Request code "2"
     * Once accepted, continue to startApp().
     */
    private fun checkBattery() {
        val intent = Intent()
        val packageName = packageName
        val systemService = getSystemService(Context.POWER_SERVICE)
        if (systemService == null) {
            throw NullPointerException("null cannot be cast to non-null type android.os.PowerManager")
        } else if (!(systemService as PowerManager).isIgnoringBatteryOptimizations(packageName)) {
            intent.action = "android.settings.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS"
            intent.data = Uri.parse("package:$packageName")
           onBatteryCheckLauncher.launch(intent)
            return
        }else Permissions.checkIfUnusedAppRestrictionsEnabled(this,onUnusedAppLauncher,::startApp)
    }


    /**
     * Set startedBefore Preference to true, start the MainActivity and close current activity.
     */
    private fun startApp() {
        PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("startedBefore", true).apply()
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    override fun attachBaseContext(newBase: Context) {

        val config =  newBase.resources.configuration
        if (config.fontScale > 1.00) {
            config.fontScale = 1.00f
        }
        config.densityDpi = newBase.resources.displayMetrics.xdpi.toInt()
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

}