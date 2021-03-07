package uk.mrs.saralarm.ui.settings.extra_ui.rules

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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
import uk.mrs.saralarm.ui.settings.extra_ui.rules.support.RulesObject
import uk.mrs.saralarm.ui.settings.extra_ui.rules.support.SoundType
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

        adapter = RulesRecyclerViewAdapter(requireContext(), this, rulesArray, root)

        root.rulesRecyclerView.adapter = adapter

        ItemTouchHelper(RulesDragAdapter(adapter!!, 3, 12)).attachToRecyclerView(root.rulesRecyclerView)
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
                    val fileName = if (data.data!!.path != null) getFileName(requireContext(), data.data!!) else "alarm_custom_sound" + Random.nextInt(1000000, 1999999).toString()

                    adapter!!.mData[position].customAlarmRulesObject.alarmSoundType = SoundType.CUSTOM
                    adapter!!.mData[position].customAlarmRulesObject.alarmFileLocation = requireContext().filesDir.toString() + File.separator + fileName
                    adapter!!.mData[position].customAlarmRulesObject.alarmFileName = fileName
                    adapter!!.saveData()
                    inputStreamToFile(requireContext(), fileName, data.data!!, requireContext().filesDir)
                } catch (e: Exception) {
                    try {
                        adapter!!.mData[position].customAlarmRulesObject.alarmSoundType = SoundType.NONE
                        adapter!!.mData[position].customAlarmRulesObject.alarmFileLocation = ""
                        adapter!!.mData[position].customAlarmRulesObject.alarmFileName = ""
                        adapter!!.saveData()
                    } catch (e: Exception) {
                        FirebaseCrashlytics.getInstance().recordException(e)
                    }
                    FirebaseCrashlytics.getInstance().recordException(e)
                    Snackbar.make(requireView(), "Something went wrong", Snackbar.LENGTH_LONG).show()
                }
            }
        } else if (requestCode == 6 && resultCode == Activity.RESULT_OK) {
            if ((data != null)) {
                val uri: Uri? = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)

                if (uri != null) {
                    try {
                        if (RingtoneManager.getRingtone(context, uri).getTitle(context).isNotEmpty()) {
                            adapter!!.mData[position].customAlarmRulesObject.alarmSoundType = SoundType.SYSTEM
                            adapter!!.mData[position].customAlarmRulesObject.alarmFileLocation = uri.toString()
                            adapter!!.mData[position].customAlarmRulesObject.alarmFileName = RingtoneManager.getRingtone(context, uri).getTitle(context)
                            adapter!!.saveData()
                        } else {
                            Toast.makeText(context, "Using default sound.", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        FirebaseCrashlytics.getInstance().recordException(e)
                        Snackbar.make(requireView(), "Something went wrong", Snackbar.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    fun getFileName(mCon: Context, uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor: Cursor? = mCon.contentResolver.query(uri, null, null, null, null)
            cursor.use { cursored ->
                if (cursored != null && cursored.moveToFirst()) {
                    result = cursored.getString(cursored.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result!!.lastIndexOf('/')
            if (cut != -1) {
                result = result!!.substring(cut + 1)
            }
        }
        return result as String
    }

    private fun inputStreamToFile(context: Context, fileName: String, uri: Uri, destFile: File) = this.launch {
        withContext(Dispatchers.IO) {
            val inputStream = context.contentResolver.openInputStream(uri)
            val output = FileOutputStream(destFile.toString() + File.separator + fileName)
            if (inputStream != null) {
                inputStream.copyTo(output, 4 * 1024)
                inputStream.close()
            } else {
                Snackbar.make(requireView(), "Something went wrong. Error code:xInputStream", Snackbar.LENGTH_LONG).show()
            }
            output.close()
        }
    }
    override val coroutineContext: CoroutineContext =
        Dispatchers.Main + SupervisorJob()

}