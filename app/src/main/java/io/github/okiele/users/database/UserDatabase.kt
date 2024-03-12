package io.github.okiele.users.database

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.InvalidationTracker
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(version = 1, entities = [Settings::class], exportSchema = false)
abstract class UserDatabase : RoomDatabase() {

    abstract fun settingsDao(): SettingsDao

    companion object {

        private val tableRootUri = mapOf(
            Contracts.Settings.TABLE_NAME to Contracts.Settings.CONTENT_URI
        )

        @Volatile
        private var instance: UserDatabase? = null
        fun get(context: Context): UserDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.createDeviceProtectedStorageContext(),
                    UserDatabase::class.java,
                    "settings"
                ).addMigrations().build().apply {
                    invalidationTracker.addObserver(
                        object : InvalidationTracker.Observer(tableRootUri.keys.toTypedArray()) {
                            override fun onInvalidated(tables: Set<String>) {
                                Log.i("UserDatabase", "Tables changed ${tables.joinToString(",")}")
                                for (table in tables) {
                                    tableRootUri[table]?.let { context.contentResolver.notifyChange(it, null) }
                                }
                            }
                        }
                    )
                }
            }
    }
}
