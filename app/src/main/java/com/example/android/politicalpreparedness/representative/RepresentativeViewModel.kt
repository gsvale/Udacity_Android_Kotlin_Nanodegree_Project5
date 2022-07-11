package com.example.android.politicalpreparedness.representative

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.android.politicalpreparedness.network.CivicsApi
import com.example.android.politicalpreparedness.network.models.Address
import com.example.android.politicalpreparedness.representative.model.Representative
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.Exception

class RepresentativeViewModel : ViewModel() {

    // Establish live data for representatives and address

    val address = MutableLiveData<Address>(Address("", "", "", "", ""))

    private var _representatives = MutableLiveData<List<Representative>>()
    val representatives: LiveData<List<Representative>>
        get() = _representatives


    // Create function to fetch representatives from API from a provided address
    fun fetchRepresentativesFromAddress() {

        val addressString: String = getAddressFromFields()

        viewModelScope.launch {

            withContext(Dispatchers.IO) {
                try {
                    val (offices, officials) = CivicsApi.retrofitService.getRepresentatives(
                        addressString
                    ).await()

                    _representatives.postValue(offices.flatMap { office ->
                        office.getRepresentatives(
                            officials
                        )
                    })

                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }


        }

    }

    // Create function get address from geo location
    fun getAddressFromGeolocation(addressItem: Address) {
        address.value = addressItem
    }

    // Create function to get address from individual fields
    private fun getAddressFromFields(): String {
        return address.value!!.toFormattedString()
    }


}
