package de.krbmr.encryptedroomdb.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

const val DATABASE_NAME = "secret_database"

@Database(entities = [Secret::class], version = 1)
abstract class SecretDatabase : RoomDatabase() {

    abstract val secretDatabaseDao: SecretDatabaseDao

    companion object {

        @Volatile
        private var INSTANCE: SecretDatabase? = null

        /**
         * Returns the database instance. If the database does not exist yet, it will be created.
         */
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

        /**
         * Creates a database instance and returns it.
         */
        private fun createDB(context: Context): SecretDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                SecretDatabase::class.java,
                DATABASE_NAME
            )
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}