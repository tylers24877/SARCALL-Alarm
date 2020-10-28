package uk.mrs.saralarm.ui.respond.dialogs

import android.app.Dialog
import android.content.Context
import android.graphics.Rect
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.SeekBar
import androidx.preference.PreferenceManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.dialog_respond_sar_a.*
import uk.mrs.saralarm.R
import uk.mrs.saralarm.support.SARResponseCode
import uk.mrs.saralarm.ui.respond.support.SMS.sendSMSResponse
import java.lang.reflect.Type
import java.util.*
import kotlin.collections.ArrayList

object SARA {

    fun dialogSARAOpen(context: Context, view: View) {
        val dialog = Dialog(context)
        dialog.setContentView(R.layout.dialog_respond_sar_a)
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
        customMessageArray.add(0, "Enter Custom Message...")
        val adapter = ArrayAdapter(context, android.R.layout.simple_expandable_list_item_1, customMessageArray)
        adapter.setDropDownViewResource(android.R.layout.simple_expandable_list_item_1)

        dialog.sar_a_spinner.adapter = adapter


        dialog.sar_a_spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (dialog.sar_a_spinner.selectedItem.toString() == "Enter Custom Message...") {
                    dialog.respond_dialog_sar_a_message_editview.isEnabled = true
                    dialog.respond_dialog_sar_a_message_inputlayout.visibility = View.VISIBLE
                    dialog.respond_dialog_sar_a_message_title_txtview.visibility = View.VISIBLE
                    return
                }
                dialog.respond_dialog_sar_a_message_editview.isEnabled = false
                dialog.respond_dialog_sar_a_message_inputlayout.visibility = View.GONE
                dialog.respond_dialog_sar_a_message_title_txtview.visibility = View.GONE
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                dialog.respond_dialog_sar_a_message_editview.isEnabled = false
            }
        }
        dialog.respond_dialog_sar_a_constraint_layout.setOnTouchListener { v, event ->
            when (event?.action) {
                MotionEvent.ACTION_DOWN ->
                    if (dialog.respond_dialog_sar_a_message_editview.isFocused) {
                        val outRect = Rect()
                        dialog.respond_dialog_sar_a_message_editview.getGlobalVisibleRect(outRect)
                        if (!outRect.contains(event.rawX.toInt(), event.rawY.toInt())) {
                            dialog.respond_dialog_sar_a_message_editview.clearFocus()
                            val systemService = v.context.getSystemService(Context.INPUT_METHOD_SERVICE)
                            if (systemService != null) {
                                (systemService as InputMethodManager).hideSoftInputFromWindow(v.windowToken, 0)
                            } else {
                                throw NullPointerException("null cannot be cast to non-null type android.view.inputmethod.InputMethodManager")
                            }
                        }
                    }
            }
            view.performClick()
            v?.onTouchEvent(event) ?: true
        }
        dialog.respond_dialog_sar_a_seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                if (dialog.respond_dialog_sar_a_message_editview.isFocused) {
                    dialog.respond_dialog_sar_a_message_editview.clearFocus()
                }
                val systemService = dialog.context.getSystemService(Context.INPUT_METHOD_SERVICE)
                val imm = systemService as InputMethodManager
                imm.hideSoftInputFromWindow(view.windowToken, 0)
                return
            }

            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (progress >= 0 && progress <= seekBar.max) {
                    val progressCal = progress * 5
                    val sb = SpannableStringBuilder()
                    sb.append("Estimated time to arrival: ")
                    sb.append(progressCal.toString())
                    sb.append(" minutes")
                    sb.setSpan(StyleSpan(1), 27, progressCal.toString().length + 27, 18)
                    dialog.respond_dialog_sar_a_seek_eta_txtview.text = sb
                }
            }
        })
        dialog.respond_dialog_sar_a_seek.progress = 5
        dialog.respond_dialog_sar_a_submit_button.setOnClickListener {
            val progressCal = dialog.respond_dialog_sar_a_seek.progress * 5
            if (dialog.sar_a_spinner.selectedItem == "Enter Custom Message...") {
                sendSMSResponse(context, view, SARResponseCode.SAR_A, dialog, progressCal, dialog.respond_dialog_sar_a_message_editview.text.toString())
            } else {
                sendSMSResponse(context, view, SARResponseCode.SAR_A, dialog, progressCal, dialog.sar_a_spinner.selectedItem.toString())
            }
        }
    }
}