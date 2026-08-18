package com.aire.data

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.aire.domain.MemoryCategory
import com.aire.domain.MemoryRecord
import com.aire.domain.SourceType
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Entity(tableName = "memories")
data class MemoryRecordEntity(
    @PrimaryKey val id: String,
    val category: String,
    val title: String,
    val summary: String,
    val occurredOn: String?,
    val attributesJson: String,
    val tagsJson: String,
    val capturedAt: Long,
    val sourceText: String,
    val sourceType: String,
    val imagePath: String? = null,
    val locationName: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
) {
    fun toDomain(): MemoryRecord = MemoryRecord(
        id = id,
        category = MemoryCategory.valueOf(category),
        title = title,
        summary = summary,
        occurredOn = occurredOn,
        attributes = Json.decodeFromString(attributesJson),
        tags = Json.decodeFromString(tagsJson),
        capturedAt = capturedAt,
        sourceText = sourceText,
        sourceType = SourceType.valueOf(sourceType),
        imagePath = imagePath,
        locationName = locationName,
        latitude = latitude,
        longitude = longitude
    )

    companion object {
        fun fromDomain(record: MemoryRecord): MemoryRecordEntity = MemoryRecordEntity(
            id = record.id,
            category = record.category.name,
            title = record.title,
            summary = record.summary,
            occurredOn = record.occurredOn,
            attributesJson = Json.encodeToString(record.attributes),
            tagsJson = Json.encodeToString(record.tags),
            capturedAt = record.capturedAt,
            sourceText = record.sourceText,
            sourceType = record.sourceType.name,
            imagePath = record.imagePath,
            locationName = record.locationName,
            latitude = record.latitude,
            longitude = record.longitude
        )
    }
}

@Entity(tableName = "memories_fts")
@Fts4(contentEntity = MemoryRecordEntity::class)
data class MemoryRecordFts(
    val title: String,
    val summary: String,
    val tagsJson: String,
    val locationName: String?
)

@Dao
interface MemoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: MemoryRecordEntity)

    @Query("SELECT * FROM memories ORDER BY capturedAt DESC")
    fun observeAll(): Flow<List<MemoryRecordEntity>>

    @Query("SELECT * FROM memories ORDER BY capturedAt DESC")
    suspend fun getAll(): List<MemoryRecordEntity>

    @Query("""
        SELECT memories.* FROM memories
        JOIN memories_fts ON memories.rowid = memories_fts.docid
        WHERE memories_fts MATCH :query
        LIMIT :limit
    """)
    suspend fun search(query: String, limit: Int = 20): List<MemoryRecordEntity>
}

@Database(entities = [MemoryRecordEntity::class, MemoryRecordFts::class], version = 3, exportSchema = false)
abstract class MemoryDatabase : RoomDatabase() {
    abstract fun dao(): MemoryDao

    companion object {
        @Volatile
        private var INSTANCE: MemoryDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE memories ADD COLUMN imagePath TEXT")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Add new columns to the main table
                db.execSQL("ALTER TABLE memories ADD COLUMN locationName TEXT")
                db.execSQL("ALTER TABLE memories ADD COLUMN latitude REAL")
                db.execSQL("ALTER TABLE memories ADD COLUMN longitude REAL")

                // 2. Rebuild the FTS table to include the new locationName column
                // SQLite FTS tables don't support ALTER TABLE to add columns.
                db.execSQL("DROP TABLE IF EXISTS memories_fts")
                db.execSQL("""
                    CREATE VIRTUAL TABLE memories_fts USING fts4(
                        content=`memories`,
                        summary,
                        locationName,
                        title,
                        tagsJson
                    )
                """)
                db.execSQL("INSERT INTO memories_fts(memories_fts) VALUES('rebuild')")
            }
        }

        fun get(context: Context): MemoryDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MemoryDatabase::class.java,
                    "aire_memories"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build().also { INSTANCE = it }
            }
    }
}
