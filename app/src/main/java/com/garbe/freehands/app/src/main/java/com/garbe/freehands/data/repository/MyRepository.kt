package com/garbe/freehands.data.repository

import com/garbe/freehands.data.local.dao.UserDao
import com/garbe/freehands.domain.models.User

class MyRepository(private val userDao: UserDao? = null) {
    fun getData(): List<String> = listOf(""Example"", ""Data"")
    fun getUsers(): List<User> = listOf(User(""1"",""John""))
}
