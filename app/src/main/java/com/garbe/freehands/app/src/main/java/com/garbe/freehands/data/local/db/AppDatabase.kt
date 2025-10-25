package com/garbe/freehands.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com/garbe/freehands.data.local.entity.UserEntity
import com/garbe/freehands.data.local.dao.UserDao

@Database(entities = [UserEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
}
