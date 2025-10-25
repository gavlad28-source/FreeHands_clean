package com.freehands.app.data.repository

import com.freehands.app.data.local.UserDao
import com.freehands.app.data.local.User
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(private val dao: UserDao) {
    suspend fun addUser(user: User) = dao.insert(user)
    suspend fun getUsers() = dao.getAll()
}
