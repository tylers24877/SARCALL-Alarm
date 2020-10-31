package uk.mrs.saralarm.ui.respond.dialogs

import android.content.Context
import android.content.DialogInterface
import android.view.View
import androidx.appcompat.app.AlertDialog
import uk.mrs.saralarm.R
import uk.mrs.saralarm.support.SARResponseCode
import uk.mrs.saralarm.ui.respond.support.SMS.sendSMSResponse

object DialogSARN {
    fun open(context: Context, view: View) {
        val dialogClickListener = DialogInterface.OnClickListener { dialog, which ->
            if (which == DialogInterface.BUTTON_POSITIVE) {
                sendSMSResponse(context, view, SARResponseCode.SAR_N, dialog, 0, null)
            }
        }
        AlertDialog.Builder(context).setMessage(R.string.SAR_N_dialog_title).setPositiveButton(R.string.sar_n_positive, dialogClickListener)
            .setNegativeButton(R.string.sar_n_negitive, dialogClickListener).show()
    }
}