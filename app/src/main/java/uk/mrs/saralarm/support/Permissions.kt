package uk.mrs.saralarm.support

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.view.LayoutInflater
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import androidx.core.content.PackageManagerCompat
import androidx.core.content.UnusedAppRestrictionsConstants
import com.google.common.util.concurrent.ListenableFuture
import uk.mrs.saralarm.R
import uk.mrs.saralarm.databinding.DialogUnusedAppPermBinding

object Permissions {

    fun checkPermissions(context: Context): List<String> {
        val permissionsToRequest = arrayOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_SMS,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_AUDIO
            } else Manifest.permission.READ_EXTERNAL_STORAGE,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.POST_NOTIFICATIONS
            } else Manifest.permission.READ_EXTERNAL_STORAGE
        )

        return permissionsToRequest.filter {
            ContextCompat.checkSelfPermission(
                context,
                it
            ) != PackageManager.PERMISSION_GRANTED
        }
    }

    fun checkIfUnusedAppRestrictionsEnabled(
        context: Context,
        onUnusedAppLauncher: ActivityResultLauncher<Intent>,
        startApp: () -> (Unit)
    ) {
        val future: ListenableFuture<Int> = PackageManagerCompat.getUnusedAppRestrictionsStatus(context)
        future.addListener(
            {
                when (future.get()) {
                    // If the user doesn't start your app for a few months, the system will
                    // place restrictions on it. See the API_* constants for details.
                    UnusedAppRestrictionsConstants.API_30_BACKPORT,
                    UnusedAppRestrictionsConstants.API_30 -> {

                        val dialog = AlertDialog.Builder(context)
                        val dialogBinding: DialogUnusedAppPermBinding = DialogUnusedAppPermBinding.inflate(
                            LayoutInflater.from(dialog.context)
                        )
                        dialogBinding.imageView.setImageResource(R.drawable.api_30)

                        dialog.setView(dialogBinding.root)
                        dialog.setPositiveButton("Okay, take me to settings") { _, _ ->
                            onUnusedAppLauncher.launch(
                                IntentCompat.createManageUnusedAppRestrictionsIntent(
                                    context,
                                    context.packageName
                                )
                            )
                        }
                        dialog.setNegativeButton("Later...", null)
                        dialog.show()
                    }
                    UnusedAppRestrictionsConstants.API_31 -> {

                        val dialog = AlertDialog.Builder(context)
                        val dialogBinding: DialogUnusedAppPermBinding = DialogUnusedAppPermBinding.inflate(
                            LayoutInflater.from(dialog.context)
                        )
                        dialogBinding.imageView.setImageResource(R.drawable.api_31)

                        dialog.setView(dialogBinding.root)
                        dialog.setPositiveButton("Okay, take me to settings") { _, _ ->
                            onUnusedAppLauncher.launch(
                                IntentCompat.createManageUnusedAppRestrictionsIntent(
                                    context,
                                    context.packageName
                                )
                            )
                        }
                        dialog.setNegativeButton("Later...", null)
                        dialog.show()
                    }
                    else -> {
                        startApp()
                    }
                }
            },
            ContextCompat.getMainExecutor(context)
        )
    }
}