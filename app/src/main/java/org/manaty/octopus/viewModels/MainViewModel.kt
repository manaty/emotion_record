package org.manaty.octopus.viewModels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainViewModel : ViewModel(){
//    val counter : MutableLiveData<Int> by lazy {
//        MutableLiveData<Int>()
//    }

    var counter : MutableLiveData<Int> = MutableLiveData() !!
    var serverStatus : MutableLiveData<Boolean> = MutableLiveData() !!
    var headsetStatus : MutableLiveData<Boolean> = MutableLiveData() !!

    init {
        counter.value = 0
        serverStatus.value = false
        headsetStatus.value = false
    }

}