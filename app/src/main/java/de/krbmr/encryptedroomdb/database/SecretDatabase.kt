package de.krbmr.encryptedroomdb.database

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import net.sqlcipher.database.SupportFactory
import javax.crypto.KeyGenerator

const val DATABASE_NAME = "secret_database.db"
const val SHARED_PREFS_NAME = "de.krbmr.encryptedroomdb.shared_prefs" // Choose a unique name!
const val PREFS_KEY_PASSPHRASE = "PREFS_KEY_PASSPHRASE"
const val ALGORITHM_AES = "AES"
const val KEY_SIZE = 256

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
            val passphrase = getPassphrase(context) ?: initializePassphrase(context)

            val factory = SupportFactory(passphrase)
            return Room.databaseBuilder(
                context.applicationContext,
                SecretDatabase::class.java,
                DATABASE_NAME
            )
                .openHelperFactory(factory)
                .fallbackToDestructiveMigration()
                .build()
        }

        /**
         * Generates a passphrase and stores it in the encrypted shared preferences.
         * Returns the newly generated passphrase.
         */
        private fun initializePassphrase(context: Context): ByteArray {
            val passphrase = generatePassphrase()

            getSharedPrefs(context).edit(commit = true) {
                putString(PREFS_KEY_PASSPHRASE, passphrase.toString(Charsets.ISO_8859_1))
            }

            return passphrase
        }

        /**
         * Retrieves the passphrase for encryption from the encrypted shared preferences.
         * Returns null if there is no stored passphrase.
         */
        private fun getPassphrase(context: Context): ByteArray? {
            val passphraseString = getSharedPrefs(context)
                .getString(PREFS_KEY_PASSPHRASE, null)
            return passphraseString?.toByteArray(Charsets.ISO_8859_1)
        }

        /**
         * Returns a reference to the encrypted shared preferences.
         */
        private fun getSharedPrefs(context: Context): SharedPreferences {
            val masterKey =
                MasterKey.Builder(context, MasterKey.DEFAULT_MASTER_KEY_ALIAS)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()

            return EncryptedSharedPreferences.create(
                context,
                SHARED_PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }

        /**
         * Generates and returns a passphrase.
         */
        private fun generatePassphrase(): ByteArray {
            val keyGenerator = KeyGenerator.getInstance(ALGORITHM_AES)
            keyGenerator.init(KEY_SIZE)
            return keyGenerator.generateKey().encoded
        }
    }
}