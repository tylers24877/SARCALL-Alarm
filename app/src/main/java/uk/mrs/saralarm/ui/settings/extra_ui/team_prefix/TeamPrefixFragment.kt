/*
 *  Copyright (C) Tyler Simmonds - All Rights Reserved
 *  Unauthorised copying of this file, via any medium is prohibited
 *  Written by Tyler Simmonds on behalf of SARCALL LTD, 2021
 *
 */

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
import uk.mrs.saralarm.R
import uk.mrs.saralarm.databinding.SettingsTeamPrefixFragmentBinding
import uk.mrs.saralarm.ui.settings.extra_ui.support.DragAdapter
import java.lang.reflect.Type


class TeamPrefixFragment : Fragment() {
    private var _binding: SettingsTeamPrefixFragmentBinding? = null
    val binding get() = _binding!!

    private var adapter: TeamPrefixRecyclerViewAdapter? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = SettingsTeamPrefixFragmentBinding.inflate(inflater, container, false)
        val teamPrefixObjectArray: ArrayList<String>

        val json: String? = context?.let { PreferenceManager.getDefaultSharedPreferences(it).getString("respondTeamPrefixJSON", "") }
        if (json.isNullOrBlank()) {
            teamPrefixObjectArray = ArrayList()
            teamPrefixObjectArray.add("")
        } else {
            val type: Type = object : TypeToken<ArrayList<String>?>() {}.type
            teamPrefixObjectArray = Gson().fromJson(json, type)
        }
        binding.apply {

            teamPrefixRecyclerView.layoutManager = LinearLayoutManager(context)

            setHasOptionsMenu(true)

            exitTransition = MaterialSharedAxis(MaterialSharedAxis.X, /* forward = */ true)
            reenterTransition = MaterialSharedAxis(MaterialSharedAxis.X, /* forward = */ false)

            adapter = TeamPrefixRecyclerViewAdapter(requireContext(), teamPrefixObjectArray, this)

            teamPrefixRecyclerView.adapter = adapter

            ItemTouchHelper(DragAdapter(adapter!!, requireContext(), 3, 12)).attachToRecyclerView(teamPrefixRecyclerView)
            teamPrefixFab.setOnClickListener {
                adapter!!.addItem()
            }
        }
        return binding.root
    }

    override fun onPause() {
        adapter!!.undoSnackBar?.dismiss()
        adapter!!.saveData()
        super.onPause()
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        // You can hide the state of the menu item here if you call getActivity().supportInvalidateOptionsMenu(); somewhere in your code
        val menuItem: MenuItem = menu.findItem(R.id.action_bar_settings)
        menuItem.isVisible = false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}