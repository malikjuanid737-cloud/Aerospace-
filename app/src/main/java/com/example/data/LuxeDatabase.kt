package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// --- entities ---

@Entity(tableName = "saved_travelers")
data class TravelerEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val email: String,
    val passportNumber: String,
    val frequentFlyerNumber: String = ""
)

@Entity(tableName = "bookings")
data class BookingEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val bookingCode: String,
    val flightNo: String,
    val airlineName: String,
    val airlineCode: String,
    val departureAirportCode: String,
    val departureCity: String,
    val arrivalAirportCode: String,
    val arrivalCity: String,
    val departureTime: Long, // Epoch millis
    val arrivalTime: Long, // Epoch millis
    val seatNumber: String,
    val cabinClass: String, // Economy, Premium, Business, First
    val ticketPrice: Double,
    val passengerName: String,
    val bookingTime: Long = System.currentTimeMillis(),
    val status: String // UPCOMING, COMPLETED, CANCELLED
)

@Entity(tableName = "price_alerts")
data class PriceAlertEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val origin: String,
    val destination: String,
    val alertPrice: Double,
    val currentPrice: Double,
    val isTriggered: Boolean = false
)

// --- daos ---

@Dao
interface TravelerDao {
    @Query("SELECT * FROM saved_travelers ORDER BY name ASC")
    fun getAllTravelers(): Flow<List<TravelerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTraveler(traveler: TravelerEntity)

    @Delete
    suspend fun deleteTraveler(traveler: TravelerEntity)
}

@Dao
interface BookingDao {
    @Query("SELECT * FROM bookings ORDER BY departureTime ASC")
    fun getAllBookings(): Flow<List<BookingEntity>>

    @Query("SELECT * FROM bookings WHERE status = 'UPCOMING' ORDER BY departureTime ASC")
    fun getUpcomingBookings(): Flow<List<BookingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooking(booking: BookingEntity)

    @Update
    suspend fun updateBooking(booking: BookingEntity)
}

@Dao
interface PriceAlertDao {
    @Query("SELECT * FROM price_alerts ORDER BY id DESC")
    fun getAllAlerts(): Flow<List<PriceAlertEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: PriceAlertEntity)

    @Delete
    suspend fun deleteAlert(alert: PriceAlertEntity)
}

// --- database ---

@Database(
    entities = [TravelerEntity::class, BookingEntity::class, PriceAlertEntity::class],
    version = 1,
    exportSchema = false
)
abstract class LuxeDatabase : RoomDatabase() {
    abstract fun travelerDao(): TravelerDao
    abstract fun bookingDao(): BookingDao
    abstract fun priceAlertDao(): PriceAlertDao
}
