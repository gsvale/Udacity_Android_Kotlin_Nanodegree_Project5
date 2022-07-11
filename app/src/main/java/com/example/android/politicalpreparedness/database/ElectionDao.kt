package com.example.android.politicalpreparedness.database

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.android.politicalpreparedness.network.models.Election

@Dao
interface ElectionDao {

    //Add insert query
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertElection(vararg election: Election)

    // Add select all election query
    @Query("SELECT * FROM election_table")
    fun getElections(): LiveData<List<Election>>

    // Add select single election query
    @Query("SELECT * FROM election_table WHERE id = :key")
    fun getElection(key: Int): Election?

    // Add delete query
    @Query("DELETE FROM election_table WHERE id = :key")
    fun deleteElection(key: Int)

    // Add clear query
    @Query("DELETE FROM election_table")
    fun clear()

}