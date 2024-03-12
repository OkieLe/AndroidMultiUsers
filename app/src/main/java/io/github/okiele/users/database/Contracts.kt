package io.github.okiele.users.database

import android.net.Uri

object Contracts {
    const val AUTHORITY = "io.github.okiele.users"
    val AUTHORITY_URI: Uri = Uri.parse("content://$AUTHORITY")
    object Settings {
        const val TABLE_NAME = "settings"
        val CONTENT_URI: Uri = Uri.withAppendedPath(AUTHORITY_URI, TABLE_NAME)
        const val CONTENT_ITEM_TYPE = "vnd.android.cursor.item/$TABLE_NAME"
        const val CONTENT_TYPE = "vnd.android.cursor.dir/$TABLE_NAME"

        const val KEY = "key"
        const val VALUE = "value"
    }
}
