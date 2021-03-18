package uk.mrs.saralarm.ui.settings.extra_ui.team_prefix

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.transition.MaterialSharedAxis
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.settings_team_prefix_fragment.view.*
import uk.mrs.saralarm.R
import java.lang.reflect.Type


class TeamPrefixFragment : Fragment() {
    private var adapter: TeamPrefixRecyclerViewAdapter? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val teamPrefixObjectArray: ArrayList<String>

        val json: String? = PreferenceManager.getDefaultSharedPreferences(context).getString("respondTeamPrefixJSON", "")
        if (json.isNullOrBlank()) {
            teamPrefixObjectArray = ArrayList()
            teamPrefixObjectArray.add("")
        } else {
            val type: Type = object : TypeToken<ArrayList<String>?>() {}.type
            teamPrefixObjectArray = Gson().fromJson(json, type)
        }
        val root: View = inflater.inflate(R.layout.settings_team_prefix_fragment, container, false)

        root.team_prefix_recycler_view.layoutManager = LinearLayoutManager(context)

        adapter = TeamPrefixRecyclerViewAdapter(requireContext(), teamPrefixObjectArray)

        root.team_prefix_recycler_view.adapter = adapter

        ItemTouchHelper(TeamPrefixDragAdapter(adapter!!, 3, 12)).attachToRecyclerView(root.team_prefix_recycler_view)
        root.team_prefix_fab.setOnClickListener {
            adapter!!.addItem()
        }

        setHasOptionsMenu(true)
        exitTransition = MaterialSharedAxis(MaterialSharedAxis.X, /* forward= */ true)
        reenterTransition = MaterialSharedAxis(MaterialSharedAxis.X, /* forward= */ false)
        return root
    }

    override fun onPause() {
        adapter!!.saveData()
        super.onPause()
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        // You can hide the state of the menu item here if you call getActivity().supportInvalidateOptionsMenu(); somewhere in your code
        val menuItem: MenuItem = menu.findItem(R.id.action_bar_settings)
        menuItem.isVisible = false
    }
}