package com/garbe/freehands.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = ""users"")
data class UserEntity(
    @PrimaryKey val id: String,
    val name: String
)
