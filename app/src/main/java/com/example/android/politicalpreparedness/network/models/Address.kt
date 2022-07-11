package com.example.android.politicalpreparedness.network.models

import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import com.example.android.politicalpreparedness.BR

data class Address(
    private var line1: String,
    private var line2: String? = null,
    private var city: String,
    private var state: String,
    private var zip: String
) : BaseObservable() {


    var _line1: String
        @Bindable get() = line1
        set(value) {
            line1 = value
            notifyPropertyChanged(BR._line1)
        }

    var _line2: String
        @Bindable get() = line2!!
        set(value) {
            line2 = value
            notifyPropertyChanged(BR._line2)
        }

    var _city: String
        @Bindable get() = city
        set(value) {
            city = value
            notifyPropertyChanged(BR._city)
        }

    var _state: String
        @Bindable get() = state
        set(value) {
            state = value
            notifyPropertyChanged(BR._state)
        }

    var _zip: String
        @Bindable get() = zip
        set(value) {
            zip = value
            notifyPropertyChanged(BR._zip)
        }


    fun toFormattedString(): String {
        var output = line1.plus("\n")
        if (!line2.isNullOrEmpty()) output = output.plus(line2).plus("\n")
        output = output.plus("$city, $state $zip")
        return output
    }
}