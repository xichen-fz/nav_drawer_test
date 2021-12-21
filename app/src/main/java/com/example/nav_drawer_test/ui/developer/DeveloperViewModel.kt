package com.example.nav_drawer_test.ui.developer

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class DeveloperViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is Developer Fragment"
    }
    val text: LiveData<String> = _text
}