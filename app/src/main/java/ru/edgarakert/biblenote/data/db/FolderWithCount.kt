package ru.edgarakert.biblenote.data.db

import androidx.room.Embedded

data class FolderWithCount(
    @Embedded val folder: Folder,
    val noteCount: Int
)
