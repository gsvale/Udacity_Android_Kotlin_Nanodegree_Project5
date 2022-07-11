package com.example.android.politicalpreparedness.election

import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.android.politicalpreparedness.database.ElectionDao
import com.example.android.politicalpreparedness.network.CivicsApi
import com.example.android.politicalpreparedness.network.models.Address
import com.example.android.politicalpreparedness.network.models.Election
import com.example.android.politicalpreparedness.network.models.VoterInfoResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.Exception

class VoterInfoViewModel(private val dataSource: ElectionDao) : ViewModel() {


    // Add live data to hold voter info

    private var _voterInfo = MutableLiveData<VoterInfoResponse>()
    val voterInfo: LiveData<VoterInfoResponse>
        get() = _voterInfo

    // Add var and methods to populate voter info

    private var _electionDate = MutableLiveData<String>()
    val electionDate: LiveData<String>
        get() = _electionDate

    private var _stateLocationsUrlVisibility = MutableLiveData<Int>()
    val stateLocationsUrlVisibility: LiveData<Int>
        get() = _stateLocationsUrlVisibility

    private var _stateBallotUrlVisibility = MutableLiveData<Int>()
    val stateBallotUrlVisibility: LiveData<Int>
        get() = _stateBallotUrlVisibility

    private var _address = MutableLiveData<String>()
    val address: LiveData<String>
        get() = _address

    // Add var and methods to support loading URLs

    private var _loadLocationsUrl = MutableLiveData<String?>("")
    val loadLocationsUrl: LiveData<String?>
        get() = _loadLocationsUrl

    fun resetLoadLocationsUrl() {
        _loadLocationsUrl.value = ""
    }

    private var _loadBallotUrl = MutableLiveData<String?>("")
    val loadBallotUrl: LiveData<String?>
        get() = _loadBallotUrl

    fun resetLoadBallotUrl() {
        _loadBallotUrl.value = ""
    }

    fun loadStateLocationsUrl() {
        _loadLocationsUrl.value =
            _voterInfo.value!!.state!![0].electionAdministrationBody.electionInfoUrl
    }

    fun loadStateBallotUrl() {
        _loadBallotUrl.value =
            _voterInfo.value!!.state!![0].electionAdministrationBody.ballotInfoUrl
    }

    fun fetchVoterInfo(currentAddress: Address, electionId: Int) {

        viewModelScope.launch {

            withContext(Dispatchers.IO) {

                try {

                    val voterInfo = CivicsApi.retrofitService.getVoterInfo(
                        currentAddress.toFormattedString(),
                        electionId.toLong()
                    ).await()

                    _electionDate.postValue(voterInfo.election.electionDay.toString())

                    if (voterInfo.state.isNullOrEmpty()) {
                        _stateLocationsUrlVisibility.postValue(View.GONE)
                        _stateBallotUrlVisibility.postValue(View.GONE)
                    } else {
                        if (voterInfo.state[0].electionAdministrationBody.votingLocationFinderUrl.isNullOrEmpty()) {
                            _stateLocationsUrlVisibility.postValue(View.GONE)
                        } else {
                            _stateLocationsUrlVisibility.postValue(View.VISIBLE)
                        }

                        if (voterInfo.state[0].electionAdministrationBody.ballotInfoUrl.isNullOrEmpty()) {
                            _stateBallotUrlVisibility.postValue(View.GONE)
                        } else {
                            _stateBallotUrlVisibility.postValue(View.VISIBLE)
                        }

                        if (voterInfo.state[0].electionAdministrationBody.correspondenceAddress != null) {
                            _address.postValue(voterInfo.state[0].electionAdministrationBody.correspondenceAddress!!.toFormattedString())
                        }
                    }

                    _voterInfo.postValue(voterInfo)

                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }

        }

    }

    // Add var and methods to save and remove elections to local database
    // cont'd -- Populate initial state of save button to reflect proper action based on election saved status

    /**
     * Hint: The saved state can be accomplished in multiple ways. It is directly related to how elections are saved/removed from the database.
     */

    private var _existsInDatabase = MutableLiveData<Election?>()
    val existsInDatabase: LiveData<Election?>
        get() = _existsInDatabase

    private var _buttonTitle = MutableLiveData<String>()
    val buttonTitle: LiveData<String>
        get() = _buttonTitle


    fun setFollowButtonText(title: String) {
        _buttonTitle.value = title
    }

    fun checkIfElectionExistsInDatabase() {
        viewModelScope.launch {

            withContext(Dispatchers.IO) {
                try {
                    val election = dataSource.getElection(voterInfo.value!!.election.id)
                    _existsInDatabase.postValue(election)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

        }
    }

    fun handleSaveButtonClick() {

        if (existsInDatabase.value != null) {
            deleteElection()
        } else {
            saveElection()
        }

    }

    private fun deleteElection() {

        viewModelScope.launch {

            withContext(Dispatchers.IO) {
                try {
                    val id = voterInfo.value!!.election.id
                    dataSource.deleteElection(id)
                    checkIfElectionExistsInDatabase()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

        }

    }

    private fun saveElection() {

        viewModelScope.launch {

            withContext(Dispatchers.IO) {
                try {
                    val election = voterInfo.value!!.election
                    dataSource.insertElection(election)
                    checkIfElectionExistsInDatabase()
                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }

        }

    }

}