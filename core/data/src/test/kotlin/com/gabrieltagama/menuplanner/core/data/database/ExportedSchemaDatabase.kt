package com.gabrieltagama.menuplanner.core.data.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals

/**
 * Migration test support that runs on the JVM (Robolectric) without MigrationTestHelper: it reads the
 * schema JSON that Room exports to core/data/schemas, creates a database file exactly as that version
 * created it, upgrades it with Migrations.ALL, compares the result with the exported schema of the
 * target version and finally opens it with Room, which validates it against the current entities.
 */
internal class ExportedSchemaDatabase(private val name: String) {

    private val context: Context = ApplicationProvider.getApplicationContext()

    fun create(version: Int, seed: (SQLiteDatabase) -> Unit = {}) {
        context.deleteDatabase(name)
        val file = context.getDatabasePath(name).apply { parentFile?.mkdirs() }
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            createStatements(schemaOf(version)).forEach(db::execSQL)
            db.version = version
            seed(db)
        }
    }

    fun migrateTo(version: Int): SupportSQLiteDatabase {
        val db = openHelper(version).writableDatabase
        assertMatchesSchema(db, schemaOf(version))
        return db
    }

    fun delete() = context.deleteDatabase(name)

    fun openWithRoom(): MenuPlannerDatabase =
        Room.databaseBuilder(context, MenuPlannerDatabase::class.java, name)
            .addMigrations(*Migrations.ALL)
            .allowMainThreadQueries()
            .build()
            .also { it.openHelper.writableDatabase }

    private fun openHelper(version: Int): SupportSQLiteOpenHelper = FrameworkSQLiteOpenHelperFactory().create(
        SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(name)
            .callback(object : SupportSQLiteOpenHelper.Callback(version) {
                override fun onCreate(db: SupportSQLiteDatabase) = error("The database must be created from an exported schema first")

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) =
                    Migrations.ALL
                        .filter { it.startVersion >= oldVersion && it.endVersion <= newVersion }
                        .sortedBy { it.startVersion }
                        .forEach { it.migrate(db) }
            })
            .build()
    )

    private fun createStatements(schema: JsonObject): List<String> {
        val entities = schema.getValue("entities").jsonArray.map { it.jsonObject }
        val tables = entities.map { entity -> entity.string("createSql").withTable(entity.string("tableName")) }
        val indices = entities.flatMap { entity ->
            entity["indices"]?.jsonArray.orEmpty().map { it.jsonObject.string("createSql").withTable(entity.string("tableName")) }
        }
        val setup = schema.getValue("setupQueries").jsonArray.map { it.jsonPrimitive.content }
        return tables + indices + setup
    }

    private fun assertMatchesSchema(db: SupportSQLiteDatabase, schema: JsonObject) =
        schema.getValue("entities").jsonArray.map { it.jsonObject }.forEach { entity ->
            val table = entity.string("tableName")
            assertEquals("columns of $table", expectedColumns(entity), actualColumns(db, table))
            assertEquals("indices of $table", expectedIndices(entity), actualIndices(db, table))
        }

    private fun expectedColumns(entity: JsonObject): Set<Column> =
        entity.getValue("fields").jsonArray.map { it.jsonObject }.map { field ->
            Column(field.string("columnName"), field.string("affinity"), field["notNull"]?.jsonPrimitive?.boolean ?: false)
        }.toSet()

    private fun actualColumns(db: SupportSQLiteDatabase, table: String): Set<Column> =
        db.query("PRAGMA table_info(`$table`)").use { cursor ->
            generateSequence { if (cursor.moveToNext()) cursor else null }
                .map { Column(it.getString(1), it.getString(2), it.getInt(3) == 1) }
                .toSet()
        }

    private fun expectedIndices(entity: JsonObject): Set<String> =
        entity["indices"]?.jsonArray.orEmpty().map { it.jsonObject.string("name") }.toSet()

    private fun actualIndices(db: SupportSQLiteDatabase, table: String): Set<String> =
        db.query("SELECT name FROM sqlite_master WHERE type = 'index' AND tbl_name = '$table' AND name NOT LIKE 'sqlite_autoindex%'").use { cursor ->
            generateSequence { if (cursor.moveToNext()) cursor.getString(0) else null }.toSet()
        }

    private data class Column(val name: String, val affinity: String, val notNull: Boolean)

    private companion object {
        val SCHEMA_DIRECTORY = File("schemas/${MenuPlannerDatabase::class.java.name}")

        fun schemaOf(version: Int): JsonObject =
            Json.parseToJsonElement(File(SCHEMA_DIRECTORY, "$version.json").readText()).jsonObject.getValue("database").jsonObject

        fun JsonObject.string(key: String): String = getValue(key).jsonPrimitive.content

        fun String.withTable(table: String): String = replace("`\${TABLE_NAME}`", "`$table`").replace("\${TABLE_NAME}", "`$table`")
    }
}
