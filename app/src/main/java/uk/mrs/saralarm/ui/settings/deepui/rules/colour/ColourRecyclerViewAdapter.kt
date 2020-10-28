package uk.mrs.saralarm.ui.settings.deepui.rules.colour

import android.animation.ObjectAnimator
import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.graphics.Color.parseColor
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import androidx.recyclerview.widget.RecyclerView
import com.flask.colorpicker.ColorPickerView
import com.flask.colorpicker.builder.ColorPickerDialogBuilder
import com.google.i18n.phonenumbers.PhoneNumberUtil
import kotlinx.android.synthetic.main.colour_recycler_view_row.view.*
import uk.mrs.saralarm.R
import uk.mrs.saralarm.support.ItemTouchViewHolder


class ColourRecyclerViewAdapter(context: Context, data: ArrayList<String>) : RecyclerView.Adapter<ColourRecyclerViewAdapter.ViewHolder?>() {
    var mContext: Context = context
    private val mData: ArrayList<String> = data
    private val mInflater: LayoutInflater = LayoutInflater.from(context)
    val phoneUtil: PhoneNumberUtil = PhoneNumberUtil.getInstance()

    var onSave: ((ArrayList<String>) -> Unit)? = null

    override fun getItemCount(): Int {
        return mData.size
    }

    fun swapItems(fromPosition: Int, toPosition: Int) {
        val original = mData[fromPosition]
        mData.removeAt(fromPosition)
        mData.add(toPosition, original)
        notifyItemMoved(fromPosition, toPosition)
    }

    fun removeItems(adapterPosition: Int) {

        if (adapterPosition >= 0 && adapterPosition < mData.size) {
            mData.removeAt(adapterPosition)
            notifyItemRemoved(adapterPosition)
        }
    }

    fun addItem(showPicker: Boolean) {
        mData.add("")
        if (showPicker) {
            setColourUsingPicker(true, mData.size - 1, "")
        }
        notifyItemInserted(mData.size - 1)
    }

    fun saveData() {
        val list = ArrayList<String>()
        val it: Iterator<String> = mData.iterator()
        while (it.hasNext()) {
            val t = it.next()
            if (t == "") {
                list.add(t)
            }
        }
        mData.removeAll(list)

        onSave?.invoke(mData)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view: View = mInflater.inflate(R.layout.colour_recycler_view_row, parent, false)
        return ViewHolder(this, view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.itemView.color_recycler_text_view.text = mData[holder.layoutPosition]
        try {
            holder.itemView.colour_recycler_cardview.setCardBackgroundColor(parseColor(mData[holder.layoutPosition]))
        } catch (e: Exception) {
            when (e) {
                is IllegalArgumentException, is StringIndexOutOfBoundsException -> {
                    holder.itemView.colour_recycler_cardview.setCardBackgroundColor(Color.WHITE)
                }
                else -> throw e
            }
        }
    }

    private fun setColourUsingPicker(removeOnCancel: Boolean, location: Int, iColour: String) {

        val colorPickerDialogBuilder = ColorPickerDialogBuilder
            .with(mContext)
            .setTitle("Choose Colour")
            .wheelType(ColorPickerView.WHEEL_TYPE.CIRCLE)
            .density(24)
            .lightnessSliderOnly()
            .showColorPreview(true)
            .setPositiveButton("ok") { _, selectedColor, _ ->
                mData[location] = "#" + Integer.toHexString(selectedColor)
                notifyItemChanged(location)
            }
            .setNegativeButton("cancel") { _, which ->
                if (removeOnCancel) removeItems(location)
            }
        try {
            if (iColour.isNotBlank())
                colorPickerDialogBuilder.initialColor(parseColor(iColour))
        } catch (e: Exception) {
            when (e) {
                is IllegalArgumentException, is StringIndexOutOfBoundsException -> {
                }
                else -> throw e
            }
        }

        val d = colorPickerDialogBuilder.build()

        d.setOnCancelListener {
            if (removeOnCancel) removeItems(location)
        }
        d.show()
    }

    inner class ViewHolder(v: ColourRecyclerViewAdapter, itemView: View) : RecyclerView.ViewHolder(itemView), ItemTouchViewHolder, View.OnClickListener {

        init {
            itemView.colour_recycler_delete_image_view.setOnClickListener {
                removeItems(adapterPosition)
            }
            itemView.colour_recycler_edit_image_view.setOnClickListener {
                setColourUsingPicker(false, adapterPosition, mData[adapterPosition])
            }
        }

        override fun onItemSelected() {
            val animator = ObjectAnimator.ofFloat(itemView.colour_recycler_cardview, "cardElevation", dipToPixels(2.0f), dipToPixels(10.0f))
            animator.interpolator = AccelerateInterpolator()
            animator.start()
        }

        override fun onItemClear() {
            val animator = ObjectAnimator.ofFloat(itemView.colour_recycler_cardview, "cardElevation", dipToPixels(10.0f), dipToPixels(2.0f))
            animator.interpolator = AccelerateInterpolator()
            animator.start()
        }

        private fun dipToPixels(dipValue: Float): Float {
            val metrics: DisplayMetrics?
            val resources: Resources = mContext.resources
            metrics = resources.displayMetrics
            return TypedValue.applyDimension(1, dipValue, metrics)
        }

        override fun onClick(v: View?) {}
    }
}