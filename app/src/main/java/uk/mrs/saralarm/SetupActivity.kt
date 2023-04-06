package uk.mrs.saralarm

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.LayoutInflater
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import androidx.core.content.PackageManagerCompat
import androidx.core.content.UnusedAppRestrictionsConstants
import androidx.preference.PreferenceManager
import com.google.common.util.concurrent.ListenableFuture
import uk.mrs.saralarm.databinding.ActivitySetupBinding
import uk.mrs.saralarm.databinding.DialogUnusedAppPermBinding


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
            if (Settings.canDrawOverlays(this)) {
                    checkBattery()
                } else {
                    checkOverlayAndMoveOn()
                }
        }

       onPermissionRequestLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
           checkOverlayAndMoveOn()
        }
        onBatteryCheckLauncher =  registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            checkIfUnusedAppRestrictionsEnabled()
        }
        onUnusedAppLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            startApp()
        }
        //Setup OnClick Listener for the setup button. When clicked...
        binding.setupPermissionButton.setOnClickListener {
            //Check if the user has the necessary permissions
            val permissionsToRequest = arrayOf(
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.SEND_SMS,
                Manifest.permission.READ_SMS,
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.READ_MEDIA_AUDIO
                }else "",
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.POST_NOTIFICATIONS
                }else ""
            )

            val permissionsToRequestNotGranted = permissionsToRequest.filter {
                ContextCompat.checkSelfPermission(
                    this,
                    it
                ) != PackageManager.PERMISSION_GRANTED
            }
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
            !Settings.canDrawOverlays(this)
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
        }else checkIfUnusedAppRestrictionsEnabled()
    }
    private fun checkIfUnusedAppRestrictionsEnabled() {
        val future: ListenableFuture<Int> =
            PackageManagerCompat.getUnusedAppRestrictionsStatus(this)
        future.addListener({
            when (future.get()) {
                // If the user doesn't start your app for a few months, the system will
                // place restrictions on it. See the API_* constants for details.
                UnusedAppRestrictionsConstants.API_30_BACKPORT,
                UnusedAppRestrictionsConstants.API_30 -> {
                    val onDialogClickListener = DialogInterface.OnClickListener { _, which ->
                        if (which == DialogInterface.BUTTON_POSITIVE) {
                            val intent = IntentCompat.createManageUnusedAppRestrictionsIntent(this, packageName)
                            intent.putExtra(Intent.EXTRA_PACKAGE_NAME, packageName)
                            onUnusedAppLauncher.launch(intent)
                        }
                    }
                    val dialog =  AlertDialog.Builder(this)
                    val dialogBinding: DialogUnusedAppPermBinding = DialogUnusedAppPermBinding.inflate(LayoutInflater.from(dialog.context))
                    dialogBinding.imageView.setImageResource(R.drawable.api_30)

                    dialog.setView(dialogBinding.root)
                    dialog.setPositiveButton("Okay, take me to settings",onDialogClickListener)
                    dialog.setNegativeButton("Later...", null)
                    dialog.show()
                }
                UnusedAppRestrictionsConstants.API_31 ->
                {
                    val onDialogClickListener = DialogInterface.OnClickListener { _, which ->
                        if (which == DialogInterface.BUTTON_POSITIVE) {
                            val intent = IntentCompat.createManageUnusedAppRestrictionsIntent(this, packageName)
                            intent.putExtra(Intent.EXTRA_PACKAGE_NAME, packageName)
                            onUnusedAppLauncher.launch(intent)
                        }
                    }
                    val dialog =  AlertDialog.Builder(this)
                    val dialogBinding: DialogUnusedAppPermBinding = DialogUnusedAppPermBinding.inflate(LayoutInflater.from(dialog.context))
                    dialogBinding.imageView.setImageResource(R.drawable.api_31)

                    dialog.setView(dialogBinding.root)
                    dialog.setPositiveButton("Okay, take me to settings",onDialogClickListener)
                    dialog.setNegativeButton("Later...", null)
                    dialog.show()
                }
                else ->{
                    startApp()
                }
            }
        }, ContextCompat.getMainExecutor(this))
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