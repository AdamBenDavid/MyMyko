package com.example.mymyko

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.app.ActivityCompat
import com.example.mymyko.data.models.Post
import com.google.android.gms.location.*
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.maps.model.Marker
import com.google.firebase.firestore.FirebaseFirestore

class MapFragment : Fragment(), OnMapReadyCallback {

    private var mMap: GoogleMap? = null
    private lateinit var tvWeatherRecommendation: TextView

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var currentLocationMarker: Marker? = null

    // focus on the post location
    private var focusedLat: Double? = null
    private var focusedLng: Double? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_map, container, false)

        val childFragment = BottomNavFragment()
        val bundle = Bundle()
        bundle.putString("current_page", "map")
        childFragment.arguments = bundle
        childFragmentManager.beginTransaction()
            .replace(R.id.navbar_container, childFragment)
            .commit()

        tvWeatherRecommendation = view.findViewById(R.id.tvWeatherRecommendation)

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fetchWeatherAndUpdateRecommendation() // Fetch weather data when the map is created

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

        arguments?.let {
            focusedLat = it.getDouble("focus_lat", 0.0)
            focusedLng = it.getDouble("focus_lng", 0.0)
        }

        return view
    }

    @SuppressLint("MissingPermission")
    private fun getUserLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                val userLatLng = LatLng(location.latitude, location.longitude)
                mMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 15f))

                // Remove old marker if exists
                currentLocationMarker?.remove()
                currentLocationMarker = mMap?.addMarker(
                    MarkerOptions().position(userLatLng).title("You are here!")
                )
            } else {
                Toast.makeText(requireContext(), "Could not get location", Toast.LENGTH_SHORT).show()
            }
        }
    }



    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        val mykonosCenter = LatLng(37.4467, 25.3289)
        mMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(mykonosCenter, 12f))

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            mMap?.isMyLocationEnabled = true
        } else {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        }

        focusedLat?.let { lat ->
            focusedLng?.let { lng ->
                val focusedPosition = LatLng(lat, lng)
                mMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(focusedPosition, 15f))
            }
        }
        loadMarkersFromFirestore()
    }


    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getUserLocation()
        } else {
            Toast.makeText(requireContext(), "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }



    private fun fetchWeatherAndUpdateRecommendation() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val jsonData = withContext(Dispatchers.IO) {
                    val client = OkHttpClient()
                    val apiKey = "200b857da17d668dbf479de6ff89c982"
                    val url =
                        "https://api.openweathermap.org/data/2.5/weather?q=Mykonos,GR&units=metric&appid=$apiKey"
                    val request = Request.Builder().url(url).build()
                    val response = client.newCall(request).execute()
                    response.body?.string()
                }

                if (jsonData != null) {
                    val jsonObject = JSONObject(jsonData)
                    val main = jsonObject.getJSONObject("main")
                    val temp = main.getDouble("temp")
                    val weatherDescription =
                        jsonObject.getJSONArray("weather").getJSONObject(0).getString("description")

                    val recommendation = when {
                        temp < 10 -> "It's a chilly ${temp}°C with ${weatherDescription}! Visit Mykonos’ museums 🏛️ or cozy up in a seaside taverna 🍷."
                        temp in 10.0..20.0 -> "The temperature is ${temp}°C with ${weatherDescription}. Enjoy a scenic walk through Mykonos Town 🌆 or visit Little Venice! 🌅"
                        temp in 20.0..30.0 -> "It's a warm ${temp}°C in Mykonos! Perfect for a beach day at Paradise Beach 🏖️ or a boat tour to Delos! ⛵"
                        else -> "It's hot ${temp}°C with ${weatherDescription}! Chill at a luxurious Mykonos beach club 🏝️ or grab a refreshing cocktail 🍹."
                    }

                    // Update the UI
                    tvWeatherRecommendation.text = recommendation
                } else {
                    tvWeatherRecommendation.text = "Weather data not available for Mykonos"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                tvWeatherRecommendation.text = "Weather data not available for Mykonos"
            }
        }
    }

    private fun loadMarkersFromFirestore() {
        val db = FirebaseFirestore.getInstance()
        db.collection("posts")
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val post = document.toObject(Post::class.java)

                    if (post.place_lat != 0.0 && post.place_lng != 0.0 && post.place_name.isNotEmpty()) {
                        val position = LatLng(post.place_lat, post.place_lng)

                        val markerOptions = MarkerOptions()
                            .position(position)
                            .title(post.place_name)
                            .snippet(post.description)

                        val marker = mMap?.addMarker(markerOptions)

                        if (post.place_lat == focusedLat && post.place_lng == focusedLng) {
                            marker?.showInfoWindow()
                            mMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(position, 15f))
                        }
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to load markers", Toast.LENGTH_SHORT).show()
            }
    }


}
