package io.github.okiele.users

import android.app.Application
import android.content.ContentValues
import android.database.ContentObserver
import android.os.UserHandle
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import io.github.okiele.users.database.Contracts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val currentUser = application.getUser().getIdentifier()

    private val sameUserData = MutableLiveData(true)
    private val settingsData = MutableLiveData(listOf<String>())

    private val contentResolver by lazy { application.contentResolver }

    fun getSameUser(): LiveData<Boolean> = sameUserData
    fun setSameUser(isSame: Boolean) {
        sameUserData.postValue(isSame)
    }

    fun getSettings(): LiveData<List<String>> {
        viewModelScope.launch {
            settingsData.postValue(fetchSettings())
        }
        viewModelScope.launch {
            observeSettings().collectLatest {
                settingsData.postValue(fetchSettings())
            }
        }
        return settingsData
    }

    fun putSettings(key: String, value: String) {
        viewModelScope.launch {
            val values = ContentValues().apply {
                put(Contracts.Settings.KEY, key)
                put(Contracts.Settings.VALUE, value)
            }
            contentResolver.insert(Contracts.Settings.CONTENT_URI, values)
        }
    }

    fun sendSettings(singleUserBinder: ISingleUser?, key: String, value: String) {
        singleUserBinder?.set(key, value)
    }

    private suspend fun fetchSettings(): List<String> = withContext(Dispatchers.IO) {
        val settings = mutableListOf<String>()
        contentResolver.query(Contracts.Settings.CONTENT_URI, null, null, null)?.let {
            while (it.moveToNext()) {
                val key = it.getString(it.getColumnIndexOrThrow(Contracts.Settings.KEY))
                val value = it.getString(it.getColumnIndexOrThrow(Contracts.Settings.VALUE))
                settings.add("$key: $value")
            }
            it.close()
        }
        return@withContext settings
    }

    private suspend fun observeSettings(): Flow<Unit> = callbackFlow {
        val observer = object : ContentObserver(null) {
            override fun onChange(selfChange: Boolean) {
                super.onChange(selfChange)
                trySend(Unit)
            }
        }
        contentResolver.registerContentObserverAsUser(Contracts.Settings.CONTENT_URI, true, observer, UserHandle.SYSTEM)
        awaitClose {
            contentResolver.unregisterContentObserver(observer)
        }
    }.flowOn(Dispatchers.IO)
}
