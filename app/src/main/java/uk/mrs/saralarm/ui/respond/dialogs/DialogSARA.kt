package uk.mrs.saralarm.ui.respond.dialogs

import android.annotation.SuppressLint
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
import uk.mrs.saralarm.R
import uk.mrs.saralarm.databinding.DialogRespondSarABinding
import uk.mrs.saralarm.support.SARResponseCode
import uk.mrs.saralarm.support.Util
import uk.mrs.saralarm.ui.respond.support.SMSSender.sendSMSResponse
import java.lang.reflect.Type
import java.util.*
import kotlin.collections.ArrayList

object DialogSARA {

    @SuppressLint("ClickableViewAccessibility")
    fun open(context: Context, view: View) {
        val binding: DialogRespondSarABinding = DialogRespondSarABinding.inflate(LayoutInflater.from(context))
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
            sarASpinner.adapter = adapter


            sarASpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    if (sarASpinner.selectedItem.toString() == context.getString(R.string.fragment_respond_dialog_all_custom_message_first_line)) {
                        respondDialogSarAMessageEditView.isEnabled = true
                        respondDialogSarAMessageInputLayout.visibility = View.VISIBLE
                        respondDialogSarAMessageTitleTxtView.visibility = View.VISIBLE
                        return
                    }
                    respondDialogSarAMessageEditView.isEnabled = false
                    respondDialogSarAMessageInputLayout.visibility = View.GONE
                    respondDialogSarAMessageTitleTxtView.visibility = View.GONE
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    respondDialogSarAMessageEditView.isEnabled = false
                }
            }
            respondDialogSarAConstraintLayout.setOnTouchListener { v, event ->
                when (event?.action) {
                    MotionEvent.ACTION_DOWN ->
                        if (respondDialogSarAMessageEditView.isFocused) {
                            val outRect = Rect()
                            respondDialogSarAMessageEditView.getGlobalVisibleRect(outRect)
                            if (!outRect.contains(event.rawX.toInt(), event.rawY.toInt())) {
                                respondDialogSarAMessageEditView.clearFocus()
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
            respondDialogSarASeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

                override fun onStopTrackingTouch(seekBar: SeekBar?) {}

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    if (respondDialogSarAMessageEditView.isFocused) {
                        respondDialogSarAMessageEditView.clearFocus()
                    }
                    val systemService = dialog.context.getSystemService(Context.INPUT_METHOD_SERVICE)
                    val imm = systemService as InputMethodManager
                    imm.hideSoftInputFromWindow(view.windowToken, 0)
                    return
                }

                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    if (progress >= 0 && progress <= seekBar.max) {
                        val progressCal = progress * 5
                        respondDialogSarASeekEtaTxtView.text =
                            Util.fromHtml(context.resources.getQuantityString(R.plurals.fragment_respond_dialog_sar_a_est_time, progressCal, progressCal))
                    }
                }
            })
            respondDialogSarASeek.progress = 5
            respondDialogSarASubmitButton.setOnClickListener {
                val progressCal = respondDialogSarASeek.progress * 5
                if (sarASpinner.selectedItem == context.getString(R.string.fragment_respond_dialog_all_custom_message_first_line)) {
                    sendSMSResponse(context, view, SARResponseCode.SAR_A, dialog, progressCal, respondDialogSarAMessageEditView.text.toString())
                } else {
                    sendSMSResponse(context, view, SARResponseCode.SAR_A, dialog, progressCal, sarASpinner.selectedItem.toString())
                }
            }
        }
    }
}