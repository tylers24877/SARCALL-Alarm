/*
 *  Copyright (C) Tyler Simmonds - All Rights Reserved
 *  Unauthorised copying of this file, via any medium is prohibited
 *  Written by Tyler Simmonds on behalf of SARCALL LTD, 2021
 *
 */

package uk.mrs.saralarm.ui.settings.extra_ui.rules

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.MaterialSharedAxis
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import uk.mrs.saralarm.R
import uk.mrs.saralarm.databinding.SettingsRulesFragmentBinding
import uk.mrs.saralarm.ui.settings.extra_ui.rules.support.RulesObject
import uk.mrs.saralarm.ui.settings.extra_ui.rules.support.SoundType
import uk.mrs.saralarm.ui.settings.extra_ui.support.DragAdapter
import java.io.File
import java.io.FileOutputStream
import java.lang.reflect.Type
import kotlin.coroutines.CoroutineContext
import kotlin.random.Random


class RulesFragment : Fragment(), CoroutineScope {
    private var _binding: SettingsRulesFragmentBinding? = null
    val binding get() = _binding!!

    private var adapter: RulesRecyclerViewAdapter? = null

    var position: Int = 100
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = SettingsRulesFragmentBinding.inflate(inflater, container, false)

        val rulesArray: ArrayList<RulesObject>

        val json: String? = context?.let { PreferenceManager.getDefaultSharedPreferences(it).getString("rulesJSON", "") }
        if (json.isNullOrBlank()) {
            rulesArray = ArrayList()
            rulesArray.add(RulesObject())
        } else {
            val type: Type = object : TypeToken<ArrayList<RulesObject>?>() {}.type
            rulesArray = Gson().fromJson(json, type)
        }
        binding.apply {
            rulesRecyclerView.layoutManager = LinearLayoutManager(context)

            adapter = RulesRecyclerViewAdapter(requireContext(), this@RulesFragment, rulesArray, this)

            rulesRecyclerView.adapter = adapter

            ItemTouchHelper(DragAdapter(adapter!!, requireContext(), 3, 12)).attachToRecyclerView(rulesRecyclerView)
            rulesFab.setOnClickListener {
                adapter!!.addItem()
                FirebaseAnalytics.getInstance(requireContext().applicationContext).logEvent("rules_row_added", null)
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

    override fun onDestroy() {
        super.onDestroy()
        coroutineContext[Job]!!.cancel()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 5 && resultCode == Activity.RESULT_OK) {
            if ((data != null) && (data.data != null)) {
                try {
                    val fileName = if (data.data!!.path != null) getFileName(requireContext(), data.data!!) else "alarm_custom_sound" + Random.nextInt(1000000, 1999999).toString()

                    adapter!!.data[position].customAlarmRulesObject.alarmSoundType = SoundType.CUSTOM
                    adapter!!.data[position].customAlarmRulesObject.alarmFileLocation = requireContext().filesDir.toString() + File.separator + fileName
                    adapter!!.data[position].customAlarmRulesObject.alarmFileName = fileName
                    adapter!!.saveData()
                    inputStreamToFile(requireContext(), fileName, data.data!!, requireContext().filesDir)
                } catch (e: Exception) {
                    FirebaseCrashlytics.getInstance().recordException(e)
                    try {
                        adapter!!.data[position].customAlarmRulesObject.alarmSoundType = SoundType.NONE
                        adapter!!.data[position].customAlarmRulesObject.alarmFileLocation = ""
                        adapter!!.data[position].customAlarmRulesObject.alarmFileName = ""
                        adapter!!.saveData()
                    } catch (e1: Exception) {
                        FirebaseCrashlytics.getInstance().recordException(e1)
                    }
                    Snackbar.make(requireView(), "Something went wrong", Snackbar.LENGTH_LONG).show()
                }
            }
        } else if (requestCode == 6 && resultCode == Activity.RESULT_OK) {
            if ((data != null)) {
                val uri: Uri? = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)

                if (uri != null) {
                    try {
                        if (RingtoneManager.getRingtone(context, uri).getTitle(context).isNotEmpty()) {
                            adapter!!.data[position].customAlarmRulesObject.alarmSoundType = SoundType.SYSTEM
                            adapter!!.data[position].customAlarmRulesObject.alarmFileLocation = uri.toString()
                            adapter!!.data[position].customAlarmRulesObject.alarmFileName = RingtoneManager.getRingtone(context, uri).getTitle(context)
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

    @SuppressLint("Range")
    private fun getFileName(mCon: Context, uri: Uri): String {
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

    override val coroutineContext: CoroutineContext =
        Dispatchers.Main + SupervisorJob()

}