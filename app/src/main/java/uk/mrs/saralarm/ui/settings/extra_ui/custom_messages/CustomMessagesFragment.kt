package uk.mrs.saralarm.ui.settings.extra_ui.custom_messages

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
import uk.mrs.saralarm.databinding.SettingsCustomMessagesFragmentBinding
import java.lang.reflect.Type

class CustomMessagesFragment : Fragment() {

    private var _binding: SettingsCustomMessagesFragmentBinding? = null
    val binding get() = _binding!!

    private var adapter: CustomMessagesRecyclerViewAdapter? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = SettingsCustomMessagesFragmentBinding.inflate(inflater, container, false)

        val customMessageArray: ArrayList<String>
        val json: String? = PreferenceManager.getDefaultSharedPreferences(context).getString("customMessageJSON", "")
        if (json.isNullOrBlank()) {
            customMessageArray = ArrayList()
            customMessageArray.add("")
        } else {
            val type: Type = object : TypeToken<ArrayList<String>?>() {}.type
            customMessageArray = Gson().fromJson(json, type)
        }
        binding.apply {
            customMessageRecyclerView.layoutManager = LinearLayoutManager(context)

            adapter = CustomMessagesRecyclerViewAdapter(requireContext(), customMessageArray, this)

            customMessageRecyclerView.adapter = adapter

            ItemTouchHelper(CustomMessagesDragAdapter(adapter!!, 3, 12)).attachToRecyclerView(customMessageRecyclerView)
            customMessageFab.setOnClickListener {
                adapter!!.addItem()
            }
        }

        setHasOptionsMenu(true)
        exitTransition = MaterialSharedAxis(MaterialSharedAxis.X, /* forward= */ true)
        reenterTransition = MaterialSharedAxis(MaterialSharedAxis.X, /* forward= */ false)
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