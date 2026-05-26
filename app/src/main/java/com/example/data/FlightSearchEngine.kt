package com.example.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.random.Random

data class Flight(
    val id: String,
    val airlineName: String,
    val airlineCode: String,
    val flightNo: String,
    val departureCity: String,
    val departureAirportCode: String,
    val arrivalCity: String,
    val arrivalAirportCode: String,
    val departureTime: Long,
    val arrivalTime: Long,
    val durationMinutes: Int,
    val price: Double,
    val stops: Int,
    val stopsInfo: String,
    val cabinClass: String,
    val baggagePolicy: String,
    val ecoImpactPercentage: Int, // e.g. 15% better than average
    val seatAvailability: Int,
    val seatLayout: Map<String, Boolean> // True if occupied, False if free
)

object FlightSearchEngine {

    val POPULAR_AIRPORTS = listOf(
        Airport("London", "LHR", "London Heathrow"),
        Airport("Paris", "CDG", "Paris Charles de Gaulle"),
        Airport("New York", "JFK", "John F. Kennedy Intl"),
        Airport("Tokyo", "HND", "Tokyo Haneda"),
        Airport("Dubai", "DXB", "Dubai International"),
        Airport("Singapore", "SIN", "Singapore Changi"),
        Airport("Sydney", "SYD", "Sydney Kingsford Smith"),
        Airport("Rome", "FCO", "Leonardo da Vinci–Fiumicino"),
        Airport("Los Angeles", "LAX", "Los Angeles Intl"),
        Airport("Zurich", "ZRH", "Zurich Airport")
    )

    data class Airport(val city: String, val code: String, val name: String)

    val AIRLINES = listOf(
        Airline("Emirates", "EK", 0xFF8A0000), // Crimson Maroon
        Airline("Singapore Airlines", "SQ", 0xFF002244), // Midnight Blue
        Airline("Qatar Airways", "QR", 0xFF5A0025), // Burgundy
        Airline("Lufthansa", "LH", 0xFFF7A200), // Amber Gold
        Airline("Japan Airlines", "JL", 0xFFE1000B), // Vibrant Red
        Airline("British Airways", "BA", 0xFF003366), // Royal Blue
        Airline("Swiss Intl Air Lines", "LX", 0xFFD20A11), // Red Swiss
        Airline("Air France", "AF", 0xFF002060) // French Blue
    )

    data class Airline(val name: String, val code: String, val colorHex: Long)

    fun fetchNearbyAirports(city: String): List<Airport> {
        val normalized = city.trim().uppercase()
        return POPULAR_AIRPORTS.filter {
            it.city.uppercase().contains(normalized) || it.code.contains(normalized)
        }.ifEmpty {
            listOf(Airport(city, city.take(3).uppercase() + "X", "$city Airport"))
        }
    }

