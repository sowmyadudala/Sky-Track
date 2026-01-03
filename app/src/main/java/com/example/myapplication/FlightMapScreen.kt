        package com.example.myapplication

        import androidx.compose.animation.core.*
        import androidx.compose.foundation.background
        import androidx.compose.foundation.layout.*
        import androidx.compose.foundation.shape.CircleShape
        import androidx.compose.foundation.shape.RoundedCornerShape
        import androidx.compose.material.icons.Icons
        import androidx.compose.material.icons.filled.*
        import androidx.compose.material3.*
        import androidx.compose.runtime.*
        import androidx.compose.ui.Alignment
        import androidx.compose.ui.Modifier
        import androidx.compose.ui.draw.clip
        import androidx.compose.ui.geometry.Offset
        import androidx.compose.ui.graphics.*
        import androidx.compose.ui.platform.LocalContext
        import androidx.compose.ui.text.font.FontWeight
        import androidx.compose.ui.unit.dp
        import androidx.compose.ui.unit.sp
        import androidx.compose.ui.viewinterop.AndroidView
        import org.osmdroid.tileprovider.tilesource.TileSourceFactory
        import org.osmdroid.util.GeoPoint
        import org.osmdroid.views.MapView
        import org.osmdroid.views.overlay.Marker
        import org.osmdroid.views.overlay.Polyline
        import kotlin.math.*
        import org.osmdroid.config.Configuration


        @Composable
        fun FlightMapScreen(
            flightItem: FlightItem,
            onBack: () -> Unit
        ) {
            val planePosition by animateFloatAsState(
                targetValue = 0.6f,
                animationSpec = tween(2000, easing = FastOutSlowInEasing)
            )

            val flightData = remember(flightItem) {
                convertToFlightData(flightItem)
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0A192F))
            ) {
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(color = Color(0xFF0A192F))

                    for (i in 0..49) {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.3f),
                            radius = 1f,
                            center = Offset(
                                (i * 37.1f % size.width),
                                (i * 23.3f % size.height)
                            )
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(70.dp)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF1A2980),
                                    Color(0xFF26D0CE)
                                )
                            )
                        )
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        Text(
                            "✈️ Flight Tracker",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        Box(modifier = Modifier.size(48.dp))
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 70.dp)
                        .padding(16.dp)
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF1E2A47)
                        ),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            FlightMapCanvas(
                                flightData = flightData,
                                planePosition = planePosition
                            )

                            Box(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .align(Alignment.TopEnd)
                            ) {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color.Black.copy(alpha = 0.7f)
                                    ),
                                    shape = CircleShape
                                ) {
                                    Box(
                                        modifier = Modifier.size(50.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("N", color = Color.White, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    FlightInfoCard(flightData = flightData)
                }
            }
        }

        @Composable
        private fun FlightMapCanvas(
            flightData: FlightDisplayData,
            planePosition: Float
        ) {
            val context = LocalContext.current

            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->

                    Configuration.getInstance().load(
                        context,
                        context.getSharedPreferences("osmdroid", 0)
                    )

                    MapView(context).apply {

                        setTileSource(TileSourceFactory.OpenTopo)
                        setMultiTouchControls(true)

                        val originLat = flightData.originLat ?: getLatForCode(flightData.originCode)
                        val originLon = flightData.originLon ?: getLonForCode(flightData.originCode)
                        val destLat = flightData.destLat ?: getLatForCode(flightData.destinationCode)
                        val destLon = flightData.destLon ?: getLonForCode(flightData.destinationCode)

                        val controller = controller

                        val centerLat = (originLat + destLat) / 2.0
                        val centerLon = (originLon + destLon) / 2.0
                        controller.setCenter(GeoPoint(centerLat, centerLon))

                        controller.setZoom(5.0)

                        overlays.add(
                            Marker(this).apply {
                                position = GeoPoint(originLat, originLon)
                                title = flightData.originCode
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            }
                        )

                        overlays.add(
                            Marker(this).apply {
                                position = GeoPoint(destLat, destLon)
                                title = flightData.destinationCode
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            }
                        )

                        overlays.add(
                            Polyline().apply {
                                addPoint(GeoPoint(originLat, originLon))
                                addPoint(GeoPoint(destLat, destLon))
                                color = android.graphics.Color.CYAN
                                width = 6f
                            }
                        )

                        if (flightData.shouldShowPlane) {
                            val planeLat =
                                originLat + (destLat - originLat) * planePosition.toDouble()
                            val planeLon =
                                originLon + (destLon - originLon) * planePosition.toDouble()

                            overlays.add(
                                Marker(this).apply {
                                    position = GeoPoint(planeLat, planeLon)
                                    title = "✈️ ${flightData.flightNumber}"
                                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                                }
                            )
                        }

                        invalidate()
                    }
                }
            )


        }

        private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
            val earthRadius = 6371.0

            val dLat = Math.toRadians(lat2 - lat1)
            val dLon = Math.toRadians(lon2 - lon1)

            val a = sin(dLat / 2) * sin(dLat / 2) +
                    cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                    sin(dLon / 2) * sin(dLon / 2)

            val c = 2 * atan2(sqrt(a), sqrt(1 - a))

            return earthRadius * c
        }

        private fun getLatForCode(code: String): Double {
            return when (code.uppercase()) {
                "DEL" -> 28.5562
                "BOM" -> 19.0896
                "MAA" -> 12.9941
                "BLR" -> 13.1986
                "HYD" -> 17.2403
                "CCU" -> 22.6547
                "JFK" -> 40.6413
                "LAX" -> 33.9416
                "LHR" -> 51.4700
                "DXB" -> 25.2532
                "SIN" -> 1.3644
                "HND" -> 35.5494
                "CDG" -> 49.0097
                "FRA" -> 50.0379
                "AMS" -> 52.3086
                else -> 20.0 + (code.hashCode() % 40)
            }
        }

        private fun getLonForCode(code: String): Double {
            return when (code.uppercase()) {
                "DEL" -> 77.1000
                "BOM" -> 72.8656
                "MAA" -> 80.1709
                "BLR" -> 77.7066
                "HYD" -> 78.4294
                "CCU" -> 88.4467
                "JFK" -> -73.7781
                "LAX" -> -118.4085
                "LHR" -> -0.4543
                "DXB" -> 55.3657
                "SIN" -> 103.9915
                "HND" -> 139.7798
                "CDG" -> 2.5479
                "FRA" -> 8.5622
                "AMS" -> 4.7639
                else -> 70.0 + (code.hashCode() % 60)
            }
        }

        @Composable
        private fun FlightInfoCard(flightData: FlightDisplayData) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1E2A47)
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Flight,
                            contentDescription = "Flight",
                            tint = Color(0xFF4CC9F0),
                            modifier = Modifier.size(24.dp)
                        )

                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "${flightData.airline} ${flightData.flightNumber}",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Text(
                                "${flightData.originCode} → ${flightData.destinationCode}",
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }

                        Badge(
                            containerColor = flightData.statusColor
                        ) {
                            Text(flightData.status, color = Color.White, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        FlightDetailItem(
                            icon = Icons.Default.TrendingUp,
                            title = "Altitude",
                            value = flightData.altitude,
                            color = Color(0xFF00D4AA)
                        )

                        FlightDetailItem(
                            icon = Icons.Default.Speed,
                            title = "Speed",
                            value = flightData.speed,
                            color = Color(0xFF4CC9F0)
                        )

                        FlightDetailItem(
                            icon = Icons.Default.Timer,
                            title = "ETA",
                            value = flightData.eta,
                            color = Color(0xFF9B59B6)
                        )

                        FlightDetailItem(
                            icon = Icons.Default.Place,
                            title = "Distance",
                            value = flightData.distance,
                            color = Color(0xFFE74C3C)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    if (flightData.showProgress) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "Progress",
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                                Text(
                                    "${flightData.progressPercent}%",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))


                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(Color(0xFF2D3B5E))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(flightData.progress)
                                        .height(10.dp)
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(
                                            brush = Brush.horizontalGradient(
                                                colors = listOf(
                                                    Color(0xFF00D4AA),
                                                    Color(0xFF4CC9F0)
                                                )
                                            )
                                        )
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    flightData.distanceFlown,
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 12.sp
                                )
                                Text(
                                    flightData.distanceRemaining,
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    } else {
                        Text(
                            "Flight ${flightData.status.lowercase()}",
                            color = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }

        @Composable
        private fun FlightDetailItem(
            icon: androidx.compose.ui.graphics.vector.ImageVector,
            title: String,
            value: String,
            color: Color
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    icon,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    title,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )

                Text(
                    value,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }


        data class FlightDisplayData(
            val flightNumber: String,
            val airline: String,
            val originCode: String,
            val destinationCode: String,
            val status: String,
            val progress: Float,
            val progressPercent: Int,
            val altitude: String,
            val speed: String,
            val eta: String,
            val distance: String,
            val distanceFlown: String,
            val distanceRemaining: String,
            val statusColor: Color,
            val showProgress: Boolean,
            val shouldShowPlane: Boolean,
            val originLat: Double? = null,
            val originLon: Double? = null,
            val destLat: Double? = null,
            val destLon: Double? = null
        )

        private fun convertToFlightData(flightItem: FlightItem): FlightDisplayData {
            val flightNumber = flightItem.number ?: "Unknown"
            val airline = flightItem.airline?.name ?: "Unknown Airline"
            val originCode = flightItem.departure?.airport?.iata
                ?: error("Origin airport missing")

            val destinationCode = flightItem.arrival?.airport?.iata
                ?: error("Destination airport missing")
            val rawStatus = flightItem.status ?: "Unknown"

            val statusInfo = determineFlightStatus(rawStatus)

            val altitude = if (statusInfo.shouldShowPlane) "${kotlin.random.Random.nextInt(30, 40)},000 ft" else "-"
            val speed = if (statusInfo.shouldShowPlane) "${kotlin.random.Random.nextInt(800, 900)} km/h" else "-"
            val eta = if (statusInfo.shouldShowPlane) "${kotlin.random.Random.nextInt(1, 3)}h ${kotlin.random.Random.nextInt(10, 59)}m" else "-"

            val originLat = getLatForCode(originCode)
            val originLon = getLonForCode(originCode)
            val destLat = getLatForCode(destinationCode)
            val destLon = getLonForCode(destinationCode)

            val distanceKm = calculateDistance(originLat, originLon, destLat, destLon).toInt()
            val distanceFlown = (distanceKm * statusInfo.progress).toInt()
            val distanceRemaining = distanceKm - distanceFlown

            val statusColor = when (statusInfo.status) {
                "In Flight" -> Color(0xFF00B894)
                "Scheduled" -> Color(0xFF3498DB)
                "Delayed" -> Color(0xFFE74C3C)
                "Landed" -> Color(0xFF9B59B6)
                "Cancelled" -> Color(0xFF95A5A6)
                else -> Color.Gray
            }

            return FlightDisplayData(
                flightNumber = flightNumber,
                airline = airline,
                originCode = originCode,
                destinationCode = destinationCode,
                status = statusInfo.status,
                progress = statusInfo.progress,
                progressPercent = (statusInfo.progress * 100).toInt(),
                altitude = altitude,
                speed = speed,
                eta = eta,
                distance = "$distanceKm km",
                distanceFlown = "$distanceFlown km flown",
                distanceRemaining = "$distanceRemaining km remaining",
                statusColor = statusColor,
                showProgress = statusInfo.showProgress,
                shouldShowPlane = statusInfo.shouldShowPlane,
                originLat = originLat,
                originLon = originLon,
                destLat = destLat,
                destLon = destLon
            )
        }

        private fun determineFlightStatus(rawStatus: String): FlightStatusInfo {
            val lowerStatus = rawStatus.lowercase()

            return when {
                "inflight" in lowerStatus || "departed" in lowerStatus || "enroute" in lowerStatus || "airborne" in lowerStatus ->
                    FlightStatusInfo(
                        status = "In Flight",
                        progress = 0.6f,
                        shouldShowPlane = true,
                        showProgress = true
                    )
                "scheduled" in lowerStatus || "checkin" in lowerStatus || "gate" in lowerStatus || "boarding" in lowerStatus ->
                    FlightStatusInfo(
                        status = "Scheduled",
                        progress = 0.0f,
                        shouldShowPlane = false,
                        showProgress = false
                    )
                "landed" in lowerStatus || "arrived" in lowerStatus || "arrival" in lowerStatus ->
                    FlightStatusInfo(
                        status = "Landed",
                        progress = 1.0f,
                        shouldShowPlane = false,
                        showProgress = true
                    )
                "delayed" in lowerStatus ->
                    FlightStatusInfo(
                        status = "Delayed",
                        progress = 0.0f,
                        shouldShowPlane = false,
                        showProgress = false
                    )
                "cancelled" in lowerStatus || "canceled" in lowerStatus ->
                    FlightStatusInfo(
                        status = "Cancelled",
                        progress = 0.0f,
                        shouldShowPlane = false,
                        showProgress = false
                    )
                else ->
                    FlightStatusInfo(
                        status = rawStatus,
                        progress = 0.0f,
                        shouldShowPlane = false,
                        showProgress = false
                    )
            }
        }

        data class FlightStatusInfo(
            val status: String,
            val progress: Float,
            val shouldShowPlane: Boolean,
            val showProgress: Boolean
        )