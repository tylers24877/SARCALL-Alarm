package uk.mrs.saralarm.ui.respond.dialogs

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.SharedPreferences
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import uk.mrs.saralarm.R
import uk.mrs.saralarm.databinding.DialogRespondSignOnOffMulitpleTeamsBinding
import uk.mrs.saralarm.databinding.FragmentRespondBinding
import uk.mrs.saralarm.support.SARResponseCode
import uk.mrs.saralarm.ui.respond.dialogs.multiple_teams.MultipleTeamsDialogAdapter
import uk.mrs.saralarm.ui.respond.support.SMSSender
import java.util.*

object DialogSignOn {
    fun open(context: Context, view: View, binding: FragmentRespondBinding) {
        val pref: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val teamPrefixArray: ArrayList<String>? = Gson().fromJson(pref.getString("respondTeamPrefixJSON", ""), object : TypeToken<ArrayList<String>?>() {}.type)
        teamPrefixArray?.removeAll(Collections.singleton(""))
        if (teamPrefixArray.isNullOrEmpty()) {
            Snackbar.make(binding.respondConstraintLayout, context.getString(R.string.fragment_respond_dialog_sign_on_no_team_prefix_toast), Snackbar.LENGTH_LONG).show()
            return
        }
        if (teamPrefixArray.size > 1) {
            val pairedArrayList = ArrayList<Pair<Boolean, String>>()
            for (teamPrefix in teamPrefixArray) {
                pairedArrayList.add(Pair(false, teamPrefix))
            }
            val dialogBinding: DialogRespondSignOnOffMulitpleTeamsBinding =
                DialogRespondSignOnOffMulitpleTeamsBinding.inflate(LayoutInflater.from(context))
            val dialog = Dialog(context)
            dialog.setContentView(dialogBinding.root)
            val window: Window = dialog.window!!
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            window.setGravity(Gravity.CENTER)
            dialog.show()
            dialogBinding.apply {
                respondDialogSignOnOffMultipleTeamsSelectButton.isEnabled = false

                respondDialogSignOnOffMultipleTeamsRecycler.layoutManager = LinearLayoutManager(context)
                val adapter = MultipleTeamsDialogAdapter(context, pairedArrayList, dialogBinding)
                respondDialogSignOnOffMultipleTeamsRecycler.adapter = adapter

                respondDialogSignOnOffMultipleTeamsSelectButton.setOnClickListener {
                    for (eachPair in adapter.data) {
                        if (eachPair.first) {
                            SMSSender.sendSMSResponse(context, view, SARResponseCode.SIGN_ON, dialog, 0, eachPair.second)
                        }
                    }
                }
            }
        } else {
            val onePrefixDialogClickListener = DialogInterface.OnClickListener { _, which ->
                if (which == DialogInterface.BUTTON_POSITIVE) {
                    SMSSender.sendSMSResponse(context, view, SARResponseCode.SIGN_ON, null, 0, teamPrefixArray[0])
                }
            }
            AlertDialog.Builder(context).setTitle(context.getString(R.string.fragment_respond_dialog_sign_on_title))
                .setMessage(context.getString(R.string.fragment_respond_dialog_sign_on_message))
                .setPositiveButton(context.getString(R.string.yes), onePrefixDialogClickListener)
                .setNegativeButton(context.getString(R.string.no), onePrefixDialogClickListener).show()
        }
    }
}