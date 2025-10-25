package com.freehands.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface UserDao {
    @Insert suspend fun insert(user: User)
    @Query(""SELECT * FROM User"") suspend fun getAll(): List<User>
}
