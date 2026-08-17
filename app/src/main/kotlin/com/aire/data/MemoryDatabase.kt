package com.aire.data

import android.content.Context
import androidx.room.*
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
    val imagePath: String? = null
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
        imagePath = imagePath
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
            imagePath = record.imagePath
        )
    }
}

@Entity(tableName = "memories_fts")
@Fts4(contentEntity = MemoryRecordEntity::class)
data class MemoryRecordFts(
    val title: String,
    val summary: String,
    val tagsJson: String
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

@Database(entities = [MemoryRecordEntity::class, MemoryRecordFts::class], version = 2, exportSchema = false)
abstract class MemoryDatabase : RoomDatabase() {
    abstract fun dao(): MemoryDao

    companion object {
        @Volatile
        private var INSTANCE: MemoryDatabase? = null

        fun get(context: Context): MemoryDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MemoryDatabase::class.java,
                    "aire_memories"
                )
                .fallbackToDestructiveMigration()
                .build().also { INSTANCE = it }
            }
    }
}
