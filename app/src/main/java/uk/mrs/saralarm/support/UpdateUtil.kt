package uk.mrs.saralarm.support
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.errorprone.annotations.Keep
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import uk.mrs.saralarm.R

class UpdateUtil {
    @Keep
    data class VersionData(
        @SerializedName("version")
        var version: String,
        @SerializedName("version_code")
        var version_code: Int,
        @SerializedName("url")
        var url: String,
        @SerializedName("release_notes")
        var release_notes: String
    )
    private val remoteConfig = Firebase.remoteConfig
    init {
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 3600
        }
        remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults)
        remoteConfig.setConfigSettingsAsync(configSettings)

        remoteConfig.fetchAndActivate()
    }

    private val _mutableLiveData = MutableLiveData<VersionData>()
    val remoteLiveData: LiveData<VersionData> = _mutableLiveData

    private val _mutableLiveDataW3W = MutableLiveData<String>()
    val remoteLiveDataW3W: LiveData<String> = _mutableLiveDataW3W

    private val _mutableLiveDataW3WClickLimit = MutableLiveData<Long>()
    val remoteLiveDataW3WClickLimit: LiveData<Long> = _mutableLiveDataW3WClickLimit
    fun remoteConfiguration(){
        val gson = Gson()
        val remote = remoteConfig.fetchAndActivate()
        remote.addOnSuccessListener {
            val stringJson = remoteConfig.getString("update")
            if (stringJson.isNotEmpty()) {
                val jsonModel = gson.fromJson(stringJson, VersionData::class.java)
                val vD = VersionData(
                    version = jsonModel.version,
                    version_code = jsonModel.version_code,
                    url = jsonModel.url,
                    release_notes = jsonModel.release_notes
                )
                _mutableLiveData.value = vD
            }
        }
    }

    fun remoteConfigurationW3W(){
        val remote = remoteConfig.fetchAndActivate()
        remote.addOnSuccessListener {
            _mutableLiveDataW3W.value =  remoteConfig.getString("W3W_API")
        }
    }
    fun remoteConfigurationW3WClickLimit(){
        val remote = remoteConfig.fetchAndActivate()
        remote.addOnSuccessListener {
            _mutableLiveDataW3WClickLimit.value =  remoteConfig.getLong("W3W_click_limit")
        }
    }
}