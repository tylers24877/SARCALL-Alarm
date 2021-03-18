package uk.mrs.saralarm

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.preference.PreferenceManager
import kotlinx.android.synthetic.main.activity_setup.*
import kotlin.jvm.internal.Intrinsics


class SetupActivity : AppCompatActivity() {

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //Loads the layout XML for this Activity and sets the layout as the current view.
        setContentView(R.layout.activity_setup)

        //Sets the SetupToolbar ID in the layout XML as the ActionBar for this activity. This tells the android API where the toolbar is.
        setSupportActionBar(setup_toolbar)

        //Setup OnClick Listener for the setup button. When clicked...
        setup_permission_button.setOnClickListener {
            //Check if the user has the necessary permissions
            if (Build.VERSION.SDK_INT < 23 || ActivityCompat.checkSelfPermission(this, "android.permission.RECEIVE_SMS") == 0
                && ActivityCompat.checkSelfPermission(this, "android.permission.SEND_SMS") == 0 &&
                ActivityCompat.checkSelfPermission(this, "android.permission.READ_SMS") == 0 &&
                ActivityCompat.checkSelfPermission(this, "android.permission.READ_EXTERNAL_STORAGE") == 0
            )
                checkOverlayAndMoveOn() //if permissions already granted, go straight to checking overlay.
            else
                requestPermissions(
                    arrayOf(
                        "android.permission.RECEIVE_SMS",
                        "android.permission.READ_SMS",
                        "android.permission.SEND_SMS",
                        "android.permission.READ_EXTERNAL_STORAGE"
                    ), 1
                ) //if permissions not granted, request them with request code of 1.
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        //if request code of 1 is received
        if (requestCode == 1) {
            for (permission in permissions) {
                if (Intrinsics.areEqual(permission as Any, "android.permission.READ_SMS" as Any)) {
                    //Check overlay
                    checkOverlayAndMoveOn()
                }
            }
        }
    }

    /**
     * Check if app is allowed to draw on top of other apps. If it cannot, permission from the user will be requested.
     * Once granted, Move onto checking battery optimisation.
     */
    @SuppressLint("InlinedApi")
    private fun checkOverlayAndMoveOn() {
        if (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                !Settings.canDrawOverlays(this)
            } else false
        ) {
            val dialogClickListener: DialogInterface.OnClickListener = DialogInterface.OnClickListener { _, which ->
                when (which) {
                    //If Positive button pressed.
                    DialogInterface.BUTTON_POSITIVE -> {
                        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                        //Start an activity to display the permission settings to enable to draw on top.
                        startActivityForResult(intent, 44)
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent()
            val packageName = packageName
            val systemService = getSystemService(Context.POWER_SERVICE)
            if (systemService == null) {
                throw NullPointerException("null cannot be cast to non-null type android.os.PowerManager")
            } else if (!(systemService as PowerManager).isIgnoringBatteryOptimizations(packageName)) {
                intent.action = "android.settings.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS"
                intent.data = Uri.parse("package:$packageName")
                startActivityForResult(intent, 2)
                return
            }
        }
        //Start app if already granted.
        startApp()
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        //If request code of 2 (from checkBattery())
        if (requestCode == 2) {
            //Start the app
            startApp()
        }
        //If request code of 44 (from checkOverlayAndMoveOn())
        if (requestCode == 44) {
            if (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Settings.canDrawOverlays(this)
                } else false
            ) {
                checkBattery()
            } else {
                checkOverlayAndMoveOn()
            }
        }
    }

    /**
     * Set startedBefore Preference to true, start the MainActivity and close current activity.
     */
    private fun startApp() {
        PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("startedBefore", true).apply()
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}