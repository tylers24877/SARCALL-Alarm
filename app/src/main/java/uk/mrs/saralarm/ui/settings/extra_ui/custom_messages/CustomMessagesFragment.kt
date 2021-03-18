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
import kotlinx.android.synthetic.main.settings_custom_messages_fragment.view.*
import uk.mrs.saralarm.R
import java.lang.reflect.Type

class CustomMessagesFragment : Fragment() {

    private var adapter : CustomMessagesRecyclerViewAdapter? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val customMessageArray: ArrayList<String>
        val json: String? = PreferenceManager.getDefaultSharedPreferences(context).getString("customMessageJSON", "")
        if (json.isNullOrBlank()) {
            customMessageArray = ArrayList()
            customMessageArray.add("")
        } else {
            val type: Type = object : TypeToken<ArrayList<String>?>() {}.type
            customMessageArray = Gson().fromJson(json, type)
        }
        val root: View = inflater.inflate(R.layout.settings_custom_messages_fragment, container, false)

        root.custom_message_recycler_view.layoutManager = LinearLayoutManager(context)

        adapter = CustomMessagesRecyclerViewAdapter(requireContext(), customMessageArray, root)

        root.custom_message_recycler_view.adapter = adapter

        ItemTouchHelper(CustomMessagesDragAdapter(adapter!!, 3, 12)).attachToRecyclerView(root.custom_message_recycler_view)
        root.custom_message_fab.setOnClickListener {
            adapter!!.addItem()
        }

        setHasOptionsMenu(true)
        exitTransition = MaterialSharedAxis(MaterialSharedAxis.X, /* forward= */ true)
        reenterTransition = MaterialSharedAxis(MaterialSharedAxis.X, /* forward= */ false)
        return root
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
}