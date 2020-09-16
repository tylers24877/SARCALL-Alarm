package uk.mrs.saralarm.ui.settings.deepui.rules

import android.content.ContentResolver
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
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.settings_rules_fragment.view.*
import kotlinx.coroutines.*
import uk.mrs.saralarm.R
import uk.mrs.saralarm.ui.settings.deepui.rules.support.RulesObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.lang.reflect.Type
import kotlin.coroutines.CoroutineContext


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
        if (data != null && requestCode == 5 && resultCode == -1) {
            val file = File(getRealPathFromURI(data.data)!!)
            try {
                copy(file, requireContext().filesDir)
                adapter!!.mData[position].customAlarmRulesObject.alarmFileLocation = requireContext().filesDir.toString() + File.separator + file.name
                adapter!!.mData[position].customAlarmRulesObject.alarmFileName = file.name
                adapter!!.saveData()
                adapter!!.notifyItemChanged(position)
            } catch (e: IOException) {
                FirebaseCrashlytics.getInstance().recordException(e)
                Snackbar.make(requireView(), "Something went wrong", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun getRealPathFromURI(contentURI: Uri?): String? {
        val requireContext: Context = requireContext()
        val contentResolver: ContentResolver = requireContext.contentResolver
        val cursor = contentResolver.query(contentURI!!, null as Array<String?>?, null as String?, null as Array<String?>?, null as String?)
            ?: return contentURI.path
        cursor.moveToFirst()
        val result = cursor.getString(cursor.getColumnIndex("_data"))
        cursor.close()
        return result
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private fun copy(sourceFile: File, destFile: File) = this.launch {
        withContext(Dispatchers.IO) {
            if (!destFile.parentFile.exists()) destFile.parentFile.mkdirs()
            if (!destFile.exists()) {
                destFile.createNewFile()
            }
            val source = FileInputStream(sourceFile).channel
            val destination = FileOutputStream(destFile.toString() + File.separator + sourceFile.name).channel
            destination.transferFrom(source, 0, source.size())
            source.close()
            destination.close()
        }
    }

    override val coroutineContext: CoroutineContext =
        Dispatchers.Main + SupervisorJob()

}