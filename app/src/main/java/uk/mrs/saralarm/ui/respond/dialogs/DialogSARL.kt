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
import uk.mrs.saralarm.databinding.DialogRespondSarLBinding
import uk.mrs.saralarm.support.SARResponseCode
import uk.mrs.saralarm.support.Util
import uk.mrs.saralarm.ui.respond.support.SMSSender.sendSMSResponse
import java.lang.reflect.Type
import java.util.*
import kotlin.collections.ArrayList

object DialogSARL {
    @SuppressLint("ClickableViewAccessibility")
    fun open(context: Context, view: View) {
        val binding: DialogRespondSarLBinding = DialogRespondSarLBinding.inflate(LayoutInflater.from(context))

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
            sarLSpinner.adapter = adapter

            sarLSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    if (sarLSpinner.selectedItem.toString() == context.getString(R.string.fragment_respond_dialog_all_custom_message_first_line)) {
                        respondDialogSarLMessageEditView.isEnabled = true
                        respondDialogSarLMessageInputLayout.visibility = View.VISIBLE
                        respondDialogSarLMessageTitleTxtView.visibility = View.VISIBLE
                        return
                    }
                    respondDialogSarLMessageEditView.isEnabled = false
                    respondDialogSarLMessageInputLayout.visibility = View.GONE
                    respondDialogSarLMessageTitleTxtView.visibility = View.GONE
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    respondDialogSarLMessageEditView.isEnabled = false
                }
            }
            respondDialogSarLConstraintLayout.setOnTouchListener { v, event ->
                when (event?.action) {
                    MotionEvent.ACTION_DOWN ->
                        if (respondDialogSarLMessageEditView.isFocused) {
                            val outRect = Rect()
                            respondDialogSarLMessageEditView.getGlobalVisibleRect(outRect)
                            if (!outRect.contains(event.rawX.toInt(), event.rawY.toInt())) {
                                respondDialogSarLMessageEditView.clearFocus()
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

            respondDialogSarLSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

                override fun onStopTrackingTouch(seekBar: SeekBar?) {}

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    if (respondDialogSarLMessageEditView.isFocused) {
                        respondDialogSarLMessageEditView.clearFocus()
                    }
                    val systemService = dialog.context.getSystemService(Context.INPUT_METHOD_SERVICE)
                    val imm = systemService as InputMethodManager
                    imm.hideSoftInputFromWindow(view.windowToken, 0)
                    return
                }

                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    if (progress >= 0 && progress <= seekBar.max) {
                        val progressCal = progress * 5
                        respondDialogSarLSeekEtaTxtView.text =
                            Util.fromHtml(context.resources.getQuantityString(R.plurals.fragment_respond_dialog_sar_l_est_time, progressCal, progressCal))
                    }
                }
            })
            respondDialogSarLSeek.progress = 5
            respondDialogSarLSubmitButton.setOnClickListener {
                val progressCal = respondDialogSarLSeek.progress * 5
                if (sarLSpinner.selectedItem == context.getString(R.string.fragment_respond_dialog_all_custom_message_first_line)) {
                    sendSMSResponse(context, view, SARResponseCode.SAR_L, dialog, progressCal, respondDialogSarLMessageEditView.text.toString())
                } else {
                    sendSMSResponse(context, view, SARResponseCode.SAR_L, dialog, progressCal, sarLSpinner.selectedItem.toString())
                }
            }
        }

    }
}