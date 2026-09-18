package ru.edgarakert.biblenote.data.db

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Note::class, Folder::class],
    version = 2,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2)]
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao

    abstract fun folderDao(): FolderDao

    companion object {
        /**
         * Обработчика даунгрейда сознательно нет.
         * fallbackToDestructiveMigrationOnDowngrade() стёр бы заметки пользователя,
         * а облачной синхронизации в приложении нет — потеря была бы безвозвратной.
         * Play Store откатов не раздаёт, так что путь достижим только при ручной
         * установке старого APK; там падение с «migration required» лучше молчаливого
         * удаления данных: обратная установка свежей версии возвращает всё на место.
         */
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "biblenote.db").build()
    }
}
