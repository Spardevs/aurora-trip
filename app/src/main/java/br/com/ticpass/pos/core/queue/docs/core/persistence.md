# Persistence Strategies

Guide to persistence options in the queue management system.

## Overview

The queue system supports flexible persistence strategies that allow you to choose when and how queue items are persisted to storage.

## Available Strategies

### PersistenceStrategy.IMMEDIATE
Items are saved immediately when added or modified.

```kotlin
val queueManager = HybridQueueManager(
    storage = YourQueueStorage(),
    processor = YourQueueProcessor(),
    persistenceStrategy = PersistenceStrategy.IMMEDIATE
)
```

**Use Cases:**
- Critical operations that must survive app crashes
- Payment processing where transaction state must be preserved
- Long-running processes that may be interrupted

**Pros:**
- Maximum durability - no data loss on crashes
- Consistent state between app restarts
- Audit trail for all operations

**Cons:**
- Higher I/O overhead
- Slightly slower enqueue operations
- Requires database/storage setup

### PersistenceStrategy.NEVER
Items are kept in memory only and not persisted.

```kotlin
val queueManager = HybridQueueManager(
    storage = InMemoryQueueStorage(),
    processor = YourQueueProcessor(),
    persistenceStrategy = PersistenceStrategy.NEVER
)
```

**Use Cases:**
- Temporary operations that don't need to survive app restarts
- High-performance scenarios where speed is critical
- Simple workflows with minimal state requirements

**Pros:**
- Maximum performance - no I/O overhead
- Simple setup - no database required
- Fast enqueue/dequeue operations

**Cons:**
- Data loss on app crashes or restarts
- No audit trail
- Not suitable for critical operations

## Storage Implementations

### Room Database Storage
For persistent storage using Android Room:

```kotlin
@Entity(tableName = "queue_items")
data class QueueItemEntity(
    @PrimaryKey val id: String,
    val priority: Int,
    val status: String,
    val data: String // JSON serialized item data
)

@Dao
interface QueueItemDao {
    @Query("SELECT * FROM queue_items WHERE status IN (:statuses)")
    suspend fun getAllByStatus(statuses: List<String>): List<QueueItemEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: QueueItemEntity)
    
    @Update
    suspend fun update(item: QueueItemEntity)
    
    @Delete
    suspend fun delete(item: QueueItemEntity)
}

class RoomQueueStorage<T : QueueItem>(
    private val dao: QueueItemDao,
    private val serializer: QueueItemSerializer<T>
) : QueueStorage<T> {
    
    override suspend fun insert(item: T) {
        val entity = serializer.toEntity(item)
        dao.insert(entity)
    }
    
    override suspend fun getAllByStatus(statuses: List<QueueItemStatus>): List<T> {
        val statusStrings = statuses.map { it.name }
        return dao.getAllByStatus(statusStrings)
            .map { serializer.fromEntity(it) }
    }
    
    // ... other methods
}
```

### In-Memory Storage
For non-persistent storage:

```kotlin
class InMemoryQueueStorage<T : QueueItem> : QueueStorage<T> {
    private val items = mutableListOf<T>()
    private val _itemsFlow = MutableStateFlow<List<T>>(emptyList())
    
    override suspend fun insert(item: T) {
        items.add(item)
        _itemsFlow.value = items.toList()
    }
    
    override suspend fun getAllByStatus(statuses: List<QueueItemStatus>): List<T> {
        return items.filter { it.status in statuses }
    }
    
    override fun observeByStatus(status: QueueItemStatus): Flow<List<T>> {
        return _itemsFlow.map { items ->
            items.filter { it.status == status }
        }
    }
    
    // ... other methods
}
```

## Best Practices

### Choosing the Right Strategy

**Use IMMEDIATE when:**
- Processing critical business operations (payments, orders)
- Queue items represent valuable state that must be preserved
- Users expect operations to survive app restarts
- Compliance requires audit trails

