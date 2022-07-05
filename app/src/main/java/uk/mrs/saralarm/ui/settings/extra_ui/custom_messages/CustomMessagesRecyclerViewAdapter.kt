/*
 *  Copyright (C) Tyler Simmonds - All Rights Reserved
 *  Unauthorised copying of this file, via any medium is prohibited
 *  Written by Tyler Simmonds on behalf of SARCALL LTD, 2021
 *
 */

package uk.mrs.saralarm.ui.settings.extra_ui.custom_messages

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
import uk.mrs.saralarm.databinding.SettingsCustomMessagesFragmentBinding
import uk.mrs.saralarm.databinding.SettingsCustomMessagesRecyclerViewRowBinding
import uk.mrs.saralarm.ui.settings.extra_ui.support.DragAdapterListener
import uk.mrs.saralarm.ui.settings.extra_ui.support.ItemTouchViewHolder


class CustomMessagesRecyclerViewAdapter(val context: Context,
                                        val data: ArrayList<String>,
                                        val binding: SettingsCustomMessagesFragmentBinding
) : RecyclerView.Adapter<CustomMessagesRecyclerViewAdapter.ViewHolder?>(), DragAdapterListener {

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
                undoSnackBar?.anchorView = binding.customMessageFab
                undoSnackBar?.show()
            }
            data.removeAt(adapterPosition)
            notifyItemRemoved(adapterPosition)
        }
    }

    fun addItem() {
        undoSnackBar?.dismiss()
        data.add("")
        notifyDataSetChanged()
    }

    fun saveData() {
        //mData.removeAll(Collections.singleton(""))

        val sharedPrefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val editor: SharedPreferences.Editor = sharedPrefs.edit()
        if (data.isEmpty())
            editor.putString("customMessageJSON", "")
        else
            editor.putString("customMessageJSON", Gson().toJson(data))
        editor.apply()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = SettingsCustomMessagesRecyclerViewRowBinding.inflate(LayoutInflater.from(context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (data[holder.layoutPosition].isBlank()) {
            holder.rowBinding.customMessageEditText.setText("")
            holder.rowBinding.customMessageEditText.hint = "Type Here..."
        } else {
            holder.rowBinding.customMessageEditText.setText(data[holder.layoutPosition])
        }
    }

    inner class ViewHolder(val rowBinding: SettingsCustomMessagesRecyclerViewRowBinding) : RecyclerView.ViewHolder(rowBinding.root), ItemTouchViewHolder, View.OnClickListener {

        init {
            rowBinding.customMessageEditText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

                }

                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

                }

                override fun afterTextChanged(s: Editable) {

                    data[adapterPosition] = rowBinding.customMessageEditText.text.toString()

                }
            })
        }

        override fun onItemSelected() {
            val animator = ObjectAnimator.ofFloat(rowBinding.customMessageRecyclerCardView, "cardElevation", dipToPixels(2.0f), dipToPixels(10.0f))
            animator.interpolator = AccelerateInterpolator()
            animator.start()
        }

        override fun onItemClear() {
            val animator = ObjectAnimator.ofFloat(rowBinding.customMessageRecyclerCardView, "cardElevation", dipToPixels(10.0f), dipToPixels(2.0f))
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