package com.example.android.politicalpreparedness.election

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.android.politicalpreparedness.database.ElectionDatabase
import com.example.android.politicalpreparedness.databinding.FragmentElectionBinding
import com.example.android.politicalpreparedness.election.adapter.ElectionListAdapter
import com.example.android.politicalpreparedness.election.adapter.ElectionListener

class ElectionsFragment : Fragment() {

    // Declare ViewModel
    private lateinit var viewModel: ElectionsViewModel


    private lateinit var binding: FragmentElectionBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val application = requireNotNull(this.activity).application
        val dataSource = ElectionDatabase.getInstance(application).electionDao
        val viewModelFactory = ElectionsViewModelFactory(dataSource)
        viewModel = ViewModelProvider(this, viewModelFactory).get(ElectionsViewModel::class.java)

        // Add binding values
        // Inflate the layout for this fragment
        binding = FragmentElectionBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel

        // Link elections to voter info

        val clickListener = ElectionListener { election ->
            this.findNavController().navigate(
                ElectionsFragmentDirections.actionElectionsFragmentToVoterInfoFragment(
                    election.id,
                    election.division
                )
            )
        }

        // Initiate recycler adapters

        val upcomingElectionsAdapter = ElectionListAdapter(clickListener)
        binding.upcomingElectionsRecycler.adapter = upcomingElectionsAdapter

        val savedElectionsAdapter = ElectionListAdapter(clickListener)
        binding.savedElectionsRecycler.adapter = savedElectionsAdapter


        // Populate recycler adapters

        viewModel.upcomingElections.observe(viewLifecycleOwner, Observer {
            it?.let {
                if(it.isEmpty()){
                    upcomingElectionsAdapter.addItems(ArrayList())
                }
                else{
                    upcomingElectionsAdapter.addItems(it)
                }
            }
            binding.upcomingElectionsPb.visibility = View.GONE
        })

        viewModel.savedElections.observe(viewLifecycleOwner, Observer {
            it?.let {
                if(it.isEmpty()){
                    savedElectionsAdapter.addItems(ArrayList())
                }
                else{
                    savedElectionsAdapter.addItems(it)
                }
            }
            binding.savedElectionsPb.visibility = View.GONE
        })

        loadElections()

        return binding.root
    }

    // Refresh adapters when fragment loads
    private fun loadElections() {
        viewModel.fetchUpcomingElections()
    }

}