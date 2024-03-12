package io.github.okiele.users.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = Contracts.Settings.TABLE_NAME)
data class Settings(
    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = Contracts.Settings.KEY)
    val key: String,
    @ColumnInfo(name = Contracts.Settings.VALUE)
    val value: String
)
