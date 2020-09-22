package uk.mrs.saralarm.ui.settings.deepui.rules

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.settings_rules_fragment.view.*
import kotlinx.coroutines.*
import uk.mrs.saralarm.R
import uk.mrs.saralarm.ui.settings.deepui.rules.support.RulesObject
import java.io.File
import java.io.FileOutputStream
import java.lang.reflect.Type
import kotlin.coroutines.CoroutineContext
import kotlin.random.Random


class RulesFragment : Fragment(), CoroutineScope {
    private var adapter: RulesRecyclerViewAdapter? = null

    var position: Int = 100
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

        adapter = RulesRecyclerViewAdapter(requireContext(), this, rulesArray)

        root.rulesRecyclerView.adapter = adapter

        ItemTouchHelper(RulesDragAdapter(adapter!!, requireContext(), 3, 12)).attachToRecyclerView(root.rulesRecyclerView)
        root.rulesFab.setOnClickListener {
            adapter!!.addItem()
            FirebaseAnalytics.getInstance(requireContext().applicationContext).logEvent("rules_row_added", null)

        }

        return root
    }

    override fun onPause() {
        adapter!!.saveData()
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineContext[Job]!!.cancel()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 5 && resultCode == Activity.RESULT_OK) {
            if ((data != null) && (data.data != null)) {
                try {
                    val fileName = if (data.data!!.path != null) File(data.data!!.path!!).name else "Alarm_Sound" + Random.nextInt(1000000, 9999999).toString()
                    inputStreamToFile(requireContext(), fileName, data.data!!, requireContext().filesDir)
                    adapter!!.mData[position].customAlarmRulesObject.alarmFileLocation = requireContext().filesDir.toString() + File.separator + fileName
                    adapter!!.mData[position].customAlarmRulesObject.alarmFileName = fileName
                    adapter!!.saveData()
                } catch (e: Exception) {
                    FirebaseCrashlytics.getInstance().recordException(e)
                    Snackbar.make(requireView(), "Something went wrong", Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun inputStreamToFile(context: Context, fileName: String, uri: Uri, destFile: File) = this.launch {
        withContext(Dispatchers.IO) {
            val inputStream = context.contentResolver.openInputStream(uri)
            val output = FileOutputStream(destFile.toString() + File.separator + fileName)
            inputStream?.copyTo(output, 4 * 1024)
            inputStream?.close()
            output.close()
        }
    }
    override val coroutineContext: CoroutineContext =
        Dispatchers.Main + SupervisorJob()

}