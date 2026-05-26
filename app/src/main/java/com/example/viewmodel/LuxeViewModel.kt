package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.BookingEntity
import com.example.data.Flight
import com.example.data.LuxeRepository
import com.example.data.PriceAlertEntity
import com.example.data.TravelerEntity
import com.example.network.AiRecommendation
import com.example.network.GeminiManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore

enum class DashboardTab {
    EXPLORE,
    SEARCH,
    MY_TRIPS,
    SUPPORT,
    PROFILE
}

enum class BookingStep {
    SEARCH_INPUT,
    FLIGHT_RESULTS,
    PASSENGER_DETAILS,
    SEAT_SELECTION,
    ADD_ONS,
    PAYMENT,
    CONFIRMATION
}

data class NotificationModel(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String,
    val timestamp: Long = System.currentTimeMillis()
)

class LuxeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = LuxeRepository(application)

    // Firebase Authentication & Firestore Integration fields
    private var firebaseAuth: FirebaseAuth? = null
    private var firebaseDb: FirebaseFirestore? = null

    private val _isFirebaseAvailable = MutableStateFlow(false)
    val isFirebaseAvailable: StateFlow<Boolean> = _isFirebaseAvailable.asStateFlow()

    private val _isFirestoreSyncing = MutableStateFlow(false)
    val isFirestoreSyncing: StateFlow<Boolean> = _isFirestoreSyncing.asStateFlow()

    private val _firestoreSyncStatus = MutableStateFlow("Sandbox mode. Authenticate to sync with cloud.")
    val firestoreSyncStatus: StateFlow<String> = _firestoreSyncStatus.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _isAuthLoading = MutableStateFlow(false)
    val isAuthLoading: StateFlow<Boolean> = _isAuthLoading.asStateFlow()

    // App Navigation & Session States
    private val _onboardingCompleted = MutableStateFlow(false)
    val onboardingCompleted: StateFlow<Boolean> = _onboardingCompleted.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _currentTab = MutableStateFlow(DashboardTab.EXPLORE)
    val currentTab: StateFlow<DashboardTab> = _currentTab.asStateFlow()

    // Current User Profile Specifics
    private val _userName = MutableStateFlow("Sir Alistair Thorne")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _userEmail = MutableStateFlow("alistair.thorne@chamber.org")
    val userEmail: StateFlow<String> = _userEmail.asStateFlow()

    private val _userPassport = MutableStateFlow("GBR950183")
    val userPassport: StateFlow<String> = _userPassport.asStateFlow()

    private val _userFlyerNumber = MutableStateFlow("EK-8840291")
    val userFlyerNumber: StateFlow<String> = _userFlyerNumber.asStateFlow()

    private val _selectedCurrency = MutableStateFlow("USD ($)")
    val selectedCurrency: StateFlow<String> = _selectedCurrency.asStateFlow()

    private val _selectedLanguage = MutableStateFlow("English (US)")
    val selectedLanguage: StateFlow<String> = _selectedLanguage.asStateFlow()

    private val _isDarkMode = MutableStateFlow(true)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    // DB Bindings
    val allBookings: StateFlow<List<BookingEntity>> = repository.allBookings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val upcomingBookings: StateFlow<List<BookingEntity>> = repository.upcomingBookings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedTravelers: StateFlow<List<TravelerEntity>> = repository.allTravelers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val priceAlerts: StateFlow<List<PriceAlertEntity>> = repository.allPriceAlerts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Search Engine parameters
    private val _searchOrigin = MutableStateFlow("SIN")
    val searchOrigin: StateFlow<String> = _searchOrigin.asStateFlow()

    private val _searchDestination = MutableStateFlow("LHR")
    val searchDestination: StateFlow<String> = _searchDestination.asStateFlow()

    private val _searchCabinClass = MutableStateFlow("Business")
    val searchCabinClass: StateFlow<String> = _searchCabinClass.asStateFlow()

    private val _searchDateMills = MutableStateFlow(Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 7) }.timeInMillis)
    val searchDateMills: StateFlow<Long> = _searchDateMills.asStateFlow()

    private val _searchPassengers = MutableStateFlow(1)
    val searchPassengers: StateFlow<Int> = _searchPassengers.asStateFlow()

    private val _isRoundTrip = MutableStateFlow(true)
    val isRoundTrip: StateFlow<Boolean> = _isRoundTrip.asStateFlow()

    private val _predictOriginResults = MutableStateFlow<List<String>>(emptyList())
    val predictOriginResults: StateFlow<List<String>> = _predictOriginResults.asStateFlow()

    private val _predictDestResults = MutableStateFlow<List<String>>(emptyList())
    val predictDestResults: StateFlow<List<String>> = _predictDestResults.asStateFlow()

    // Booking Flow State Machine
    private val _bookingStep = MutableStateFlow(BookingStep.SEARCH_INPUT)
    val bookingStep: StateFlow<BookingStep> = _bookingStep.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Flight>>(emptyList())
    val searchResults: StateFlow<List<Flight>> = _searchResults.asStateFlow()

    private val _fareTrends = MutableStateFlow<List<Pair<String, Double>>>(emptyList())
    val fareTrends: StateFlow<List<Pair<String, Double>>> = _fareTrends.asStateFlow()

    private val _selectedFlight = MutableStateFlow<Flight?>(null)
    val selectedFlight: StateFlow<Flight?> = _selectedFlight.asStateFlow()

    // Filter/Sort State
    private val _resultsSortBy = MutableStateFlow("Price") // Price, Duration, Eco
    val resultsSortBy: StateFlow<String> = _resultsSortBy.asStateFlow()

    // Passenger custom details
    private val _passengerNameInput = MutableStateFlow("")
    val passengerNameInput: StateFlow<String> = _passengerNameInput.asStateFlow()

    private val _passengerEmailInput = MutableStateFlow("")
    val passengerEmailInput: StateFlow<String> = _passengerEmailInput.asStateFlow()

    private val _passengerPassportInput = MutableStateFlow("")
    val passengerPassportInput: StateFlow<String> = _passengerPassportInput.asStateFlow()

    private val _passengerFlyerInput = MutableStateFlow("")
    val passengerFlyerInput: StateFlow<String> = _passengerFlyerInput.asStateFlow()

    // Seat selection State
    private val _selectedSeatCode = MutableStateFlow("12A")
    val selectedSeatCode: StateFlow<String> = _selectedSeatCode.asStateFlow()

    // Addons State
    private val _addonBaggage = MutableStateFlow(false)
    val addonBaggage: StateFlow<Boolean> = _addonBaggage.asStateFlow()

    private val _addonMeal = MutableStateFlow(false)
    val addonMeal: StateFlow<Boolean> = _addonMeal.asStateFlow()

    private val _addonInsurance = MutableStateFlow(false)
    val addonInsurance: StateFlow<Boolean> = _addonInsurance.asStateFlow()

    // Promo code state
    private val _promoCode = MutableStateFlow("")
    val promoCode: StateFlow<String> = _promoCode.asStateFlow()

    private val _promoDiscountPercent = MutableStateFlow(0)
    val promoDiscountPercent: StateFlow<Int> = _promoDiscountPercent.asStateFlow()

    private val _promoStatusMessage = MutableStateFlow("")
    val promoStatusMessage: StateFlow<String> = _promoStatusMessage.asStateFlow()

    // Completed e-Ticket Reference
    private val _latestConfirmedBooking = MutableStateFlow<BookingEntity?>(null)
    val latestConfirmedBooking: StateFlow<BookingEntity?> = _latestConfirmedBooking.asStateFlow()

    // AI Explore Recommendations State
    private val _selectedExploreCategory = MutableStateFlow("Romantic")
    val selectedExploreCategory: StateFlow<String> = _selectedExploreCategory.asStateFlow()

    private val _aiSuggestions = MutableStateFlow<List<AiRecommendation>>(emptyList())
    val aiSuggestions: StateFlow<List<AiRecommendation>> = _aiSuggestions.asStateFlow()

    private val _isAiSuggestionsLoading = MutableStateFlow(false)
    val isAiSuggestionsLoading: StateFlow<Boolean> = _isAiSuggestionsLoading.asStateFlow()

    // AI Customer Support Chat State
    private val _chatHistory = MutableStateFlow<List<Pair<String, Boolean>>>(
        listOf("Welcome to your AeroLuxe Elite Concierge. I am your premium AI travel assistant. Ask me anything about baggage policies, gourmet dining on-board, custom seat selections, or your booked itineraries." to false)
    )
    val chatHistory: StateFlow<List<Pair<String, Boolean>>> = _chatHistory.asStateFlow()

    private val _chatInput = MutableStateFlow("")
    val chatInput: StateFlow<String> = _chatInput.asStateFlow()

    private val _isChatbotResponding = MutableStateFlow(false)
    val isChatbotResponding: StateFlow<Boolean> = _isChatbotResponding.asStateFlow()

    // Notification banners
    private val _activeNotifications = MutableStateFlow<List<NotificationModel>>(emptyList())
    val activeNotifications: StateFlow<List<NotificationModel>> = _activeNotifications.asStateFlow()

    init {
        // Load initial explore suggestions
        fetchExploreSuggestions("Romantic")
        initFirebase()
    }

    private fun initFirebase() {
        try {
            val app = getApplication<Application>()
            if (FirebaseApp.getApps(app).isEmpty()) {
                var apiKey = com.example.BuildConfig.FIREBASE_API_KEY
                var appId = com.example.BuildConfig.FIREBASE_APP_ID
                var projectId = com.example.BuildConfig.FIREBASE_PROJECT_ID

                if (apiKey.isEmpty() || apiKey == "null" || apiKey == "YOUR_FIREBASE_API_KEY") {
                    apiKey = "AIzaSyA2Oat06Zlct59EQVscyHzrEDV8cWouofY"
                }
                if (appId.isEmpty() || appId == "null" || appId == "YOUR_FIREBASE_APP_ID") {
                    appId = "1:733138422082:android:21227aa504d5bcb02fe0db"
                }
                if (projectId.isEmpty() || projectId == "null" || projectId == "YOUR_FIREBASE_PROJECT_ID") {
                    projectId = "airlines-e8b40"
                }

                if (apiKey.isNotEmpty() && appId.isNotEmpty()) {
                    val options = FirebaseOptions.Builder()
                        .setApiKey(apiKey)
                        .setApplicationId(appId)
                        .setProjectId(projectId)
                        .build()
                    FirebaseApp.initializeApp(app, options)
                    android.util.Log.d("FirebaseInit", "Firebase programmatically initialized.")
                } else {
                    android.util.Log.w("FirebaseInit", "Firebase config missing, using local sandbox mode.")
                }
            }
            firebaseAuth = FirebaseAuth.getInstance()
            try {
                firebaseDb = FirebaseFirestore.getInstance()
                _firestoreSyncStatus.value = "Cloud database connected."
                android.util.Log.d("FirebaseInit", "Firestore programmatically initialized.")
            } catch (ffe: Exception) {
                android.util.Log.e("FirebaseInit", "Failed to initialize Firestore: ${ffe.message}")
            }
            _isFirebaseAvailable.value = firebaseAuth != null

            // Restore user session if already signed in
            firebaseAuth?.currentUser?.let { user ->
                _userEmail.value = user.email ?: ""
                _userName.value = user.displayName ?: "Traveler"
                _isLoggedIn.value = true
                addNotification("Session Restored", "Welcome back, elite customer.")
                syncAllFromCloud()
            }
        } catch (e: Exception) {
            android.util.Log.e("FirebaseInit", "Failed to initialize Firebase Auth: ${e.message}")
            _isFirebaseAvailable.value = false
        }
    }

    fun signInWithEmailPassword(email: String, password: String, onSelection: () -> Unit = {}) {
        val auth = firebaseAuth
        if (auth == null) {
            // Local fallback sandbox mode if Firebase is not configured
            completeLogin("Local Elite Guest", email, "SANDBOX-992", "FLYER-992")
            _authError.value = "Firebase Authentication is not configured. Running in sandbox demo mode."
            onSelection()
            return
        }

        _isAuthLoading.value = true
        _authError.value = null

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                _isAuthLoading.value = false
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    _userEmail.value = user?.email ?: email
                    _userName.value = user?.displayName ?: email.substringBefore("@")
                    _isLoggedIn.value = true
                    addNotification("Firebase Signed In", "Security session initialized successfully.")
                    syncAllFromCloud()
                    onSelection()
                } else {
                    val msg = task.exception?.localizedMessage ?: "Invalid email or password."
                    _authError.value = msg
                    addNotification("Security Failure", msg)
                }
            }
    }

    fun signUpWithEmailPassword(
        email: String,
        password: String,
        name: String,
        passport: String,
        flyerNo: String,
        onSelection: () -> Unit = {}
    ) {
        val auth = firebaseAuth
        if (auth == null) {
            // Local fallback signup if Firebase not configured
            completeLogin(name, email, passport, flyerNo)
            _authError.value = "Firebase Authentication is not configured. Running in sandbox demo mode."
            onSelection()
            return
        }

        _isAuthLoading.value = true
        _authError.value = null

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(name)
                        .build()
                    user?.updateProfile(profileUpdates)?.addOnCompleteListener {
                        _isAuthLoading.value = false
                        _userEmail.value = email
                        _userName.value = name
                        _userPassport.value = passport
                        _userFlyerNumber.value = flyerNo
                        _isLoggedIn.value = true
                        addNotification("Account Activated", "Secure profile provisioned under cloud keys.")
                        syncAllToCloud()
                        onSelection()
                    } ?: run {
                        _isAuthLoading.value = false
                        _userEmail.value = email
                        _userName.value = name
                        _userPassport.value = passport
                        _userFlyerNumber.value = flyerNo
                        _isLoggedIn.value = true
                        addNotification("Account Activated", "Secure profile provisioned.")
                        syncAllToCloud()
                        onSelection()
                    }
                } else {
                    _isAuthLoading.value = false
                    val msg = task.exception?.localizedMessage ?: "Account creation failed."
                    _authError.value = msg
                    addNotification("Federation Error", msg)
                }
            }
    }

    // --- FIRESTORE SYNCHRONIZATION AND DATABASE UPLOADS ---

    fun uploadBookingToFirestore(booking: BookingEntity) {
        val auth = firebaseAuth ?: return
        val db = firebaseDb ?: return
        val email = auth.currentUser?.email ?: _userEmail.value
        if (email.isEmpty()) return

        val data = hashMapOf(
            "bookingCode" to booking.bookingCode,
            "flightNo" to booking.flightNo,
            "airlineName" to booking.airlineName,
            "airlineCode" to booking.airlineCode,
            "departureAirportCode" to booking.departureAirportCode,
            "departureCity" to booking.departureCity,
            "arrivalAirportCode" to booking.arrivalAirportCode,
            "arrivalCity" to booking.arrivalCity,
            "departureTime" to booking.departureTime,
            "arrivalTime" to booking.arrivalTime,
            "seatNumber" to booking.seatNumber,
            "cabinClass" to booking.cabinClass,
            "ticketPrice" to booking.ticketPrice,
            "passengerName" to booking.passengerName,
            "bookingTime" to booking.bookingTime,
            "status" to booking.status
        )

        db.collection("users").document(email)
            .collection("bookings").document(booking.bookingCode)
            .set(data)
            .addOnSuccessListener {
                android.util.Log.d("FirestoreSync", "Booking ${booking.bookingCode} uploaded to cloud.")
                updateSyncMsg("Cloud sync complete: Booking uploaded")
            }
    }

    fun uploadPriceAlertToFirestore(alert: PriceAlertEntity) {
        val auth = firebaseAuth ?: return
        val db = firebaseDb ?: return
        val email = auth.currentUser?.email ?: _userEmail.value
        if (email.isEmpty()) return

        val docId = "${alert.origin}_${alert.destination}_${alert.alertPrice}".replace(".", "_")

        val data = hashMapOf(
            "origin" to alert.origin,
            "destination" to alert.destination,
            "alertPrice" to alert.alertPrice,
            "currentPrice" to alert.currentPrice,
            "isTriggered" to alert.isTriggered
        )

        db.collection("users").document(email)
            .collection("price_alerts").document(docId)
            .set(data)
            .addOnSuccessListener {
                updateSyncMsg("Price alert ${alert.origin}-${alert.destination} uploaded")
            }
    }

    fun deletePriceAlertFromFirestore(alert: PriceAlertEntity) {
        val auth = firebaseAuth ?: return
        val db = firebaseDb ?: return
        val email = auth.currentUser?.email ?: _userEmail.value
        if (email.isEmpty()) return

        val docId = "${alert.origin}_${alert.destination}_${alert.alertPrice}".replace(".", "_")
        db.collection("users").document(email)
            .collection("price_alerts").document(docId)
            .delete()
    }

    fun uploadTravelerToFirestore(traveler: TravelerEntity) {
        val auth = firebaseAuth ?: return
        val db = firebaseDb ?: return
        val email = auth.currentUser?.email ?: _userEmail.value
        if (email.isEmpty()) return

        val docId = traveler.name.lowercase().replace(" ", "_")

        val data = hashMapOf(
            "name" to traveler.name,
            "email" to traveler.email,
            "passportNumber" to traveler.passportNumber,
            "frequentFlyerNumber" to traveler.frequentFlyerNumber
        )

        db.collection("users").document(email)
            .collection("travelers").document(docId)
            .set(data)
            .addOnSuccessListener {
                updateSyncMsg("Companion ${traveler.name} saved to cloud")
            }
    }

    fun deleteTravelerFromFirestore(traveler: TravelerEntity) {
        val auth = firebaseAuth ?: return
        val db = firebaseDb ?: return
        val email = auth.currentUser?.email ?: _userEmail.value
        if (email.isEmpty()) return

        val docId = traveler.name.lowercase().replace(" ", "_")
        db.collection("users").document(email)
            .collection("travelers").document(docId)
            .delete()
    }

    private fun updateSyncMsg(msg: String) {
        _firestoreSyncStatus.value = msg
        android.util.Log.d("FirestoreSync", msg)
    }

    fun syncAllToCloud() {
        val db = firebaseDb ?: return
        val auth = firebaseAuth ?: return
        val email = auth.currentUser?.email ?: _userEmail.value
        if (email.isEmpty()) return

        _isFirestoreSyncing.value = true
        _firestoreSyncStatus.value = "Uploading database to Firestore..."

        viewModelScope.launch {
            try {
                allBookings.value.forEach { uploadBookingToFirestore(it) }
                priceAlerts.value.forEach { uploadPriceAlertToFirestore(it) }
                savedTravelers.value.forEach { uploadTravelerToFirestore(it) }

                _firestoreSyncStatus.value = "All database records uploaded to Firestore successfully."
                addNotification("Cloud Backup Created", "Successfully synchronized all database records with Firestore.")
            } catch (e: Exception) {
                _firestoreSyncStatus.value = "Upload error: ${e.localizedMessage}"
            } finally {
                _isFirestoreSyncing.value = false
            }
        }
    }

    fun syncAllFromCloud() {
        val db = firebaseDb ?: return
        val auth = firebaseAuth ?: return
        val email = auth.currentUser?.email ?: _userEmail.value
        if (email.isEmpty()) return

        _isFirestoreSyncing.value = true
        _firestoreSyncStatus.value = "Downloading records from Firestore..."

        db.collection("users").document(email).collection("bookings").get()
            .addOnSuccessListener { bookingDocs ->
                viewModelScope.launch {
                    for (doc in bookingDocs.documents) {
                        try {
                            val entity = BookingEntity(
                                bookingCode = doc.getString("bookingCode") ?: "",
                                flightNo = doc.getString("flightNo") ?: "",
                                airlineName = doc.getString("airlineName") ?: "",
                                airlineCode = doc.getString("airlineCode") ?: "",
                                departureAirportCode = doc.getString("departureAirportCode") ?: "",
                                departureCity = doc.getString("departureCity") ?: "",
                                arrivalAirportCode = doc.getString("arrivalAirportCode") ?: "",
                                arrivalCity = doc.getString("arrivalCity") ?: "",
                                departureTime = doc.getLong("departureTime") ?: 0L,
                                arrivalTime = doc.getLong("arrivalTime") ?: 0L,
                                seatNumber = doc.getString("seatNumber") ?: "",
                                cabinClass = doc.getString("cabinClass") ?: "",
                                ticketPrice = doc.getDouble("ticketPrice") ?: 0.0,
                                passengerName = doc.getString("passengerName") ?: "",
                                bookingTime = doc.getLong("bookingTime") ?: System.currentTimeMillis(),
                                status = doc.getString("status") ?: "UPCOMING"
                            )
                            if (!allBookings.value.any { it.bookingCode == entity.bookingCode }) {
                                repository.insertBooking(entity)
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("FirestoreSync", "Failed mapping bookingDoc: ${e.message}")
                        }
                    }
                }
                
                db.collection("users").document(email).collection("travelers").get()
                    .addOnSuccessListener { travelerDocs ->
                        viewModelScope.launch {
                            for (doc in travelerDocs.documents) {
                                try {
                                    val entity = TravelerEntity(
                                        name = doc.getString("name") ?: "",
                                        email = doc.getString("email") ?: "",
                                        passportNumber = doc.getString("passportNumber") ?: "",
                                        frequentFlyerNumber = doc.getString("frequentFlyerNumber") ?: ""
                                    )
                                    if (!savedTravelers.value.any { it.name.equals(entity.name, ignoreCase = true) }) {
                                        repository.insertTraveler(entity)
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("FirestoreSync", "Failed mapping travelerDoc: ${e.message}")
                                }
                            }
                        }
                    }

                db.collection("users").document(email).collection("price_alerts").get()
                    .addOnSuccessListener { alertDocs ->
                        viewModelScope.launch {
                            for (doc in alertDocs.documents) {
                                try {
                                    val entity = PriceAlertEntity(
                                        origin = doc.getString("origin") ?: "",
                                        destination = doc.getString("destination") ?: "",
                                        alertPrice = doc.getDouble("alertPrice") ?: 0.0,
                                        currentPrice = doc.getDouble("currentPrice") ?: 0.0,
                                        isTriggered = doc.getBoolean("isTriggered") ?: false
                                    )
                                    if (!priceAlerts.value.any { it.origin == entity.origin && it.destination == entity.destination }) {
                                        repository.insertPriceAlert(entity)
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("FirestoreSync", "Failed mapping alertDoc: ${e.message}")
                                }
                            }
                        }
                        _isFirestoreSyncing.value = false
                        _firestoreSyncStatus.value = "Successfully synchronized with cloud database."
                        addNotification("Sync Success", "Firestore documents imported into your offline container.")
                    }
                    .addOnFailureListener {
                        _isFirestoreSyncing.value = false
                    }
            }
            .addOnFailureListener { e ->
                _isFirestoreSyncing.value = false
                _firestoreSyncStatus.value = "Import error: ${e.localizedMessage}"
            }
    }

    // Onboarding triggers
    fun completeOnboarding() {
        _onboardingCompleted.value = true
    }

    fun completeLogin(name: String, email: String, passport: String, flyerNo: String) {
        _userName.value = name.ifBlank { "Sir Alistair Thorne" }
        _userEmail.value = email.ifBlank { "alistair.thorne@chamber.org" }
        _userPassport.value = passport.ifBlank { "GBR950183" }
        _userFlyerNumber.value = flyerNo.ifBlank { "EK-8840291" }
        _isLoggedIn.value = true
        addNotification("Session Secured", "Elite account logged in biometric verified.")
    }

    fun logout() {
        try {
            firebaseAuth?.signOut()
        } catch (e: Exception) {
            android.util.Log.e("FirebaseLogout", "Failed to sign out of Firebase: ${e.message}")
        }
        _isLoggedIn.value = false
        addNotification("Session Cleared", "Secure profile disconnected.")
    }

    fun setTab(tab: DashboardTab) {
        _currentTab.value = tab
    }

    fun toggleDarkMode() {
        _isDarkMode.value = !_isDarkMode.value
    }

    fun updateProfile(name: String, email: String, passport: String, flyerNo: String) {
        _userName.value = name
        _userEmail.value = email
        _userPassport.value = passport
        _userFlyerNumber.value = flyerNo
        addNotification("Profile Synced", "Global luxury traveler details saved.")
    }

    fun updateSettings(currency: String, language: String) {
        _selectedCurrency.value = currency
        _selectedLanguage.value = language
        addNotification("Preferences Customised", "System configured to $language | $currency.")
    }

    // Search input modification
    fun setSearchQuery(
        origin: String,
        destination: String,
        cabinClass: String,
        isRound: Boolean,
        dateMils: Long,
        passengers: Int
    ) {
        _searchOrigin.value = origin.uppercase()
        _searchDestination.value = destination.uppercase()
        _searchCabinClass.value = cabinClass
        _isRoundTrip.value = isRound
        _searchDateMills.value = dateMils
        _searchPassengers.value = passengers

        // Simple predictive suggestions based on typing
        val textOrig = origin.uppercase()
        _predictOriginResults.value = if (textOrig.length > 1) {
            listOf("London Heathrow (LHR)", "Singapore Changi (SIN)", "Dubai International (DXB)", "New York (JFK)")
                .filter { it.uppercase().contains(textOrig) }
        } else emptyList()

        val textDest = destination.uppercase()
        _predictDestResults.value = if (textDest.length > 1) {
            listOf("London Heathrow (LHR)", "Singapore Changi (SIN)", "Dubai International (DXB)", "Tokyo Haneda (HND)", "Paris (CDG)")
                .filter { it.uppercase().contains(textDest) }
        } else emptyList()
    }

    fun executeFlightSearch() {
        _bookingStep.value = BookingStep.FLIGHT_RESULTS
        
        val list = repository.searchFlights(
            _searchOrigin.value,
            _searchDestination.value,
            _searchCabinClass.value,
            _searchDateMills.value,
            _searchPassengers.value
        )
        _searchResults.value = list
        
        _fareTrends.value = repository.getFareTrends(_searchOrigin.value, _searchDestination.value)
    }

    fun setResultsSort(sortBy: String) {
        _resultsSortBy.value = sortBy
        val current = _searchResults.value
        _searchResults.value = when (sortBy) {
            "Price" -> current.sortedBy { it.price }
            "Duration" -> current.sortedBy { it.durationMinutes }
            "Eco" -> current.sortedBy { it.ecoImpactPercentage }
            else -> current
        }
    }

    // Select flight and enter passenger details step
    fun selectFlight(flight: Flight) {
        _selectedFlight.value = flight
        _passengerNameInput.value = _userName.value
        _passengerEmailInput.value = _userEmail.value
        _passengerPassportInput.value = _userPassport.value
        _passengerFlyerInput.value = _userFlyerNumber.value
        _bookingStep.value = BookingStep.PASSENGER_DETAILS
    }

    fun applyPassengerDetails(name: String, email: String, passport: String, flyer: String) {
        _passengerNameInput.value = name
        _passengerEmailInput.value = email
        _passengerPassportInput.value = passport
        _passengerFlyerInput.value = flyer
        // Auto select a free seat initially from the flight's layout is occupant dependent
        val flight = _selectedFlight.value
        if (flight != null) {
            val freeSeat = flight.seatLayout.entries.find { !it.value }?.key ?: "01A"
            _selectedSeatCode.value = freeSeat
        }
        _bookingStep.value = BookingStep.SEAT_SELECTION
    }

    fun selectSeat(seatCode: String) {
        _selectedSeatCode.value = seatCode
    }

    fun proceedToAddons() {
        _bookingStep.value = BookingStep.ADD_ONS
    }

    fun toggleBaggage() {
        _addonBaggage.value = !_addonBaggage.value
    }

    fun toggleMeal() {
        _addonMeal.value = !_addonMeal.value
    }

    fun toggleInsurance() {
        _addonInsurance.value = !_addonInsurance.value
    }

    fun proceedToPayment() {
        _bookingStep.value = BookingStep.PAYMENT
    }

    fun applyPromo(code: String) {
        val uppercaseCode = code.trim().uppercase()
        _promoCode.value = uppercaseCode
        if (uppercaseCode == "AEROLUXE" || uppercaseCode == "LUCKY7") {
            _promoDiscountPercent.value = 15
            _promoStatusMessage.value = "Aeroluxe Executive promo applied! 15% discount."
        } else {
            _promoDiscountPercent.value = 0
            _promoStatusMessage.value = "Invalid premium promo code."
        }
    }

    fun calculateTotalCost(): Double {
        val flight = _selectedFlight.value ?: return 0.0
        var cost = flight.price
        if (_addonBaggage.value) cost += 50.0
        if (_addonMeal.value) cost += 45.0
        if (_addonInsurance.value) cost += 29.0
        
        if (_promoDiscountPercent.value > 0) {
            cost = cost * (1.0 - (_promoDiscountPercent.value / 100.0))
        }
        return Math.round(cost * 100.0) / 100.0
    }

    // Complete Booking, generate eTicket and insert to Room
    fun completeBooking(paymentMethod: String) {
        val flight = _selectedFlight.value ?: return
        val details = BookingEntity(
            bookingCode = "AEROLX" + UUID.randomUUID().toString().take(4).uppercase(),
            flightNo = flight.flightNo,
            airlineName = flight.airlineName,
            airlineCode = flight.airlineCode,
            departureAirportCode = flight.departureAirportCode,
            departureCity = flight.departureCity,
            arrivalAirportCode = flight.arrivalAirportCode,
            arrivalCity = flight.arrivalCity,
            departureTime = flight.departureTime,
            arrivalTime = flight.arrivalTime,
            seatNumber = _selectedSeatCode.value,
            cabinClass = flight.cabinClass,
            ticketPrice = calculateTotalCost(),
            passengerName = _passengerNameInput.value,
            status = "UPCOMING"
        )

        viewModelScope.launch {
            repository.insertBooking(details)
            uploadBookingToFirestore(details)
            _latestConfirmedBooking.value = details
            _bookingStep.value = BookingStep.CONFIRMATION
            addNotification(
                title = "Flight Ticket Issued",
                description = "E-Ticket for ${flight.flightNo} to ${flight.arrivalCity} generated."
            )
        }
    }

    fun resetBookingFlow() {
        _selectedFlight.value = null
        _selectedSeatCode.value = ""
        _addonBaggage.value = false
        _addonMeal.value = false
        _addonInsurance.value = false
        _promoCode.value = ""
        _promoDiscountPercent.value = 0
        _promoStatusMessage.value = ""
        _bookingStep.value = BookingStep.SEARCH_INPUT
    }

    // Cancelling a booking
    fun cancelExistingBooking(booking: BookingEntity) {
        viewModelScope.launch {
            val updated = booking.copy(status = "CANCELLED")
            repository.updateBooking(updated)
            uploadBookingToFirestore(updated)
            addNotification(
                title = "Trip Cancelled",
                description = "Rescheduling credit for ${booking.flightNo} sent to email."
            )
        }
    }

    // Modifying/Rescheduling boarding seat
    fun rescheduleSeat(booking: BookingEntity, newSeat: String) {
        viewModelScope.launch {
            val updated = booking.copy(seatNumber = newSeat)
            repository.updateBooking(updated)
            uploadBookingToFirestore(updated)
            addNotification(
                title = "Seat Rescheduled",
                description = "Your new seat is verified at $newSeat."
            )
        }
    }

    // Price Watch alerts settings
    fun addPriceWatch(origin: String, destination: String, target: Double) {
        viewModelScope.launch {
            val entity = PriceAlertEntity(
                origin = origin.uppercase(),
                destination = destination.uppercase(),
                alertPrice = target,
                currentPrice = target * 1.12
            )
            repository.insertPriceAlert(entity)
            uploadPriceAlertToFirestore(entity)
            addNotification(
                title = "Price Alert Set",
                description = "We will prompt if prices for $origin-$destination fall under $target."
            )
        }
    }

    fun removePriceWatch(alert: PriceAlertEntity) {
        viewModelScope.launch {
            repository.deletePriceAlert(alert)
            deletePriceAlertFromFirestore(alert)
        }
    }

    // Travelers configurations
    fun saveNewTraveler(name: String, email: String, passport: String, flyer: String) {
        viewModelScope.launch {
            val entity = TravelerEntity(
                name = name,
                email = email,
                passportNumber = passport,
                frequentFlyerNumber = flyer
            )
            repository.insertTraveler(entity)
            uploadTravelerToFirestore(entity)
            addNotification(
                title = "Companion Added",
                description = "$name is now available in traveler preferences."
            )
        }
    }

    fun deleteSavedTraveler(traveler: TravelerEntity) {
        viewModelScope.launch {
            repository.deleteTraveler(traveler)
            deleteTravelerFromFirestore(traveler)
        }
    }

    // AI Destinations Recommendations via Gemini
    fun fetchExploreSuggestions(category: String) {
        _selectedExploreCategory.value = category
        _isAiSuggestionsLoading.value = true
        viewModelScope.launch {
            try {
                val results = GeminiManager.getRecommendations(category)
                _aiSuggestions.value = results
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isAiSuggestionsLoading.value = false
            }
        }
    }

    // AI Chatbox Messaging
    fun updateChatInput(text: String) {
        _chatInput.value = text
    }

    fun sendChatMessage() {
        val message = _chatInput.value.trim()
        if (message.isEmpty()) return

        val updatedHistory = _chatHistory.value.toMutableList()
        updatedHistory.add(message to true) // user message
        _chatHistory.value = updatedHistory
        _chatInput.value = ""
        _isChatbotResponding.value = true

        viewModelScope.launch {
            try {
                val apiResponse = GeminiManager.querySupportChat(updatedHistory, message)
                val currentHistory = _chatHistory.value.toMutableList()
                currentHistory.add(apiResponse to false) // assistant response
                _chatHistory.value = currentHistory
            } catch (e: Exception) {
                e.printStackTrace()
                val currentHistory = _chatHistory.value.toMutableList()
                currentHistory.add("Apologies, I encountered a temporary operational glitch. Please try again." to false)
                _chatHistory.value = currentHistory
            } finally {
                _isChatbotResponding.value = false
            }
        }
    }

    // Simulated local push alerts
    private fun addNotification(title: String, description: String) {
        val updated = _activeNotifications.value.toMutableList()
        updated.add(0, NotificationModel(title = title, description = description))
        _activeNotifications.value = updated
    }

    fun dismissNotification(id: String) {
        val updated = _activeNotifications.value.filter { it.id != id }
        _activeNotifications.value = updated
    }
}
