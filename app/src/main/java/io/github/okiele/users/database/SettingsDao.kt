package io.github.okiele.users.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SettingsDao {
    @Query("SELECT * FROM ${Contracts.Settings.TABLE_NAME}")
    fun getAll(): Flow<List<Settings>>

    @Query("SELECT * FROM ${Contracts.Settings.TABLE_NAME} WHERE " + Contracts.Settings.KEY + "=:key")
    fun get(key: String): Flow<List<Settings>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vararg settings: Settings): List<Long>

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(settings: Settings): Int

    @Delete
    suspend fun delete(vararg settings: Settings): Int
}
