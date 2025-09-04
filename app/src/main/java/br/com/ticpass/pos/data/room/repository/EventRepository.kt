package br.com.ticpass.pos.data.room.repository

import br.com.ticpass.pos.data.room.dao.EventDao
import br.com.ticpass.pos.data.room.entity.EventEntity
import br.com.ticpass.pos.data.room.entity.PosEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventRepository @Inject constructor(
    private val eventDao: EventDao
) {

    suspend fun insertMany(events: List<EventEntity>) {
        return eventDao.insertEvents(events)
    }

    suspend fun getAllEvents() = eventDao.getAllEvents()

    suspend fun getSelectedEvent(): EventEntity? {
        return try {
            eventDao.getSelectedEvent().takeIf { true }
        } catch (e: Exception) {
            null
        }
    }
    suspend fun selectEvent(eventId: String) {
        // Desmarca todos os eventos
        eventDao.deselectAllEvents()

        // Marca o evento específico como selecionado
        eventDao.selectEvent(eventId)
    }

    suspend fun updateMany(events: List<EventEntity>) {
        if (events.isEmpty()) return

        events.forEach { event ->
            eventDao.updateEvent(event)
        }
    }

    suspend fun upsertEvent(event: EventEntity) = eventDao.upsertEvent(event)


    suspend fun unSelectEvent(eventId: String) {

        val oldEvent = eventDao.getSelectedEvent()

        if(oldEvent != null) {
            oldEvent.isSelected = false
            eventDao.updateEvent(oldEvent)
        }

        val eventToUpdate = eventDao.getEventById(eventId)

        if (eventToUpdate != null) {
            eventToUpdate.isSelected = false
            eventDao.updateEvent(eventToUpdate)
        }
    }

    suspend fun toggleTicketType(): String {
        val currEvent = eventDao.getSelectedEvent() as EventEntity
        val ticketFormat = currEvent.ticketFormat

        val formarts = listOf("default", "compact", "grouped")
        val currentIndex = formarts.indexOf(ticketFormat)

        if (currentIndex == -1) {
            throw IllegalArgumentException("Value not found in the list")
        }

        val nextIndex = (currentIndex + 1) % formarts.size
        val newValue = formarts[nextIndex]

        currEvent.ticketFormat = newValue
        eventDao.updateEvent(currEvent)

        return newValue
    }

    suspend fun clearAll() = eventDao.clearEvents()

    companion object {

        // For Singleton instantiation
        @Volatile private var instance: EventRepository? = null

        fun getInstance(eventDao: EventDao) =
            instance ?: synchronized(this) {
                instance ?: EventRepository(eventDao).also { instance = it }
            }
    }

    suspend fun getFirstEvent(): EventEntity? {
        return try {
            eventDao.getFirstEvent()
        } catch (e: Exception) {
            null
        }
    }

}
