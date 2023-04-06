/*
 *  Copyright (C) Tyler Simmonds - All Rights Reserved
 *  Unauthorised copying of this file, via any medium is prohibited
 *  Written by Tyler Simmonds on behalf of SARCALL LTD, 2021
 *
 */

package uk.mrs.saralarm.ui.settings.extra_ui.phone_numbers

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
import uk.mrs.saralarm.databinding.SettingsSmsNumbersFragmentBinding
import uk.mrs.saralarm.ui.settings.extra_ui.support.DragAdapter
import java.lang.reflect.Type


class SMSNumbersFragment : Fragment() {
    private var _binding: SettingsSmsNumbersFragmentBinding? = null
    val binding get() = _binding!!

    private var adapter: SMSNumbersRecyclerViewAdapter? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = SettingsSmsNumbersFragmentBinding.inflate(inflater, container, false)

        val smsNumberObjectArray: ArrayList<String>

        val json: String? = context?.let { PreferenceManager.getDefaultSharedPreferences(it).getString("respondSMSNumbersJSON", "") }
        if (json.isNullOrBlank()) {
            smsNumberObjectArray = ArrayList()
            smsNumberObjectArray.add("")
        } else {
            val type: Type = object : TypeToken<ArrayList<String>?>() {}.type
            smsNumberObjectArray = Gson().fromJson(json, type)
        }

        binding.apply {
            smsNumbersRecyclerView.layoutManager = LinearLayoutManager(context)

            adapter = SMSNumbersRecyclerViewAdapter(requireContext(), smsNumberObjectArray, this)

            smsNumbersRecyclerView.adapter = adapter

            ItemTouchHelper(DragAdapter(adapter!!, requireContext(), 3, 12)).attachToRecyclerView(smsNumbersRecyclerView)
            smsNumbersFab.setOnClickListener {
                adapter!!.addItem()
            }

            setHasOptionsMenu(true)
            exitTransition = MaterialSharedAxis(MaterialSharedAxis.X, /* forward = */ true)
            reenterTransition = MaterialSharedAxis(MaterialSharedAxis.X, /* forward = */ false)
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