    // Dynamic generation based on source & destination with stable prices using seeds
    fun getFlights(
        origin: String,
        destination: String,
        cabinClass: String,
        dateMillis: Long,
        passengers: Int
    ): List<Flight> {
        val cleanOrigin = origin.trim().uppercase().ifEmpty { "JFK" }
        val cleanDest = destination.trim().uppercase().ifEmpty { "LHR" }
        
        // Find matching airport names
        val origAir = POPULAR_AIRPORTS.find { it.code == cleanOrigin || it.city.uppercase() == cleanOrigin } ?: Airport(origin, cleanOrigin.take(3), "$origin Airport")
        val destAir = POPULAR_AIRPORTS.find { it.code == cleanDest || it.city.uppercase() == cleanDest } ?: Airport(destination, cleanDest.take(3), "$destination Airport")

        if (origAir.code == destAir.code) {
            return emptyList()
        }

        val flights = mutableListOf<Flight>()

        // Seed generator with date + origin + destination codes to keep search reproducible
        val seed = (cleanOrigin.hashCode() + cleanDest.hashCode() + (dateMillis / 86400000)).toLong()
        val random = Random(seed)

        // Base distance calculation for price & flight duration
        val charDiff = Math.abs(cleanOrigin[0].code - cleanDest[0].code)
        val baseDistanceHours = if (charDiff == 0) 3 else charDiff + 2
        val baseDurationMinutes = baseDistanceHours * 60 + random.nextInt(0, 45)

        // Generate 4 to 8 flights
        val numFlights = random.nextInt(4, 8)
        
        val multiplier = when (cabinClass.uppercase()) {
            "PREMIUM" -> 1.5
            "BUSINESS" -> 2.8
            "FIRST" -> 5.0
            else -> 1.0 // Economy
        }

        val basePrice = (120 + baseDistanceHours * 90) * multiplier

        for (i in 0 until numFlights) {
            val airline = AIRLINES[random.nextInt(AIRLINES.size)]
            val flightNum = airline.code + "-" + random.nextInt(100, 999)
            
            // Random schedule spacing
            val departureOffsetMinutes = 300 + (i * 180) + random.nextInt(-45, 45)
            val cal = Calendar.getInstance().apply {
                timeInMillis = dateMillis
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                add(Calendar.MINUTE, departureOffsetMinutes)
            }
            
            val depTime = cal.timeInMillis
            val arrTime = depTime + (baseDurationMinutes * 60 * 1000)

            val stopsValue = if (baseDistanceHours < 5) {
                0
            } else {
                if (random.nextBoolean()) 0 else random.nextInt(1, 3)
            }

            val stopsText = when (stopsValue) {
                0 -> "Direct"
                1 -> "1 stop via ${if (cleanOrigin == "DXB") "SIN" else "DXB"}"
                else -> "2 stops via CDG, HND"
            }

            // High-end pricing fluctuation
            val priceFluc = (random.nextDouble(-0.15, 0.25) * basePrice)
            val ticketPrice = Math.round((basePrice + priceFluc) * passengers * 100) / 100.0

            val bagsPolicy = when (cabinClass.uppercase()) {
                "FIRST" -> "3x 32kg Checked + 2 Cabin Bags"
                "BUSINESS" -> "2x 32kg Checked + 2 Cabin Bags"
                "PREMIUM" -> "2x 23kg Checked + 1 Cabin Bag"
                else -> "1x 23kg Checked + 1 Cabin Bag"
            }

            val ecoReduction = random.nextInt(2, 28)
            val seatAvail = random.nextInt(1, 15)

            // Setup seat layout: rows 1 to 30, seats A to F
            val seatMap = mutableMapOf<String, Boolean>()
            val rows = when (cabinClass.uppercase()) {
                "FIRST" -> 4
                "BUSINESS" -> 8
                "PREMIUM" -> 12
                else -> 30
            }
            for (row in 1..rows) {
                for (col in listOf("A", "B", "C", "D", "E", "F")) {
                    val seatCode = "$row$col"
                    // Generate pseudo-random availability so a few seats are occupied
                    val occupied = (seed + row.hashCode() + col.hashCode() + i) % 3 == 0L
                    seatMap[seatCode] = occupied
                }
            }

            flights.add(
                Flight(
                    id = "$flightNum-$i",
                    airlineName = airline.name,
                    airlineCode = airline.code,
                    flightNo = flightNum,
                    departureCity = origAir.city,
                    departureAirportCode = origAir.code,
                    arrivalCity = destAir.city,
                    arrivalAirportCode = destAir.code,
                    departureTime = depTime,
                    arrivalTime = arrTime,
                    durationMinutes = baseDurationMinutes,
                    price = ticketPrice,
                    stops = stopsValue,
                    stopsInfo = stopsText,
                    cabinClass = cabinClass,
                    baggagePolicy = bagsPolicy,
                    ecoImpactPercentage = ecoReduction,
                    seatAvailability = seatAvail,
                    seatLayout = seatMap
                )
            )
        }

        return flights.sortedBy { it.price }
    }

    // Dynamic historical price comparison for the next 7 days
    fun getFareTrendData(origin: String, destination: String): List<Pair<String, Double>> {
        val cleanOrigin = origin.uppercase().ifEmpty { "JFK" }
        val cleanDest = destination.uppercase().ifEmpty { "LHR" }
        val seed = (cleanOrigin.hashCode() + cleanDest.hashCode()).toLong()
        val random = Random(seed)

        val calendar = Calendar.getInstance()
        val simpleDateFormat = SimpleDateFormat("EEE dd", Locale.US)
        
        val list = mutableListOf<Pair<String, Double>>()
        val basePrice = 300.0 + random.nextInt(100, 400)

        for (i in 0 until 7) {
            val label = simpleDateFormat.format(calendar.time)
            val priceFactor = 1.0 + random.nextDouble(-0.15, 0.40)
            list.add(label to Math.round(basePrice * priceFactor * 100.0) / 100.0)
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        return list
    }
}
