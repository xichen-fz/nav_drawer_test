package com.example.nav_drawer_test.ui

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

//private const val ENABLE_BLUETOOTH_REQUEST_CODE = 1

class SharedViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "@NavSharedModel.kt"
    }
    val text: LiveData<String> = _text

//    private val _text_home = MutableLiveData<String>().apply {
//        value = "@NavSharedModel: Home Fragment"
//    }
//    val text_home: LiveData<String> = _text_home

//    fun onClickHomeButton (string: String) {
//        _text_home.postValue(string)
//    }
}
