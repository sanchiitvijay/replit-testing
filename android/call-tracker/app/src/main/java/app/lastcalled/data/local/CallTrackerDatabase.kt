package app.lastcalled.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ContactEntity::class,
        ContactPhoneEntity::class,
        CallRecordEntity::class,
        AppSettingsEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class CallTrackerDatabase : RoomDatabase() {
    abstract fun contactDao(): ContactDao
    abstract fun callRecordDao(): CallRecordDao
    abstract fun appSettingsDao(): AppSettingsDao

    companion object {
        fun create(context: Context): CallTrackerDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                CallTrackerDatabase::class.java,
                "last-called.db",
            ).build()
    }
}
