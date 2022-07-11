package com.example.android.politicalpreparedness.election

import android.Manifest
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.android.politicalpreparedness.BuildConfig
import com.example.android.politicalpreparedness.R
import com.example.android.politicalpreparedness.database.ElectionDatabase
import com.example.android.politicalpreparedness.databinding.FragmentVoterInfoBinding
import com.example.android.politicalpreparedness.network.models.Address
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
import com.google.android.material.snackbar.Snackbar
import androidx.lifecycle.Observer
import java.util.*

class VoterInfoFragment : Fragment() {

    companion object {
        // Add Constant for Location request
        private const val REQUEST_LOCATION_PERMISSIONS_RESULT_CODE = 33
        private const val REQUEST_TURN_DEVICE_LOCATION_ON = 34
    }

    private lateinit var viewModel: VoterInfoViewModel

    private lateinit var binding: FragmentVoterInfoBinding

    private lateinit var currentAddress: Address
    private var electionId: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        // Add ViewModel values and create ViewModel

        val application = requireNotNull(this.activity).application
        val dataSource = ElectionDatabase.getInstance(application).electionDao
        val viewModelFactory = VoterInfoViewModelFactory(dataSource)
        viewModel = ViewModelProvider(this, viewModelFactory).get(VoterInfoViewModel::class.java)

        //Add binding values
        binding = FragmentVoterInfoBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel


        // Populate voter info -- hide views without provided data.
        /**
        Hint: You will need to ensure proper data is provided from previous fragment.
         */
        electionId = VoterInfoFragmentArgs.fromBundle(requireArguments()).argElectionId
        val division = VoterInfoFragmentArgs.fromBundle(requireArguments()).argDivision

        viewModel.voterInfo.observe(viewLifecycleOwner, Observer {
            it?.let {

                binding.electionName.setTitleTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.white
                    )
                )
                binding.electionName.title = it.election.name

                binding.loadingPb.visibility = View.GONE
                binding.voterInfoLayout.visibility = View.VISIBLE
                viewModel.checkIfElectionExistsInDatabase()
            }
        })

        if (checkLocationPermissions()) {
            checkDeviceLocationOn()
        }

        // Handle loading of URLs

        viewModel.loadLocationsUrl.observe(viewLifecycleOwner, Observer {
            if(!it.isNullOrEmpty()){
                loadUrlIntent(it)
                viewModel.resetLoadLocationsUrl()
            }
        })

        viewModel.loadBallotUrl.observe(viewLifecycleOwner, Observer {
            if(!it.isNullOrEmpty()){
                loadUrlIntent(it)
                viewModel.resetLoadBallotUrl()
            }
        })

        // Handle save button UI state
        // cont'd Handle save button clicks

        binding.followButton.setOnClickListener {
            viewModel.handleSaveButtonClick()
        }

        viewModel.existsInDatabase.observe(viewLifecycleOwner, Observer {
            if (it != null) {
                viewModel.setFollowButtonText(getString(R.string.unfollow_election))
            } else {
                viewModel.setFollowButtonText(getString(R.string.follow_election))
            }
            binding.followButton.visibility = View.VISIBLE
        })


        return binding.root
    }

    // Create method to load URL intents

    private fun loadUrlIntent(url: String) {
        val uri = Uri.parse(url)
        val intent = Intent(Intent.ACTION_VIEW, uri)
        startActivity(intent)
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        // Handle location permission result to get location on permission granted
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
            checkDeviceLocationOn()
        } else {
            Snackbar.make(
                binding.root,
                R.string.permission_denied_explanation,
                Snackbar.LENGTH_INDEFINITE
            )
                .setAction(R.string.settings) {

                    if (isAdded) {
                        startActivity(Intent().apply {
                            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                            data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        })
                    }
                }.show()
        }

    }

    private fun checkLocationPermissions(): Boolean {
        return if (isPermissionGranted()) {
            true
        } else {
            // Request Location permissions
            val permissionsArray = arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
            requestPermissions(
                permissionsArray,
                REQUEST_LOCATION_PERMISSIONS_RESULT_CODE
            )
            false
        }
    }

    private fun isPermissionGranted(): Boolean {
        // Check if permission is already granted and return (true = granted, false = denied/other)
        return (
                PackageManager.PERMISSION_GRANTED ==
                        ActivityCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) && PackageManager.PERMISSION_GRANTED ==
                        ActivityCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ))
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_TURN_DEVICE_LOCATION_ON) {
            checkDeviceLocationOn(false)
        }
    }

    private fun checkDeviceLocationOn(resolve: Boolean = true) {

        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }

        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val settingsClient = LocationServices.getSettingsClient(requireContext())
        val locationSettingsResponseTask =
            settingsClient.checkLocationSettings(builder.build())
        locationSettingsResponseTask.addOnFailureListener { exception ->
            if (exception is ResolvableApiException && resolve) {
                try {
                    startIntentSenderForResult(
                        exception.resolution.intentSender,
                        REQUEST_TURN_DEVICE_LOCATION_ON,
                        null,
                        0,
                        0,
                        0,
                        null
                    )
                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.d("", "Error getting location settings resolution: " + sendEx.message)
                }
            } else {
                Snackbar.make(
                    binding.root,
                    R.string.location_required_error, Snackbar.LENGTH_INDEFINITE
                ).setAction(android.R.string.ok) {
                    if (isAdded) {
                        checkDeviceLocationOn()
                    }
                }.show()
            }
        }
        locationSettingsResponseTask.addOnCompleteListener {
            if (it.isSuccessful) {
                getLocation()
            }
        }

    }

    private fun getLocation() {

        // Get location from LocationServices
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            checkLocationPermissions()
            return
        }

        val fusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(requireActivity())
        val task: Task<Location> = fusedLocationProviderClient.lastLocation
        task.addOnSuccessListener { location ->
            if (location != null) {
                currentAddress = geoCodeLocation(location)
                binding.loadingPb.visibility = View.VISIBLE
                viewModel.fetchVoterInfo(currentAddress, electionId)
            } else {

                val locationRequest = LocationRequest.create()
                locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
                locationRequest.interval = 10 * 1000
                locationRequest.fastestInterval = 5 * 1000

                val locationCallback = object : LocationCallback() {
                    override fun onLocationResult(locationResult: LocationResult) {

                        for (locationItem in locationResult.locations) {
                            currentAddress = geoCodeLocation(locationItem)
                            binding.loadingPb.visibility = View.VISIBLE
                            viewModel.fetchVoterInfo(currentAddress, electionId)
                            fusedLocationProviderClient.removeLocationUpdates(this)
                        }
                    }
                }

                fusedLocationProviderClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    null
                )
            }
        }

    }

    private fun geoCodeLocation(location: Location): Address {
        val geocoder = Geocoder(context, Locale.getDefault())
        return geocoder.getFromLocation(location.latitude, location.longitude, 1)
            .map { address ->
                Address(
                    address.thoroughfare,
                    address.subThoroughfare,
                    address.locality,
                    address.adminArea,
                    address.postalCode
                )
            }
            .first()
    }


}