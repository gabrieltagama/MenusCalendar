package com.gabrieltagama.menuplanner.core.data.database

import androidx.room.TypeConverter
import java.time.Instant
import java.time.LocalDate

/**
 * Room column conversions shared by every table, projection and query parameter: a calendar day is
 * stored as its epoch day and an instant as its epoch millis, so rows, DAOs and mappers work with
 * LocalDate and Instant directly. Enums need no converter: Room stores them by name, which keeps
 * the existing TEXT columns.
 */
internal object Converters {

    @TypeConverter
    fun localDateToEpochDay(date: LocalDate?): Long? = date?.toEpochDay()

    @TypeConverter
    fun epochDayToLocalDate(epochDay: Long?): LocalDate? = epochDay?.let(LocalDate::ofEpochDay)

    @TypeConverter
    fun instantToEpochMilli(instant: Instant?): Long? = instant?.toEpochMilli()

    @TypeConverter
    fun epochMilliToInstant(epochMilli: Long?): Instant? = epochMilli?.let(Instant::ofEpochMilli)
}
