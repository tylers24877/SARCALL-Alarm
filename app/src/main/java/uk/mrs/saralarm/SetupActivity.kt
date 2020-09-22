package uk.mrs.saralarm

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.preference.PreferenceManager
import kotlinx.android.synthetic.main.activity_setup.*
import kotlin.jvm.internal.Intrinsics


class SetupActivity : AppCompatActivity() {

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup)
        setSupportActionBar(SetupToolbar)


        setup_permission_button.setOnClickListener {
            if (Build.VERSION.SDK_INT < 23 || ActivityCompat.checkSelfPermission(this, "android.permission.RECEIVE_SMS") == 0 && ActivityCompat.checkSelfPermission(this,
                    "android.permission.SEND_SMS") == 0 &&
                ActivityCompat.checkSelfPermission(this, "android.permission.READ_SMS") == 0 && ActivityCompat.checkSelfPermission(this,
                    "android.permission.WRITE_EXTERNAL_STORAGE") == 0 && ActivityCompat.checkSelfPermission(this, "android.permission.READ_EXTERNAL_STORAGE") == 0)
            {
                checkOverlay()
            } else {
                requestPermissions(arrayOf("android.permission.RECEIVE_SMS","android.permission.READ_SMS","android.permission.SEND_SMS","android.permission.READ_EXTERNAL_STORAGE",
                        "android.permission.WRITE_EXTERNAL_STORAGE"), 1)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            for (permission in permissions) {
                if (Intrinsics.areEqual(permission as Any, "android.permission.READ_SMS" as Any)) {
                    checkOverlay()
                }
            }
        }
    }

    /* access modifiers changed from: private */
    private fun checkBattery() {
        if (Build.VERSION.SDK_INT >= 23) {
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
        startApp()
    }

    private fun checkOverlay() {
        /*if (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                !Settings.canDrawOverlays(this)
            } else false) {

            val dialogClickListener: DialogInterface.OnClickListener = DialogInterface.OnClickListener { dialog, which ->
                when (which) {
                    DialogInterface.BUTTON_POSITIVE -> {
                        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                        startActivityForResult(intent, 44)
                    }
                    DialogInterface.BUTTON_NEGATIVE -> {
                        checkBattery()
                    }
                }
            }
            val builder: AlertDialog.Builder = AlertDialog.Builder(this)
            builder.setMessage("The 'Display on top' permission is needed to activate the alarm, when the screen is on." +
                    "\nPlease enable the permission in settings")
                .setPositiveButton("Okay, take me to settings", dialogClickListener)
                .setNegativeButton("Maybe later", dialogClickListener)
            builder.setOnCancelListener {
                checkBattery()
            }
            builder.show()
        }
        else*/
        checkBattery()
    }

    /* access modifiers changed from: protected */
    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 2) {
            startApp()
        }
        if (requestCode == 44) {
            checkBattery()
        }
    }

    private fun startApp() {
        PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("startedBefore", true).apply()
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}