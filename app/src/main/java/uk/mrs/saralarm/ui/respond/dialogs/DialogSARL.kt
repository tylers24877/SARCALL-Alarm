package uk.mrs.saralarm.ui.respond.dialogs

import android.app.Dialog
import android.content.Context
import android.graphics.Rect
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.SeekBar
import androidx.preference.PreferenceManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.dialog_respond_sar_l.*
import uk.mrs.saralarm.R
import uk.mrs.saralarm.support.SARResponseCode
import uk.mrs.saralarm.support.Util
import uk.mrs.saralarm.ui.respond.support.SMSSender.sendSMSResponse
import java.lang.reflect.Type
import java.util.*
import kotlin.collections.ArrayList

object DialogSARL {
    fun open(context: Context, view: View) {
        val dialog = Dialog(context)
        dialog.setContentView(R.layout.dialog_respond_sar_l)
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


        dialog.sar_l_spinner.adapter = adapter

        dialog.sar_l_spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (dialog.sar_l_spinner.selectedItem.toString() == context.getString(R.string.fragment_respond_dialog_all_custom_message_first_line)) {
                    dialog.respond_dialog_sar_l_message_edit_view.isEnabled = true
                    dialog.respond_dialog_sar_l_message_input_layout.visibility = View.VISIBLE
                    dialog.respond_dialog_sar_l_message_title_txt_view.visibility = View.VISIBLE
                    return
                }
                dialog.respond_dialog_sar_l_message_edit_view.isEnabled = false
                dialog.respond_dialog_sar_l_message_input_layout.visibility = View.GONE
                dialog.respond_dialog_sar_l_message_title_txt_view.visibility = View.GONE
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                dialog.respond_dialog_sar_l_message_edit_view.isEnabled = false
            }
        }
        dialog.respond_dialog_sar_l_constraint_layout.setOnTouchListener { v, event ->
            when (event?.action) {
                MotionEvent.ACTION_DOWN ->
                    if (dialog.respond_dialog_sar_l_message_edit_view.isFocused) {
                        val outRect = Rect()
                        dialog.respond_dialog_sar_l_message_edit_view.getGlobalVisibleRect(outRect)
                        if (!outRect.contains(event.rawX.toInt(), event.rawY.toInt())) {
                            dialog.respond_dialog_sar_l_message_edit_view.clearFocus()
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

        dialog.respond_dialog_sar_l_seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                if (dialog.respond_dialog_sar_l_message_edit_view.isFocused) {
                    dialog.respond_dialog_sar_l_message_edit_view.clearFocus()
                }
                val systemService = dialog.context.getSystemService(Context.INPUT_METHOD_SERVICE)
                val imm = systemService as InputMethodManager
                imm.hideSoftInputFromWindow(view.windowToken, 0)
                return
            }

            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (progress >= 0 && progress <= seekBar.max) {
                    val progressCal = progress * 5
                    dialog.respond_dialog_sar_l_seek_eta_txt_view.text =
                        Util.fromHtml(context.resources.getQuantityString(R.plurals.fragment_respond_dialog_sar_l_est_time, progressCal, progressCal))
                }
            }
        })
        dialog.respond_dialog_sar_l_seek.progress = 5
        dialog.respond_dialog_sar_l_submit_button.setOnClickListener {
            val progressCal = dialog.respond_dialog_sar_l_seek.progress * 5
            if (dialog.sar_l_spinner.selectedItem == context.getString(R.string.fragment_respond_dialog_all_custom_message_first_line)) {
                sendSMSResponse(context, view, SARResponseCode.SAR_L, dialog, progressCal, dialog.respond_dialog_sar_l_message_edit_view.text.toString())
            } else {
                sendSMSResponse(context, view, SARResponseCode.SAR_L, dialog, progressCal, dialog.sar_l_spinner.selectedItem.toString())
            }
        }

    }
}