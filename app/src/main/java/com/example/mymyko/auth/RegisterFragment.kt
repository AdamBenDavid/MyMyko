package com.example.mymyko.auth


import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.mymyko.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase

class RegisterFragment : Fragment() {
  // form fields
  private lateinit var etEmail: EditText
  private lateinit var etFirstName: EditText
  private lateinit var etLastName: EditText
  private lateinit var etCountry: EditText
  private lateinit var etPassword: EditText
  private lateinit var btnSignUp: Button
  private lateinit var tvRedirectLogin: TextView
  private lateinit var auth: FirebaseAuth

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    //load xml
    val view = inflater.inflate(R.layout.fragment_register, container, false)

    etFirstName = view.findViewById(R.id.firstname_field)
    etLastName = view.findViewById(R.id.lastname_field)
    etEmail = view.findViewById(R.id.email_field)
    etPassword = view.findViewById(R.id.password_field)
    etCountry = view.findViewById(R.id.country_field)
    btnSignUp = view.findViewById(R.id.register_submit_button)
    tvRedirectLogin = view.findViewById(R.id.register_login_button)

    auth = Firebase.auth

    // on click "signup"
    btnSignUp.setOnClickListener { signUpUser() }

    // on click "login" - navigate to login page
    tvRedirectLogin.setOnClickListener {
      findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
    }
    return view
  }

  // register user
  private fun signUpUser() {
    val firstName = etFirstName.text.toString().trim()
    val lastName = etLastName.text.toString().trim()
    val email = etEmail.text.toString().trim()
    val password = etPassword.text.toString().trim()
    val country = etCountry.text.toString().trim()

    if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || password.isEmpty() || country.isEmpty()) {
      Toast.makeText(requireContext(), "All fields are required", Toast.LENGTH_SHORT).show()
      return
    }

    // create user in fireBase
    auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
      if (task.isSuccessful) {
        val user = auth.currentUser
        user?.let {
          saveUserToFirestore(it, firstName, lastName, country) //save user to firestore
        }
        Toast.makeText(requireContext(), "Successfully Signed Up", Toast.LENGTH_SHORT).show()
        auth.signOut() // logout user

        //navigate to login page
        findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
      } else {
        //if fail
        Log.w("Register", "createUserWithEmail:failure", task.exception)
        Toast.makeText(requireContext(), "Sign Up Failed", Toast.LENGTH_SHORT).show()
      }
    }
  }


  private fun saveUserToFirestore(user: FirebaseUser, firstName: String, lastName: String, country: String) {
    val db = FirebaseFirestore.getInstance()
    val userData = hashMapOf(
      "firstname" to firstName,
      "lastname" to lastName,
      "email" to user.email,
      "city" to "",
      "country" to country,
      "profileImageUrl" to "",
      "role" to "user"
    )

    db.collection("users").document(user.uid)
      .set(userData)
      .addOnSuccessListener {
        Log.d("Firestore", "User data saved successfully")
      }
      .addOnFailureListener { e ->
        Log.e("Firestore", "Error saving user data", e)
      }
  }
}
