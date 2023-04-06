/*
 *  Copyright (C) Tyler Simmonds - All Rights Reserved
 *  Unauthorised copying of this file, via any medium is prohibited
 *  Written by Tyler Simmonds on behalf of SARCALL LTD, 2021
 *
 */

package uk.mrs.saralarm.ui.settings.extra_ui.phone_numbers

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import android.text.Editable
import android.text.TextWatcher
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.Phonenumber
import uk.mrs.saralarm.databinding.SettingsSmsNumbersFragmentBinding
import uk.mrs.saralarm.databinding.SettingsSmsNumbersRecyclerViewRowBinding
import uk.mrs.saralarm.ui.settings.extra_ui.support.DragAdapterListener
import uk.mrs.saralarm.ui.settings.extra_ui.support.ItemTouchViewHolder


class SMSNumbersRecyclerViewAdapter(val context: Context,
                                    val data: ArrayList<String>,
                                    val binding: SettingsSmsNumbersFragmentBinding
) : RecyclerView.Adapter<SMSNumbersRecyclerViewAdapter.ViewHolder?>(), DragAdapterListener {

    val phoneUtil: PhoneNumberUtil = PhoneNumberUtil.getInstance()
    var undoSnackBar: Snackbar? = null
    override fun getItemCount(): Int {
        return data.size
    }

    override fun swapItems(fromPosition: Int, toPosition: Int) {
        val original = data[fromPosition]
        data.removeAt(fromPosition)
        data.add(toPosition, original)
        notifyItemMoved(fromPosition, toPosition)
    }

    @SuppressLint("ShowToast")
    override fun removeItems(adapterPosition: Int, allowUndo: Boolean) {
        if (adapterPosition >= 0 && adapterPosition < data.size) {
            if (allowUndo) {
                val temp = data[adapterPosition]

                undoSnackBar = Snackbar.make(binding.root, "Deleted", Snackbar.LENGTH_LONG).apply {
                    setAction("Undo") {
                        data.add(adapterPosition, temp)
                        notifyDataSetChanged()
                    }
                    duration = 9000
                }
                undoSnackBar?.anchorView = binding.smsNumbersFab
                undoSnackBar?.show()
            }
            data.removeAt(adapterPosition)
            notifyItemRemoved(adapterPosition)
        }
    }

    fun addItem() {
        undoSnackBar?.dismiss()
        data.add("")
        notifyItemInserted(data.size)
    }

    fun saveData() {
        val list = ArrayList<String>()
        val it: Iterator<String> = data.iterator()
        while (it.hasNext()) {
            val t = it.next()
            if (t == "") {
                list.add(t)
            }
        }
        data.removeAll(list.toSet())

        val sharedPrefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val editor: SharedPreferences.Editor = sharedPrefs.edit()
        if (data.isEmpty())
            editor.putString("respondSMSNumbersJSON", "")
        else
            editor.putString("respondSMSNumbersJSON", Gson().toJson(data as Any))

        editor.apply()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = SettingsSmsNumbersRecyclerViewRowBinding.inflate(LayoutInflater.from(context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (data[holder.adapterPosition].isBlank()) {
            holder.rowBinding.smsNumbersEditText.setText("")
        } else {
            try {
                if (!phoneUtil.isValidNumber(phoneUtil.parse(data[holder.adapterPosition], "GB"))) {
                    holder.rowBinding.smsNumbersRecyclerTextInput.error = "SMS Number is in the wrong format"
                } else {
                    holder.rowBinding.smsNumbersRecyclerTextInput.error = ""
                }
            } catch (e: NumberParseException) {
                holder.rowBinding.smsNumbersRecyclerTextInput.error = "SMS Number is in the wrong format"
            }
            holder.rowBinding.smsNumbersEditText.setText(data[holder.adapterPosition])
        }
    }


    inner class ViewHolder(val rowBinding: SettingsSmsNumbersRecyclerViewRowBinding) : RecyclerView.ViewHolder(rowBinding.root), ItemTouchViewHolder, View.OnClickListener {

        init {
            rowBinding.apply {
                smsNumbersEditText.inputType = 3
                smsNumbersEditText.maxLines = 1
                smsNumbersEditText.addTextChangedListener(object : TextWatcher {

                    override fun afterTextChanged(editable: Editable) {
                        if (editable.isNotBlank())
                            try {
                                val formattedNumber: Phonenumber.PhoneNumber = phoneUtil.parse(editable.toString(), "GB")
                                if (!phoneUtil.isValidNumber(formattedNumber)) {
                                    smsNumbersRecyclerTextInput.error = "SMS Number is in the wrong format"
                                } else {
                                    smsNumbersEditText.removeTextChangedListener(this)
                                    val prevSelection: Int = smsNumbersEditText.selectionStart
                                    val prevLength: Int = smsNumbersEditText.length()
                                    smsNumbersEditText.setText(phoneUtil.format(formattedNumber, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL))
                                    smsNumbersEditText.setSelection(
                                        if (smsNumbersEditText.length() - prevLength + prevSelection > 0) {
                                            smsNumbersEditText.length() - prevLength + prevSelection
                                        } else {
                                            0
                                        }
                                    )
                                    smsNumbersRecyclerTextInput.error = ""
                                    smsNumbersEditText.addTextChangedListener(this)
                                }
                            } catch (e: NumberParseException) {
                                smsNumbersRecyclerTextInput.error = "SMS Number is in the wrong format"
                            }
                        if (adapterPosition >= 0 && adapterPosition < data.size) {
                            data[adapterPosition] = smsNumbersEditText.text.toString()
                        }
                    }

                    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

                    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
                })
            }
        }

        override fun onItemSelected() {
            val animator = ObjectAnimator.ofFloat(rowBinding.smsRecyclerCardView, "cardElevation", dipToPixels(2.0f), dipToPixels(10.0f))
            animator.interpolator = AccelerateInterpolator()
            animator.start()
        }

        override fun onItemClear() {
            val animator = ObjectAnimator.ofFloat(rowBinding.smsRecyclerCardView, "cardElevation", dipToPixels(10.0f), dipToPixels(2.0f))
            animator.interpolator = AccelerateInterpolator()
            animator.start()
        }

        private fun dipToPixels(dipValue: Float): Float {
            val metrics: DisplayMetrics?
            val resources: Resources = context.resources
            metrics = resources.displayMetrics
            return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dipValue, metrics)
        }

        override fun onClick(v: View?) {}
    }
}