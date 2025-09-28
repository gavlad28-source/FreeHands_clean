package com.freehands.assistant.utils

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*

/**
 * Type converters for Room database to store complex types as strings.
 */
class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun fromStringList(value: String?): List<String> {
        if (value == null) {
            return emptyList()
        }
        val listType = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, listType) ?: emptyList()
    }

    @TypeConverter
    fun fromListString(list: List<String>?): String {
        return gson.toJson(list ?: emptyList())
    }

    @TypeConverter
    fun fromStringToMap(value: String?): Map<String, String> {
        if (value.isNullOrEmpty()) {
            return emptyMap()
        }
        val mapType = object : TypeToken<Map<String, String>>() {}.type
        return gson.fromJson(value, mapType) ?: emptyMap()
    }

    @TypeConverter
    fun fromMapString(map: Map<String, String>?): String {
        return gson.toJson(map ?: emptyMap<String, String>())
    }
    
    /**
     * Converts a CommandType enum to a String for storage in the database.
     */
    @TypeConverter
    fun fromCommandType(commandType: CommandType?): String {
        return commandType?.name ?: CommandType.UNKNOWN.name
    }
    
    /**
     * Converts a String from the database back to a CommandType enum.
     */
    @TypeConverter
    fun toCommandType(value: String?): CommandType {
        return try {
            CommandType.valueOf(value ?: return CommandType.UNKNOWN)
        } catch (e: IllegalArgumentException) {
            CommandType.UNKNOWN
        }
    }
    
    /**
     * Converts a list of CommandType enums to a JSON string for storage.
     */
    @TypeConverter
    fun fromCommandTypeList(commands: List<CommandType>?): String {
        return gson.toJson(commands?.map { it.name } ?: emptyList())
    }
    
    /**
     * Converts a JSON string from the database back to a list of CommandType enums.
     */
    @TypeConverter
    fun toCommandTypeList(value: String?): List<CommandType> {
        if (value.isNullOrEmpty()) return emptyList()
        return try {
            val listType = object : TypeToken<List<String>>() {}.type
            val stringList: List<String> = gson.fromJson(value, listType) ?: return emptyList()
            stringList.mapNotNull { type -> CommandType.values().find { it.name == type } }
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromStringToIntMap(value: String?): Map<String, Int> {
        if (value.isNullOrEmpty()) {
            return emptyMap()
        }
        val mapType = object : TypeToken<Map<String, Int>>() {}.type
        return gson.fromJson(value, mapType) ?: emptyMap()
    }

    @TypeConverter
    fun fromMapStringInt(map: Map<String, Int>?): String {
        return gson.toJson(map ?: emptyMap<String, Int>())
    }

    @TypeConverter
    fun fromStringToBooleanMap(value: String?): Map<String, Boolean> {
        if (value.isNullOrEmpty()) {
            return emptyMap()
        }
        val mapType = object : TypeToken<Map<String, Boolean>>() {}.type
        return gson.fromJson(value, mapType) ?: emptyMap()
    }

    @TypeConverter
    fun fromMapStringBoolean(map: Map<String, Boolean>?): String {
        return gson.toJson(map ?: emptyMap<String, Boolean>())
    }

    @TypeConverter
    fun fromStringToFloatMap(value: String?): Map<String, Float> {
        if (value.isNullOrEmpty()) {
            return emptyMap()
        }
        val mapType = object : TypeToken<Map<String, Float>>() {}.type
        return gson.fromJson(value, mapType) ?: emptyMap()
    }

    @TypeConverter
    fun fromMapStringFloat(map: Map<String, Float>?): String {
        return gson.toJson(map ?: emptyMap<String, Float>())
    }

    @TypeConverter
    fun fromStringToDoubleMap(value: String?): Map<String, Double> {
        if (value.isNullOrEmpty()) {
            return emptyMap()
        }
        val mapType = object : TypeToken<Map<String, Double>>() {}.type
        return gson.fromJson(value, mapType) ?: emptyMap()
    }

    @TypeConverter
    fun fromMapStringDouble(map: Map<String, Double>?): String {
        return gson.toJson(map ?: emptyMap<String, Double>())
    }

    @TypeConverter
    fun fromStringToLongMap(value: String?): Map<String, Long> {
        if (value.isNullOrEmpty()) {
            return emptyMap()
        }
        val mapType = object : TypeToken<Map<String, Long>>() {}.type
        return gson.fromJson(value, mapType) ?: emptyMap()
    }

    @TypeConverter
    fun fromMapStringLong(map: Map<String, Long>?): String {
        return gson.toJson(map ?: emptyMap<String, Long>())
    }
}
