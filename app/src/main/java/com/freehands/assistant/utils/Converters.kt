package com.freehands.assistant.utils

import androidx.room.TypeConverter
import com.freehands.assistant.data.model.CommandType
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.util.*

/**
 * Type converters for Room database to store complex types as strings.
 */
class Converters {
    private val gson: Gson = GsonBuilder()
        .enableComplexMapKeySerialization()
        .serializeNulls()
        .create()

    // Date converters
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? = value?.let { Date(it) }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? = date?.time

    // String list converters
    @TypeConverter
    fun fromStringList(value: String?): List<String> {
        if (value.isNullOrEmpty()) return emptyList()
        return try {
            val listType = object : TypeToken<List<String>>() {}.type
            gson.fromJson(value, listType) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromListString(list: List<String>?): String {
        return gson.toJson(list ?: emptyList())
    }

    // Map converters
    @TypeConverter
    fun fromStringToMap(value: String?): Map<String, String> {
        if (value.isNullOrEmpty()) return emptyMap()
        return try {
            val mapType = object : TypeToken<Map<String, String>>() {}.type
            gson.fromJson(value, mapType) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    @TypeConverter
    fun fromMapString(map: Map<String, String>?): String {
        return gson.toJson(map ?: emptyMap())
    }

    // CommandType converters
    @TypeConverter
    fun fromCommandType(commandType: CommandType?): String? {
        return commandType?.name
    }
    
    @TypeConverter
    fun toCommandType(value: String?): CommandType? {
        return value?.let { 
            try {
                CommandType.fromString(it)
            } catch (e: IllegalArgumentException) {
                CommandType.UNKNOWN
            }
        }
    }
    
    @TypeConverter
    fun fromCommandTypeList(types: List<CommandType>?): String {
        return gson.toJson(types?.map { it.name } ?: emptyList())
    }
    
    @TypeConverter
    fun toCommandTypeList(value: String?): List<CommandType> {
        if (value.isNullOrEmpty()) return emptyList()
        return try {
            val listType = object : TypeToken<List<String>>() {}.type
            val stringList: List<String> = gson.fromJson(value, listType) ?: emptyList()
            stringList.mapNotNull { CommandType.fromString(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Integer map converters
    @TypeConverter
    fun fromStringToIntMap(value: String?): Map<String, Int> {
        if (value.isNullOrEmpty()) return emptyMap()
        return try {
            val mapType = object : TypeToken<Map<String, Int>>() {}.type
            gson.fromJson(value, mapType) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    @TypeConverter
    fun fromMapStringInt(map: Map<String, Int>?): String {
        return gson.toJson(map ?: emptyMap())
    }

    // Boolean map converters
    @TypeConverter
    fun fromStringToBooleanMap(value: String?): Map<String, Boolean> {
        if (value.isNullOrEmpty()) return emptyMap()
        return try {
            val mapType = object : TypeToken<Map<String, Boolean>>() {}.type
            gson.fromJson(value, mapType) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    @TypeConverter
    fun fromMapStringBoolean(map: Map<String, Boolean>?): String {
        return gson.toJson(map ?: emptyMap())
    }

    // Float map converters
    @TypeConverter
    fun fromStringToFloatMap(value: String?): Map<String, Float> {
        if (value.isNullOrEmpty()) return emptyMap()
        return try {
            val mapType = object : TypeToken<Map<String, Float>>() {}.type
            gson.fromJson(value, mapType) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    @TypeConverter
    fun fromMapStringFloat(map: Map<String, Float>?): String {
        return gson.toJson(map ?: emptyMap())
    }

    // Double map converters
    @TypeConverter
    fun fromStringToDoubleMap(value: String?): Map<String, Double> {
        if (value.isNullOrEmpty()) return emptyMap()
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
