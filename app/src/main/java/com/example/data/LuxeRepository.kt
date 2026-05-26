package com.example.data

import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

class LuxeRepository(private val context: Context) {

    private val database: LuxeDatabase by lazy {
        Room.databaseBuilder(
            context.applicationContext,
            LuxeDatabase::class.java,
            "luxe_flight_db"
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    private val travelerDao by lazy { database.travelerDao() }
    private val bookingDao by lazy { database.bookingDao() }
    private val priceAlertDao by lazy { database.priceAlertDao() }

    init {
        // Prepopulate data in base background thread
        CoroutineScope(Dispatchers.IO).launch {
            try {
                seedInitialData()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Bookings Flows
    val allBookings: Flow<List<BookingEntity>> = bookingDao.getAllBookings()
    val upcomingBookings: Flow<List<BookingEntity>> = bookingDao.getUpcomingBookings()

    suspend fun insertBooking(booking: BookingEntity) {
        bookingDao.insertBooking(booking)
    }

    suspend fun updateBooking(booking: BookingEntity) {
        bookingDao.updateBooking(booking)
    }

    // Travelers Flows
    val allTravelers: Flow<List<TravelerEntity>> = travelerDao.getAllTravelers()

    suspend fun insertTraveler(traveler: TravelerEntity) {
        travelerDao.insertTraveler(traveler)
    }

    suspend fun deleteTraveler(traveler: TravelerEntity) {
        travelerDao.deleteTraveler(traveler)
    }

    // Price Alerts Flows
    val allPriceAlerts: Flow<List<PriceAlertEntity>> = priceAlertDao.getAllAlerts()

    suspend fun insertPriceAlert(alert: PriceAlertEntity) {
        priceAlertDao.insertAlert(alert)
    }

    suspend fun deletePriceAlert(alert: PriceAlertEntity) {
        priceAlertDao.deleteAlert(alert)
    }

    // Flight Search Interface
    fun searchFlights(
        origin: String,
        destination: String,
        cabinClass: String,
        dateMillis: Long,
        passengers: Int
    ): List<Flight> {
        return FlightSearchEngine.getFlights(origin, destination, cabinClass, dateMillis, passengers)
    }

    fun getFareTrends(origin: String, destination: String): List<Pair<String, Double>> {
        return FlightSearchEngine.getFareTrendData(origin, destination)
    }

    // DB Seeder helper to populate neat demo data
    private suspend fun seedInitialData() {
        val currentTravelers = travelerDao.getAllTravelers().first()
        if (currentTravelers.isEmpty()) {
            // Add premium mock travelers
            travelerDao.insertTraveler(TravelerEntity(
                name = "Sir Alistair Thorne",
                email = "alistair.thorne@chamber.org",
                passportNumber = "GBR950183",
                frequentFlyerNumber = "EK-8840291"
            ))
            travelerDao.insertTraveler(TravelerEntity(
                name = "Lady Penelope Vance",
                email = "penelope.vance@chateau.fr",
                passportNumber = "FRA201389",
                frequentFlyerNumber = "AF-309180"
            ))
            travelerDao.insertTraveler(TravelerEntity(
                name = "Johnathan Doe",
                email = "j.doe@luxelife.com",
                passportNumber = "USA10034a",
                frequentFlyerNumber = "BA-112349"
            ))
        }

        val bookings = bookingDao.getAllBookings().first()
        if (bookings.isEmpty()) {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, 3) // Upcoming in 3 days
            val flightDate1Dep = cal.timeInMillis
            val flightDate1Arr = flightDate1Dep + (7 * 3600 * 1000) // 7 hour duration

            cal.add(Calendar.DAY_OF_YEAR, -10) // Completed trip 7 days ago
            val flightDate2Dep = cal.timeInMillis
            val flightDate2Arr = flightDate2Dep + (3 * 3600 * 1000) // 3 hour duration

            // Upcoming
            bookingDao.insertBooking(BookingEntity(
                bookingCode = "AEROLX7A",
                flightNo = "SQ-321",
                airlineName = "Singapore Airlines",
                airlineCode = "SQ",
                departureAirportCode = "SIN",
                departureCity = "Singapore",
                arrivalAirportCode = "LHR",
                arrivalCity = "London",
                departureTime = flightDate1Dep,
                arrivalTime = flightDate1Arr,
                seatNumber = "03A",
                cabinClass = "First Class",
                ticketPrice = 4250.00,
                passengerName = "Sir Alistair Thorne",
                status = "UPCOMING"
            ))

            // Past Completed
            bookingDao.insertBooking(BookingEntity(
                bookingCode = "AEROLX3B",
                flightNo = "EK-201",
                airlineName = "Emirates",
                airlineCode = "EK",
                departureAirportCode = "DXB",
                departureCity = "Dubai",
                arrivalAirportCode = "CDG",
                arrivalCity = "Paris",
                departureTime = flightDate2Dep,
                arrivalTime = flightDate2Arr,
                seatNumber = "12F",
                cabinClass = "Business Class",
                ticketPrice = 2800.00,
                passengerName = "Lady Penelope Vance",
                status = "COMPLETED"
            ))
        }

        val alerts = priceAlertDao.getAllAlerts().first()
        if (alerts.isEmpty()) {
            priceAlertDao.insertAlert(PriceAlertEntity(
                origin = "JFK",
                destination = "DXB",
                alertPrice = 650.00,
                currentPrice = 580.00,
                isTriggered = true
            ))
            priceAlertDao.insertAlert(PriceAlertEntity(
                origin = "LHR",
                destination = "HND",
                alertPrice = 850.00,
                currentPrice = 900.00,
                isTriggered = false
            ))
        }
    }
}
