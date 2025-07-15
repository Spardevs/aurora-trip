package br.com.ticpass.pos.data.room.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import br.com.ticpass.pos.data.room.entity.EventEntity
import br.com.ticpass.pos.data.room.entity.PosEntity

@Dao
interface EventDao {

    @Query("SELECT * FROM events")
    suspend fun getAllEvents(): List<EventEntity>

    @Query("SELECT * FROM events WHERE isSelected = 1 LIMIT 1")
    suspend fun getSelectedEvent(): EventEntity

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEvent(pos: EventEntity)


    @Query("SELECT * FROM events WHERE id = :eventId")
    suspend fun getEventById(eventId: String): EventEntity?

    @Update
    suspend fun updateEvent(event: EventEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEvents(events: List<EventEntity>)

    @Delete
    suspend fun removeEvent(event: EventEntity)

    @Query("DELETE FROM events")
    suspend fun clearEvents()
}
