package ru.edgarakert.biblenote.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "prayer_entries",
    foreignKeys = [ForeignKey(
        entity = PrayerRequest::class,
        parentColumns = ["id"],
        childColumns = ["requestId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("requestId")]
)
data class PrayerEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val requestId: Long,
    val text: String = "",
    val date: Long = System.currentTimeMillis(),
)
