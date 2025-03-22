package com.example.mymyko

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomnavigation.BottomNavigationView

class BottomNavFragment : Fragment() {
  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    val view = inflater.inflate(R.layout.fragment_bottom_nav, container, false) //gets xml file
    val navigationBar = view.findViewById<BottomNavigationView>(R.id.bottom_navigation)
    val currentPage = arguments?.getString("current_page")

    // circle by current page
    when (currentPage) {
      "home" -> navigationBar.selectedItemId = R.id.home
      "profile" -> navigationBar.selectedItemId = R.id.profile
      "upload" -> navigationBar.selectedItemId = R.id.upload
      "map" -> navigationBar.selectedItemId = R.id.map
    }

    // navigate by item's click
    // call HandleMove
    navigationBar.setOnItemSelectedListener { item: MenuItem ->
      when (item.itemId) {
        R.id.home -> {
          Log.d("BottomNavFragment", "Home button clicked")
          handleMove(currentPage, "home")
          true
        }
        R.id.profile -> {
          Log.d("BottomNavFragment", "Profile button clicked")
          handleMove(currentPage, "profile")
          true
        }
        R.id.upload -> {
          Log.d("BottomNavFragment", "Upload button clicked")
          handleMove(currentPage, "upload")
          true
        }
        R.id.map -> {
          Log.d("BottomNavFragment", "Map button clicked")
          handleMove(currentPage, "map")
          true
        }
        R.id.logout -> {
          Log.d("BottomNavFragment", "Logout button clicked")
          handleMove(currentPage, "logout")
          true
        }
        else -> false
      }
    }
    return view
  }

  // the navigate manager
  fun handleMove(currentPage: String?, newPage: String) {
    when (currentPage) {
      "home" -> when (newPage) {
        "profile" -> findNavController().navigate(R.id.action_homeFragment_to_profileFragment)
        "upload"  -> findNavController().navigate(R.id.action_homeFragment_to_uploadFragment)
        "map"     -> findNavController().navigate(R.id.action_global_cafeMapFragment)
        "logout"  -> findNavController().navigate(R.id.action_homeFragment_to_logoutFragment)
      }
      "profile" -> when (newPage) {
        "home"    -> findNavController().navigate(R.id.action_profileFragment_to_homeFragment)
        "upload"  -> findNavController().navigate(R.id.action_profileFragment_to_uploadFragment)
        "map"     -> findNavController().navigate(R.id.action_global_cafeMapFragment)
        "logout"  -> findNavController().navigate(R.id.action_profileFragment_to_logoutFragment)
      }
      "map" -> when (newPage) { // ✅ Now properly handles BACK navigation
        "home"    -> findNavController().navigate(R.id.action_mapFragment_to_home) // ✅ Goes back to HomeFragment
        "profile" -> findNavController().navigate(R.id.action_mapFragment_to_profileFragment) // ✅ Goes back to ProfileFragment
        "upload"  -> findNavController().navigate(R.id.action_mapFragment_to_uploadFragment) // ✅ Goes back to UploadFragment
        "logout"  -> findNavController().navigate(R.id.action_mapFragment_to_logoutFragment) // ✅ Logs out properly
      }
      "upload" -> when (newPage) {
        "home"    -> findNavController().navigate(R.id.action_uploadFragment_to_homeFragment)
        "profile" -> findNavController().navigate(R.id.action_uploadFragment_to_profileFragment)
        "map"     -> findNavController().navigate(R.id.action_global_cafeMapFragment)
        "logout"  -> findNavController().navigate(R.id.action_uploadFragment_to_logoutFragment)
      }
      else -> {
        findNavController().navigate(R.id.action_global_cafeMapFragment)
      }
    }
  }
}
