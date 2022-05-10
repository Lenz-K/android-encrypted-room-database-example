package de.krbmr.encryptedroomdb.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Secret::class], version = 1)
abstract class SecretDatabase : RoomDatabase() {

    abstract val secretDatabaseDao: SecretDatabaseDao

    companion object {

        @Volatile
        private var INSTANCE: SecretDatabase? = null

        fun getInstance(context: Context): SecretDatabase {
            synchronized(this) {
                var instance = INSTANCE

                if (instance == null) {
                    instance = createDB(context)
                    INSTANCE = instance
                }
                return instance
            }
        }

        private fun createDB(context: Context): SecretDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                SecretDatabase::class.java,
                "secret_database"
            )
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}