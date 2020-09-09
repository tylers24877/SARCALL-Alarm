package uk.mrs.saralarm.ui.settings.deepui.triggers

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.settings_triggers_fragment.view.*
import uk.mrs.saralarm.R
import java.lang.reflect.Type


class TriggersFragment : Fragment() {
    var adapter : TriggersRecyclerViewAdapter? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val triggerArray: ArrayList<String>

        val json: String? = PreferenceManager.getDefaultSharedPreferences(context).getString("triggersJSON", "")
        if (json.isNullOrBlank()) {
            triggerArray = ArrayList()
            triggerArray.add("")
        } else {
            val type: Type = object : TypeToken<ArrayList<String>?>() {}.type
            triggerArray = Gson().fromJson(json, type)
        }
        val root: View = inflater.inflate(R.layout.settings_triggers_fragment, container, false)

        root.triggersRecyclerView.layoutManager = LinearLayoutManager(context)

        adapter = TriggersRecyclerViewAdapter(requireContext(), triggerArray)

        root.triggersRecyclerView.adapter = adapter

        ItemTouchHelper(TriggersDragAdapter(adapter!!, requireContext(), 3, 12)).attachToRecyclerView(root.triggersRecyclerView)
        root.triggersFab.setOnClickListener{
            adapter!!.addItem()
        }

        return root
    }

    override fun onPause() {
        adapter!!.saveData()
        super.onPause()
    }
}