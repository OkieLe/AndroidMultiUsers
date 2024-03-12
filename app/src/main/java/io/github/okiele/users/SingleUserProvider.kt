package io.github.okiele.users

import android.content.ContentProvider
import android.content.ContentResolver
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.util.Log
import androidx.room.OnConflictStrategy
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.SupportSQLiteQueryBuilder
import io.github.okiele.users.database.Contracts
import io.github.okiele.users.database.UserDatabase

class SingleUserProvider : ContentProvider() {
    private var userDatabase: UserDatabase? = null
    private var databaseHelper: SupportSQLiteOpenHelper? = null

    override fun onCreate(): Boolean {
        UserDatabase.get(requireContext()).let {
            userDatabase = it
            databaseHelper = it.openHelper
        }
        return true
    }

    private fun queryLocal(table: String, projection: Array<out String>?, queryArgs: Bundle): Cursor? {
        val queryBuilder = SupportSQLiteQueryBuilder.builder(table)
        appendProjection(queryBuilder, projection ?: defaultColumns[table].orEmpty())
        appendSelection(queryBuilder, queryArgs)
        appendGroupBy(queryBuilder, queryArgs)
        appendOrderBy(queryBuilder, queryArgs)
        appendLimit(queryBuilder, queryArgs)
        Log.d(TAG, queryBuilder.create().sql)
        return databaseHelper?.readableDatabase?.query(queryBuilder.create())
    }

