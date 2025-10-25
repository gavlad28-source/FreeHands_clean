package com/garbe/freehands.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com/garbe/freehands.data.local.entity.UserEntity

@Dao
interface UserDao {
    @Insert
    fun insert(user: UserEntity)

    @Query(""SELECT * FROM users"")
    fun getAll(): List<UserEntity>
}
