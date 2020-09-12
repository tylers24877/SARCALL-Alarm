package uk.mrs.saralarm.ui.settings.deepui.rules

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
import kotlinx.android.synthetic.main.settings_rules_fragment.view.*
import uk.mrs.saralarm.R
import uk.mrs.saralarm.ui.settings.deepui.rules.support.RulesObject
import java.lang.reflect.Type


class RulesFragment : Fragment() {
    private var adapter: RulesRecyclerViewAdapter? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val rulesArray: ArrayList<RulesObject>

        val json: String? = PreferenceManager.getDefaultSharedPreferences(context).getString("rulesJSON", "")
        if (json.isNullOrBlank()) {
            rulesArray = ArrayList()
            rulesArray.add(RulesObject())
        } else {
            val type: Type = object : TypeToken<ArrayList<RulesObject>?>() {}.type
            rulesArray = Gson().fromJson(json, type)
        }
        val root: View = inflater.inflate(R.layout.settings_rules_fragment, container, false)

        root.rulesRecyclerView.layoutManager = LinearLayoutManager(context)

        adapter = RulesRecyclerViewAdapter(requireContext(), rulesArray)

        root.rulesRecyclerView.adapter = adapter

        ItemTouchHelper(RulesDragAdapter(adapter!!, requireContext(), 3, 12)).attachToRecyclerView(root.rulesRecyclerView)
        root.rulesFab.setOnClickListener {
            adapter!!.addItem()
        }

        return root
    }

    override fun onPause() {
        adapter!!.saveData()
        super.onPause()
    }
}