@file:OptIn(ExperimentalMaterial3Api::class)
package com.example.myapplication

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*


data class FlightResponse(val departures: List<FlightItem>?)

data class FlightItem(
    val number: String?,
    val airline: AirlineInfo?,
    val status: String?,
    val departure: DepartureInfo?,
    val arrival: ArrivalInfo?
)

data class AirlineInfo(val name: String?)

data class AirportShortInfo(
    val iata: String? = null,
    val name: String? = null
)

data class DepartureInfo(
    val airport: AirportShortInfo? = null,
    val scheduledTime: ScheduledTime? = null
)

data class ArrivalInfo(
    val airport: AirportShortInfo? = null,
    val scheduledTime: ScheduledTime? = null
)

data class ScheduledTime(
    val local: String? = null,
    val utc: String? = null
)


interface AeroAPI {
    @Headers(
        "X-RapidAPI-Key: 1513b4f774msh56378ee7de3d78ep194288jsn6e66abc527db",
        "X-RapidAPI-Host: aerodatabox.p.rapidapi.com"
    )
    @GET("flights/airports/iata/{origin}")
    suspend fun getFlightsFromAirport(
        @Path("origin") origin: String,
        @Query("direction") direction: String = "Departure",
        @Query("withAirport") withAirport: Boolean = true,
        @Query("withArrivalAirport") withArrivalAirport: Boolean = true,
        @Query("withLocation") withLocation: Boolean = true
    ): FlightResponse
}

fun createApi(): AeroAPI =
    Retrofit.Builder()
        .baseUrl("https://aerodatabox.p.rapidapi.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(AeroAPI::class.java)


private val FAKE_AIRPORTS = listOf(
    "LAX", "SFO", "SEA", "ORD", "ATL",
    "DFW", "DEN", "BOS", "MIA", "PHX",
    "LAS", "MSP", "DTW", "IAD", "EWR"
)


@Composable
fun FlightSearchScreen(onBack: () -> Unit) {

    val api = remember { createApi() }
    val scope = rememberCoroutineScope()

    var origin by remember { mutableStateOf(TextFieldValue("")) }
    var flights by remember { mutableStateOf<List<FlightItem>>(emptyList()) }

    var selectedFlight by remember { mutableStateOf<FlightItem?>(null) }
    var flightToTrack by remember { mutableStateOf<FlightItem?>(null) }
    var showMap by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    val bg = Brush.verticalGradient(
        listOf(Color(0xFF001B48), Color(0xFF0056A4), Color(0xFF00A6FB))
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(bg)
            .padding(20.dp)
    ) {

        TextButton(
            onClick = {
                FirebaseAuth.getInstance().signOut()
                onBack()
            },
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            Text("Logout", color = Color.White)
        }

        when {
            showMap -> {
                FlightMapScreen(
                    flightItem = flightToTrack ?: getDefaultFlightItem(),
                    onBack = {
                        showMap = false
                        flightToTrack = null
                    }
                )
            }

            selectedFlight != null -> {
                FlightDetailsInline(
                    flight = selectedFlight!!,
                    searchedOrigin = origin.text,
                    onBack = { selectedFlight = null },
                    onTrack = { flight ->
                        val fakeDest = flight.fakeDestination()

                        flightToTrack = flight.copy(
                            departure = DepartureInfo(
                                airport = AirportShortInfo(iata = origin.text)
                            ),
                            arrival = ArrivalInfo(
                                airport = AirportShortInfo(iata = fakeDest)
                            )
                        )
                        showMap = true
                    }
                )
            }

            else -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {

                    Spacer(Modifier.height(50.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Flight, null, tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text("Search Flights", color = Color.White, fontSize = 24.sp)
                    }

                    Spacer(Modifier.height(20.dp))

                    OutlinedTextField(
                        value = origin,
                        onValueChange = {
                            origin = TextFieldValue(it.text.uppercase(), it.selection)
                        },
                        label = { Text("Origin (e.g., JFK)", color = Color.White) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                        textStyle = LocalTextStyle.current.copy(color = Color.White),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = Color.White,
                            cursorColor = Color.White
                        )
                    )

                    Spacer(Modifier.height(20.dp))

                    Button(
                        enabled = origin.text.length == 3,
                        onClick = {
                            isLoading = true
                            scope.launch(Dispatchers.IO) {
                                try {
                                    flights =
                                        api.getFlightsFromAirport(origin.text).departures
                                            ?: emptyList()
                                } catch (e: Exception) {
                                    Log.e("API", e.toString())
                                }
                                isLoading = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Search Flights")
                    }

                    Spacer(Modifier.height(20.dp))

                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White)
                    } else {
                        LazyColumn {
                            items(flights) { flight ->
                                val toDisplay = flight.fakeDestination()

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp)
                                        .clickable { selectedFlight = flight }
                                ) {
                                    Column(Modifier.padding(12.dp)) {
                                        Text(
                                            "${flight.airline?.name ?: "Unknown"} ${flight.number ?: ""}",
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text("Status: ${flight.safeStatus()}")
                                        Text("From: ${origin.text} → $toDisplay")

                                        Spacer(Modifier.height(4.dp))

                                        Text(
                                            "View details →",
                                            color = Color(0xFF0056A4),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun FlightDetailsInline(
    flight: FlightItem,
    searchedOrigin: String,
    onBack: () -> Unit,
    onTrack: (FlightItem) -> Unit
) {
    val destination = flight.fakeDestination()

    Column(
        Modifier.fillMaxSize().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("← Back", color = Color.White, modifier = Modifier.clickable { onBack() })

        Spacer(Modifier.height(20.dp))

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    "${flight.airline?.name ?: "Unknown"} ${flight.number ?: ""}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Text("Status: ${flight.safeStatus()}")
                Text("From: $searchedOrigin → $destination")
                Text("To: $destination")

                Spacer(Modifier.height(8.dp))

                Text("Departure: ${flight.safeDepartureTime()}")
                Text("Arrival: ${flight.safeArrivalTime()}")

                Spacer(Modifier.height(20.dp))

                Button(
                    onClick = { onTrack(flight) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Track Flight on Map")
                }
            }
        }
    }
}


private fun FlightItem.fakeDestination(): String {
    val key = number ?: airline?.name ?: "X"
    val index = kotlin.math.abs(key.hashCode()) % FAKE_AIRPORTS.size
    return FAKE_AIRPORTS[index]
}

private fun getDefaultFlightItem() = FlightItem(
    number = "AI-123",
    airline = AirlineInfo("Air India"),
    status = "In Flight",
    departure = DepartureInfo(AirportShortInfo("DEL")),
    arrival = ArrivalInfo(AirportShortInfo("BOM"))
)

private fun FlightItem.safeStatus(): String =
    status ?: listOf("Scheduled", "In Flight", "Delayed", "Landed")
        .let { it[kotlin.math.abs((number ?: "").hashCode()) % it.size] }

private fun FlightItem.safeDepartureTime(): String =
    departure?.scheduledTime?.local ?: "Today • 10:30"

private fun FlightItem.safeArrivalTime(): String =
    arrival?.scheduledTime?.local ?: "Today • 13:15"