    override fun query(
        uri: Uri, projection: Array<out String>?, queryArgs: Bundle?, cancellationSignal: CancellationSignal?
    ): Cursor? {
        if (uriMatcher.match(uri) == UriMatcher.NO_MATCH) {
            Log.w(TAG, "No match to query")
            return null
        } else if (uri.pathSegments.isEmpty()) {
            Log.w(TAG, "No valid table to query")
            return null
        }
        val table: String = uri.pathSegments.first()
        Log.d(TAG, "Querying table $table")
        val whereArgs = mergeArgsFromUri(
            uri, queryArgs?.getString(ContentResolver.QUERY_ARG_SQL_SELECTION),
            queryArgs?.getStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS))
        val intQueryArgs = (queryArgs ?: Bundle()).apply {
            whereArgs.first?.let { putString(ContentResolver.QUERY_ARG_SQL_SELECTION, it) }
            whereArgs.second?.let { putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, it) }
        }
        return queryLocal(table, projection, intQueryArgs)
    }

    override fun query(
        uri: Uri, projection: Array<out String>?,
        selection: String?, selectionArgs: Array<out String>?, sortOrder: String?
    ): Cursor? {
        Log.d(TAG, "query $uri")
        if (uriMatcher.match(uri) == UriMatcher.NO_MATCH) {
            Log.w(TAG, "No match to query")
            return null
        } else if (uri.pathSegments.isEmpty()) {
            Log.w(TAG, "No valid table to query")
            return null
        }
        val table: String = uri.pathSegments.first()
        Log.d(TAG, "Querying table $table")
        val whereArgs = mergeArgsFromUri(uri, selection, selectionArgs)
        val queryArgs = Bundle().apply {
            whereArgs.first?.let { putString(ContentResolver.QUERY_ARG_SQL_SELECTION, it) }
            whereArgs.second?.let { putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, it) }
            sortOrder?.let { putString(ContentResolver.QUERY_ARG_SQL_SORT_ORDER, it) }
        }
        return queryLocal(table, projection, queryArgs)
    }

    override fun getType(uri: Uri): String? {
        return when (uriMatcher.match(uri)) {
            SETTINGS -> Contracts.Settings.CONTENT_TYPE
            SETTINGS_KEY -> Contracts.Settings.CONTENT_ITEM_TYPE
            else -> null
        }
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        Log.d(TAG, "Insert $uri")
        val match = uriMatcher.match(uri)
        if (match == UriMatcher.NO_MATCH) {
            Log.w(TAG, "No match to insert")
            return null
        } else if (values?.isEmpty != false) {
            Log.w(TAG, "No values to insert")
            return null
        } else if (uri.pathSegments.isEmpty()) {
            Log.w(TAG, "No valid table to insert")
            return null
        }
        val table: String = uri.pathSegments.first()
        Log.d(TAG, "Inserting into table $table")
        return userDatabase?.openHelper?.writableDatabase?.let { db ->
            when (match) {
                SETTINGS -> {
                    db.insert(table, OnConflictStrategy.REPLACE, values).let {
                        Uri.withAppendedPath(Contracts.Settings.CONTENT_URI, values.getAsString(Contracts.Settings.KEY))
                    }.also {
                        Log.d(TAG, "Inserted settings row $it")
                        context?.contentResolver?.notifyChange(Contracts.Settings.CONTENT_URI, null)
                    }
                }

                else -> null
            }
        }
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        val match = uriMatcher.match(uri)
        if (match == UriMatcher.NO_MATCH) {
            Log.w(TAG, "No match to delete")
            return 0
        } else if (uri.pathSegments.isEmpty()) {
            Log.w(TAG, "No valid table to delete")
            return 0
        }
        val table: String = uri.pathSegments.first()
        Log.d(TAG, "Deleting from table $table")
        val whereArgs = mergeArgsFromUri(uri, selection, selectionArgs)
        return userDatabase?.openHelper?.writableDatabase?.let { db ->
            when (match) {
                SETTINGS -> {
                    db.delete(table, whereArgs.first, whereArgs.second).also {
                        Log.d(TAG, "Deleted settings $it")
                        context?.contentResolver?.notifyChange(Contracts.Settings.CONTENT_URI, null)
                    }
                }

                else -> 0
            }
        } ?: 0
    }

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int {
        val match = uriMatcher.match(uri)
        if (match == UriMatcher.NO_MATCH) {
            Log.w(TAG, "No match to update")
            return 0
        } else if (values?.isEmpty != false) {
            Log.w(TAG, "No values to update")
            return 0
        } else if (uri.pathSegments.isEmpty()) {
            Log.w(TAG, "No valid table to update")
            return 0
        }
        val table: String = uri.pathSegments.first()
        Log.d(TAG, "Update into table $table")
        val whereArgs = mergeArgsFromUri(uri, selection, selectionArgs)
        return userDatabase?.openHelper?.writableDatabase?.let { db ->
            when (match) {
                SETTINGS -> {
                    db.update(table, OnConflictStrategy.REPLACE, values, whereArgs.first, whereArgs.second).also {
                        Log.d(TAG, "Updated settings ${values.getAsString(Contracts.Settings.KEY)}: $it")
                        context?.contentResolver?.notifyChange(Contracts.Settings.CONTENT_URI, null)
                    }
                }

                SETTINGS_KEY -> {
                    db.update(table, OnConflictStrategy.REPLACE, values, whereArgs.first, whereArgs.second).also {
                        Log.d(TAG, "Updated settings ${values.getAsString(Contracts.Settings.KEY)}: $it")
                        context?.contentResolver?.notifyChange(Contracts.Settings.CONTENT_URI, null)
                    }
                }

                else -> 0
            }
        } ?: 0
    }

    companion object {
        private const val TAG = "SingleUserProvider"
        private const val SETTINGS = 1001
        private const val SETTINGS_KEY = 1002
        private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH)
        init {
            uriMatcher.addURI(Contracts.AUTHORITY, Contracts.Settings.TABLE_NAME, SETTINGS)
            uriMatcher.addURI(Contracts.AUTHORITY, Contracts.Settings.TABLE_NAME + "/*", SETTINGS_KEY)
        }
        private val defaultColumns = mapOf(
            Contracts.Settings.TABLE_NAME to arrayOf(Contracts.Settings.KEY, Contracts.Settings.VALUE)
        )

        private fun appendProjection(builder: SupportSQLiteQueryBuilder, projection: Array<out String>) {
            builder.columns(projection)
        }

        private fun appendSelection(builder: SupportSQLiteQueryBuilder, queryArgs: Bundle) {
            val selection = queryArgs.getString(ContentResolver.QUERY_ARG_SQL_SELECTION)
            val selectionArgs = queryArgs.getStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS)
            builder.selection(selection, selectionArgs)
        }

        private fun appendGroupBy(builder: SupportSQLiteQueryBuilder, queryArgs: Bundle) {
            val groupBy = queryArgs.getString(ContentResolver.QUERY_ARG_SQL_GROUP_BY)
            if (!groupBy.isNullOrBlank()) {
                builder.groupBy(groupBy)
            } else {
                return
            }
            val having = queryArgs.getString(ContentResolver.QUERY_ARG_SQL_HAVING)
            if (!having.isNullOrBlank()) {
                builder.having(having)
            }
        }

        private fun appendOrderBy(builder: SupportSQLiteQueryBuilder, queryArgs: Bundle) {
            val sortOrder = queryArgs.getString(ContentResolver.QUERY_ARG_SQL_SORT_ORDER)
            if (!sortOrder.isNullOrBlank()) {
                builder.orderBy(sortOrder)
            }
        }

        private fun appendLimit(builder: SupportSQLiteQueryBuilder, queryArgs: Bundle) {
            val limitNum = queryArgs.getInt(ContentResolver.QUERY_ARG_LIMIT)
            val offsetNum = queryArgs.getInt(ContentResolver.QUERY_ARG_OFFSET)
            val limit = if (limitNum > 0 && offsetNum > 0) {
                "$offsetNum,$limitNum"
            } else if (limitNum > 0) {
                limitNum.toString()
            } else ""
            if (limit.isNotBlank()) {
                builder.limit(limit)
            }
        }

        private fun mergeArgsFromUri(
            uri: Uri, where: String?, whereArgs: Array<out String>?
        ): Pair<String?, Array<out String>?> {
            val match = uriMatcher.match(uri)
            val mergedWhere = when (match) {
                SETTINGS_KEY -> (Contracts.Settings.KEY + "=?")
                else -> null
            }?.let {
                if (where.isNullOrBlank().not()) {
                    "$it AND $where"
                } else it
            }
            val mergedWhereArgs = when (match) {
                SETTINGS_KEY -> arrayOf(uri.lastPathSegment.orEmpty()).plus(whereArgs.orEmpty())
                else -> whereArgs.orEmpty()
            }
            return Pair(mergedWhere, mergedWhereArgs)
        }
    }
}
