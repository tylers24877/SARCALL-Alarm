package uk.mrs.saralarm.ui.settings.deepui.phone_numbers

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
import kotlinx.android.synthetic.main.settings_sms_numbers_fragment.view.*
import uk.mrs.saralarm.R
import java.lang.reflect.Type


class SMSNumbersFragment : Fragment() {
    var adapter : SMSNumbersRecyclerViewAdapter? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val SMSNumberObjectArray: ArrayList<String>

        val json: String? = PreferenceManager.getDefaultSharedPreferences(context).getString("respondSMSNumbersJSON", "")
        if (json.isNullOrBlank()) {
            SMSNumberObjectArray = ArrayList()
            SMSNumberObjectArray.add("")
        } else {
            val type: Type = object : TypeToken<ArrayList<String>?>() {}.type
            SMSNumberObjectArray = Gson().fromJson(json, type)
        }
        val root: View = inflater.inflate(R.layout.settings_sms_numbers_fragment, container, false)

        root.SMSNumbersRecyclerView.layoutManager = LinearLayoutManager(context)

        adapter = SMSNumbersRecyclerViewAdapter(requireContext(), SMSNumberObjectArray)

        root.SMSNumbersRecyclerView.adapter = adapter

        ItemTouchHelper(SMSNumbersDragAdapter(adapter!!, requireContext(), 3, 12)).attachToRecyclerView(root.SMSNumbersRecyclerView)
        root.SMSNumbersFab.setOnClickListener{
            adapter!!.addItem()
        }

        return root
    }

    override fun onPause() {
        adapter!!.saveData()
        super.onPause()
    }
}