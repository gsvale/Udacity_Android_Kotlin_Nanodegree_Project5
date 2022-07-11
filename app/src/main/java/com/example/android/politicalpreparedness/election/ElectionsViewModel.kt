package com.example.android.politicalpreparedness.election

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.android.politicalpreparedness.database.ElectionDao
import com.example.android.politicalpreparedness.network.CivicsApi
import com.example.android.politicalpreparedness.network.models.Election
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.Exception

// Construct ViewModel and provide election datasource
class ElectionsViewModel(private val dataSource: ElectionDao) : ViewModel() {

    // Create live data val for upcoming elections

    private var _upcomingElections = MutableLiveData<List<Election>>()
    val upcomingElections: LiveData<List<Election>>
        get() = _upcomingElections

    // Create live data val for saved elections

    lateinit var savedElections: LiveData<List<Election>>

    init {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    savedElections = dataSource.getElections()
                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }
        }
    }

    //Create val and functions to populate live data for upcoming elections from the API and saved elections from local database
    fun fetchUpcomingElections() {

        viewModelScope.launch {

            withContext(Dispatchers.IO) {
                try {
                    val electionsResponse = CivicsApi.retrofitService.getElections().await()

                    _upcomingElections.postValue(electionsResponse.elections)

                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }

        }

    }


}