package de.krbmr.encryptedroomdb

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import de.krbmr.encryptedroomdb.database.SecretDatabaseDao

class MainActivityViewModelFactory(private val dataSource: SecretDatabaseDao) : ViewModelProvider.Factory {

    @Suppress("unchecked_cast")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainActivityViewModel::class.java)) {
            return MainActivityViewModel(dataSource) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}