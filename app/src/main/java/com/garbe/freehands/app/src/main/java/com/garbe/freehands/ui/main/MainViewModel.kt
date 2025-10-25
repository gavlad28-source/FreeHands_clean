package com/garbe/freehands.ui.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com/garbe/freehands.data.repository.MyRepository
import com/garbe/freehands.domain.models.User

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: MyRepository
) : ViewModel() {

    val data = MutableLiveData<List<String>>()
    val users = MutableLiveData<List<User>>()

    fun loadData() {
        data.value = repository.getData()
        users.value = repository.getUsers()
    }
}
