package io.github.okiele.users

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.UserHandle
import android.util.Log

class SingleUserReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.i(TAG, "Received ${intent.getIntExtra(KEY_USER, -1)} from ${UserHandle.getCallingUserId()}")
    }

    companion object {
        private const val TAG = "SingleUserReceiver"
        private const val ACTION = "io.github.okiele.users.SINGLE_USER"
        private const val KEY_USER = "USER"
        fun broadcast(context: Context, userId: Int) {
            context.sendBroadcast(Intent(ACTION).apply {
                `package` = context.packageName
                addCategory(Intent.CATEGORY_DEFAULT)
                putExtra(KEY_USER, userId)
            })
        }
    }
}
