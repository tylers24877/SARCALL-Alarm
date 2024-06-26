/*
 *  Copyright (C) Tyler Simmonds - All Rights Reserved
 *  Unauthorised copying of this file, via any medium is prohibited
 *  Written by Tyler Simmonds on behalf of SARCALL LTD, 2021
 *
 */

package uk.mrs.saralarm.ui.respond.dialogs

import android.content.Context
import android.content.DialogInterface
import android.view.View
import androidx.appcompat.app.AlertDialog
import uk.mrs.saralarm.R
import uk.mrs.saralarm.support.SARResponseCode
import uk.mrs.saralarm.ui.respond.support.SMSSender.sendSMSResponse

object DialogSARH {
    fun open(context: Context, view: View) {
        val dialogClickListener = DialogInterface.OnClickListener { dialog, which ->
            if (which == DialogInterface.BUTTON_POSITIVE) {
                sendSMSResponse(context, view, SARResponseCode.SAR_H, dialog, 0, null)
            }
        }
        AlertDialog.Builder(context).setMessage(context.getString(R.string.fragment_respond_dialog_sar_h_message)).setPositiveButton(R.string.fragment_respond_sar_n_positive, dialogClickListener)
            .setNegativeButton(R.string.fragment_respond_sar_n_negative, dialogClickListener).show()
    }
}