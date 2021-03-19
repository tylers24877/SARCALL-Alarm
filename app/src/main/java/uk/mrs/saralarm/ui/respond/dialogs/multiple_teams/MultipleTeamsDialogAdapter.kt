package uk.mrs.saralarm.ui.respond.dialogs.multiple_teams

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import uk.mrs.saralarm.databinding.DialogRespondSignOnOffMulitpleTeamsBinding
import uk.mrs.saralarm.databinding.DialogRespondSignOnOffMultipleTeamsRowBinding
import java.util.*

class MultipleTeamsDialogAdapter(val context: Context,
                                 val data: ArrayList<Pair<Boolean, String>>, val dialogBinding: DialogRespondSignOnOffMulitpleTeamsBinding
) : RecyclerView.Adapter<MultipleTeamsDialogAdapter.ViewHolder?>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = DialogRespondSignOnOffMultipleTeamsRowBinding.inflate(LayoutInflater.from(context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.rowBinding.multipleTeamsRowCheckbox.isChecked = data[holder.layoutPosition].first
        holder.rowBinding.multipleTeamsRowCheckbox.text = data[holder.layoutPosition].second
    }

    inner class ViewHolder(val rowBinding: DialogRespondSignOnOffMultipleTeamsRowBinding) : RecyclerView.ViewHolder(rowBinding.root) {
        init {
            rowBinding.multipleTeamsRowCheckbox.setOnCheckedChangeListener { _, isChecked ->
                data[adapterPosition] = Pair(isChecked, rowBinding.multipleTeamsRowCheckbox.text.toString())
                var checked = false
                for (eachPair in data) {
                    if (eachPair.first) checked = true
                }
                dialogBinding.respondDialogSignOnOffMultipleTeamsSelectButton.isEnabled = checked
            }
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }
}