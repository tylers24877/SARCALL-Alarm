package uk.mrs.saralarm.ui.respond.dialogs

import android.content.Context
import android.content.DialogInterface
import android.view.View
import androidx.appcompat.app.AlertDialog
import uk.mrs.saralarm.R
import uk.mrs.saralarm.support.SARResponseCode
import uk.mrs.saralarm.ui.respond.support.SMSSender.sendSMSResponse

object DialogSARN {
    fun open(context: Context, view: View) {
        val dialogClickListener = DialogInterface.OnClickListener { dialog, which ->
            if (which == DialogInterface.BUTTON_POSITIVE) {
                sendSMSResponse(context, view, SARResponseCode.SAR_N, dialog, 0, null)
            }
        }
        AlertDialog.Builder(context).setMessage(R.string.fragment_respond_sar_n_dialog_title).setPositiveButton(R.string.fragment_respond_sar_n_positive, dialogClickListener)
            .setNegativeButton(R.string.fragment_respond_sar_n_negative, dialogClickListener).show()
    }
}