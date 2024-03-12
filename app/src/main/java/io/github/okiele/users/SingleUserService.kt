package io.github.okiele.users

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import io.github.okiele.users.database.Settings
import io.github.okiele.users.database.UserDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SingleUserService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val settingsDao by lazy { UserDatabase.get(applicationContext).settingsDao() }

    private val binder = object : ISingleUser.Stub() {
        override fun get(userId: Int): Boolean {
            return userId == applicationContext.getUser().getIdentifier()
        }

        override fun set(key: String?, value: String?): Boolean {
            if (key.isNullOrBlank() || value.isNullOrBlank()) {
                return false
            }

            serviceScope.launch {
                withContext(Dispatchers.IO) {
                    settingsDao.insert(Settings(key, value)).onEach {
                        Log.d(TAG, "Inserted row $it")
                    }
                }
            }
            return true
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        Log.i(TAG, "onBind")
        return if (ACTION == intent.action) {
            binder
        } else {
            Log.e(TAG, "No valid action provided")
            null
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "SingleUserService"
        private const val ACTION = "io.github.okiele.users.SINGLE_USER"
    }
}