**Use NEVER when:**
- Processing temporary UI operations
- Performance is critical and data loss is acceptable
- Queue items can be easily recreated
- Simple workflows with minimal state

### Performance Considerations

```kotlin
// For high-performance scenarios with IMMEDIATE persistence
class OptimizedQueueStorage<T : QueueItem> : QueueStorage<T> {
    private val batchSize = 10
    private val pendingInserts = mutableListOf<T>()
    
    override suspend fun insert(item: T) {
        pendingInserts.add(item)
        
        if (pendingInserts.size >= batchSize) {
            flushPendingInserts()
        }
    }
    
    private suspend fun flushPendingInserts() {
        if (pendingInserts.isNotEmpty()) {
            dao.insertAll(pendingInserts.map { serializer.toEntity(it) })
            pendingInserts.clear()
        }
    }
}
```

### Error Handling

```kotlin
class ResilientQueueStorage<T : QueueItem>(
    private val primaryStorage: QueueStorage<T>,
    private val fallbackStorage: QueueStorage<T> = InMemoryQueueStorage()
) : QueueStorage<T> {
    
    override suspend fun insert(item: T) {
        try {
            primaryStorage.insert(item)
        } catch (e: Exception) {
            Log.w("QueueStorage", "Primary storage failed, using fallback", e)
            fallbackStorage.insert(item)
        }
    }
    
    // ... other methods with similar fallback logic
}
```

## Migration Strategies

### From NEVER to IMMEDIATE
```kotlin
class MigrationQueueManager<T : QueueItem, E : BaseProcessingEvent>(
    private val inMemoryStorage: InMemoryQueueStorage<T>,
    private val persistentStorage: QueueStorage<T>,
    // ... other parameters
) {
    
    suspend fun migrateToPersistent() {
        val inMemoryItems = inMemoryStorage.getAllByStatus(
            listOf(QueueItemStatus.PENDING, QueueItemStatus.PROCESSING)
        )
        
        inMemoryItems.forEach { item ->
            persistentStorage.insert(item)
        }
        
        // Switch to persistent storage
        // ... update queue manager configuration
    }
}
```

### Cleanup Strategies
```kotlin
class QueueStorageCleanup<T : QueueItem>(
    private val storage: QueueStorage<T>
) {
    
    suspend fun cleanupCompletedItems(olderThanDays: Int = 30) {
        val cutoffDate = System.currentTimeMillis() - (olderThanDays * 24 * 60 * 60 * 1000)
        
        storage.removeByStatus(
            listOf(QueueItemStatus.COMPLETED, QueueItemStatus.CANCELLED)
        )
    }
    
    suspend fun archiveOldItems() {
        // Move old items to archive storage
        // Keep recent items for quick access
    }
}
```

## Monitoring and Debugging

### Storage Metrics
```kotlin
class QueueStorageMetrics<T : QueueItem>(
    private val storage: QueueStorage<T>
) {
    
    suspend fun getStorageStats(): StorageStats {
        val allItems = storage.getAllByStatus(QueueItemStatus.values().toList())
        
        return StorageStats(
            totalItems = allItems.size,
            pendingItems = allItems.count { it.status == QueueItemStatus.PENDING },
            processingItems = allItems.count { it.status == QueueItemStatus.PROCESSING },
            completedItems = allItems.count { it.status == QueueItemStatus.COMPLETED },
            failedItems = allItems.count { it.status == QueueItemStatus.FAILED }
        )
    }
}
```

### Performance Monitoring
```kotlin
class InstrumentedQueueStorage<T : QueueItem>(
    private val delegate: QueueStorage<T>
) : QueueStorage<T> {
    
    override suspend fun insert(item: T) {
        val startTime = System.currentTimeMillis()
        try {
            delegate.insert(item)
        } finally {
            val duration = System.currentTimeMillis() - startTime
            Log.d("QueueStorage", "Insert took ${duration}ms")
        }
    }
    
    // ... other instrumented methods
}
```
