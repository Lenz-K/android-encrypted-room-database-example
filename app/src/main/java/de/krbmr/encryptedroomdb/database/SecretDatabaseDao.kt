package de.krbmr.encryptedroomdb.database

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface SecretDatabaseDao {

    @Insert
    fun insertSecret(secret: Secret)

    @Query("SELECT * FROM secret_table")
    fun getAllSecrets(): LiveData<List<Secret>>

    @Query("Delete FROM secret_table")
    fun clearSecrets()
}