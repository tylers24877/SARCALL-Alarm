/*
 *  Copyright (C) Tyler Simmonds - All Rights Reserved
 *  Unauthorised copying of this file, via any medium is prohibited
 *  Written by Tyler Simmonds on behalf of SARCALL LTD, 2021
 *
 */

package uk.mrs.saralarm.ui.settings.extra_ui.team_prefix

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import android.text.Editable
import android.text.InputFilter
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
import uk.mrs.saralarm.databinding.SettingsTeamPrefixFragmentBinding
import uk.mrs.saralarm.databinding.SettingsTeamPrefixRecyclerViewRowBinding
import uk.mrs.saralarm.ui.settings.extra_ui.support.DragAdapterListener
import uk.mrs.saralarm.ui.settings.extra_ui.support.ItemTouchViewHolder
import kotlin.jvm.internal.Intrinsics


class TeamPrefixRecyclerViewAdapter(val context: Context,
                                    val data: ArrayList<String>,
                                    val binding: SettingsTeamPrefixFragmentBinding
) : RecyclerView.Adapter<TeamPrefixRecyclerViewAdapter.ViewHolder?>(), DragAdapterListener {

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
                undoSnackBar?.anchorView = binding.teamPrefixFab
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
        data.removeAll(list)

        val sharedPrefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val editor: SharedPreferences.Editor = sharedPrefs.edit()
        if (data.isEmpty())
            editor.putString("respondTeamPrefixJSON", "")
        else
            editor.putString("respondTeamPrefixJSON", Gson().toJson(data))

        editor.apply()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = SettingsTeamPrefixRecyclerViewRowBinding.inflate(LayoutInflater.from(context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (data[holder.adapterPosition].isBlank()) {
            holder.rowBinding.teamPrefixEditText.setText("")
        } else {
            holder.rowBinding.teamPrefixEditText.setText(data[holder.adapterPosition])
        }
    }


    inner class ViewHolder(val rowBinding: SettingsTeamPrefixRecyclerViewRowBinding) : RecyclerView.ViewHolder(rowBinding.root), ItemTouchViewHolder, View.OnClickListener {

        init {
            rowBinding.apply {
                teamPrefixEditText.maxLines = 1
                teamPrefixEditText.filters += InputFilter.AllCaps()
                teamPrefixEditText.addTextChangedListener(object : TextWatcher {

                    override fun afterTextChanged(editable: Editable) {
                        if (adapterPosition >= 0 && adapterPosition < data.size) {
                            data[adapterPosition] = teamPrefixEditText.text.toString()
                        }
                    }

                    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

                    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
                })
                val filter =
                    InputFilter { source, start, end, _, _, _ ->
                        for (i in start until end) {
                            if (Character.isWhitespace(source[i])) {
                                return@InputFilter ""
                            }
                        }
                        null
                    }
                teamPrefixEditText.filters += arrayOf(filter)
            }
        }

        override fun onItemSelected() {
            val animator = ObjectAnimator.ofFloat(rowBinding.teamPrefixCardView, "cardElevation", dipToPixels(2.0f), dipToPixels(10.0f))
            animator.interpolator = AccelerateInterpolator()
            animator.start()
        }

        override fun onItemClear() {
            Intrinsics.checkNotNullExpressionValue(itemView, "itemView")
            val animator = ObjectAnimator.ofFloat(rowBinding.teamPrefixCardView, "cardElevation", dipToPixels(10.0f), dipToPixels(2.0f))
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