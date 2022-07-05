/*
 *  Copyright (C) Tyler Simmonds - All Rights Reserved
 *  Unauthorised copying of this file, via any medium is prohibited
 *  Written by Tyler Simmonds on behalf of SARCALL LTD, 2021
 *
 */

package uk.mrs.saralarm.ui.respond.dialogs

import android.app.Dialog
import android.content.Context
import android.graphics.Rect
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.preference.PreferenceManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import uk.mrs.saralarm.R
import uk.mrs.saralarm.databinding.DialogRespondSarNBinding
import uk.mrs.saralarm.support.SARResponseCode
import uk.mrs.saralarm.ui.respond.support.SMSSender.sendSMSResponse
import java.lang.reflect.Type
import java.util.*

object DialogSARN {
    fun open(context: Context, view: View) {
        val binding: DialogRespondSarNBinding = DialogRespondSarNBinding.inflate(LayoutInflater.from(context))

        val dialog = Dialog(context)
        dialog.setContentView(binding.root)
        val window: Window = dialog.window!!
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        window.setGravity(Gravity.CENTER)
        dialog.show()
        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        var customMessageArray = ArrayList<String>()
        val json = pref.getString("customMessageJSON", "")
        if (json!!.isNotEmpty()) {
            val type: Type = object : TypeToken<ArrayList<String>?>() {}.type
            val fromJson: ArrayList<String> = Gson().fromJson(json, type)
            customMessageArray = fromJson
        }
        customMessageArray.removeAll(Collections.singleton(""))
        customMessageArray.add(0, context.getString(R.string.fragment_respond_dialog_all_custom_message_first_line))
        val adapter = ArrayAdapter(context, android.R.layout.simple_expandable_list_item_1, customMessageArray)
        adapter.setDropDownViewResource(android.R.layout.simple_expandable_list_item_1)

        binding.apply {
            sarNSpinner.adapter = adapter

            sarNSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    if (sarNSpinner.selectedItem.toString() == context.getString(R.string.fragment_respond_dialog_all_custom_message_first_line)) {
                        respondDialogSarNMessageEditView.isEnabled = true
                        respondDialogSarNMessageInputLayout.visibility = View.VISIBLE
                        respondDialogSarNMessageTitleTxtView.visibility = View.VISIBLE
                        return
                    }
                    respondDialogSarNMessageEditView.isEnabled = false
                    respondDialogSarNMessageInputLayout.visibility = View.GONE
                    respondDialogSarNMessageTitleTxtView.visibility = View.GONE
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    respondDialogSarNMessageEditView.isEnabled = false
                }
            }
            respondDialogSarNConstraintLayout.setOnTouchListener { v, event ->
                when (event?.action) {
                    MotionEvent.ACTION_DOWN ->
                        if (respondDialogSarNMessageEditView.isFocused) {
                            val outRect = Rect()
                            respondDialogSarNMessageEditView.getGlobalVisibleRect(outRect)
                            if (!outRect.contains(event.rawX.toInt(), event.rawY.toInt())) {
                                respondDialogSarNMessageEditView.clearFocus()
                                val systemService = v.context.getSystemService(Context.INPUT_METHOD_SERVICE)
                                if (systemService != null) {
                                    (systemService as InputMethodManager).hideSoftInputFromWindow(v.windowToken, 0)
                                } else {
                                    throw NullPointerException()
                                }
                            }
                        }
                }
                view.performClick()
                v?.onTouchEvent(event) ?: true
            }
            respondDialogSarNSubmitButton.setOnClickListener {
                if (sarNSpinner.selectedItem == context.getString(R.string.fragment_respond_dialog_all_custom_message_first_line)) {
                    sendSMSResponse(context, view, SARResponseCode.SAR_N, dialog, 0, respondDialogSarNMessageEditView.text.toString())
                } else {
                    sendSMSResponse(context, view, SARResponseCode.SAR_N, dialog, 0, sarNSpinner.selectedItem.toString())
                }
            }
        }

    }
}