package com.example.nav_drawer_test.ui.datalist

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class DatalistViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is Datalist Fragment"
    }
    val text: LiveData<String> = _text
}