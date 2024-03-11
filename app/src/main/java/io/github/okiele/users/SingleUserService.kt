package io.github.okiele.users

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log

class SingleUserService : Service() {
    private val binder = object : ISingleUser.Stub() {
        override fun get(userId: Int): Boolean {
            Log.i(TAG, "Check $userId")
            return userId == applicationContext.getUser().getIdentifier()
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

    companion object {
        private const val TAG = "SingleUserService"
        private const val ACTION = "io.github.okiele.users.SINGLE_USER"
    }
}
