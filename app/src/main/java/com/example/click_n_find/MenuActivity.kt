package com.example.click_n_find

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MenuActivity : BaseActivity() {
    private lateinit var buttonLogOff: Button
    private lateinit var buttonInfo: Button
    private lateinit var buttonPlay: Button
    private lateinit var profileImageView: ImageView
    private lateinit var profileName: TextView
    private lateinit var seconds: TextView
    private lateinit var points: TextView
    private lateinit var totalGames: TextView
    private lateinit var record: TextView
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

        auth = FirebaseAuth.getInstance()

        initializeGoogleSignIn()

        // Initialize profile elements
        profileImageView = findViewById(R.id.profileImageView)
        profileName = findViewById(R.id.profileTextView)
        seconds = findViewById(R.id.secondsTextView)
        points = findViewById(R.id.pointsTextView)
        totalGames = findViewById(R.id.totalPlayedTextView)
        record = findViewById(R.id.recordTextView)

        // Initialize buttons
        buttonLogOff = findViewById(R.id.buttonLogOff)
        buttonInfo = findViewById(R.id.buttonInfo)
        buttonPlay = findViewById(R.id.buttonPlay)

        // Set onClickListener for buttonLogOff
        buttonLogOff.setOnClickListener {
            logOff()
        }

        buttonInfo.setOnClickListener{
            val intent = Intent(this, InfoActivity::class.java)
            startActivity(intent)
        }

        buttonPlay.setOnClickListener{
            val intent = Intent(this, GameActivity::class.java)
            startActivity(intent)
        }

        // Display user's profile information
        displayUserInfo()
    }

    private fun logOff() {
        // Sign out of Firebase Authentication
        auth.signOut()

        // Sign out of Google Sign-In
        googleSignInClient.signOut().addOnCompleteListener(this) {
            // Return to the MainActivity
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun displayUserInfo() {
        val user: FirebaseUser? = auth.currentUser

        user?.let {
            // User is signed in
            val photoUrl = user.photoUrl
            val displayName = user.displayName

            // Set profile picture
            photoUrl?.let {
                Glide.with(this)
                    .load(it)
                    .circleCrop()
                    .into(profileImageView)
            }

            // Set user's name
            displayName?.let {
                profileName.text = it
            }

            // Fetch additional information from the database
            fetchAdditionalUserInfo(user.uid)
        }
    }

    private fun fetchAdditionalUserInfo(uid: String) {
        val userReference = FirebaseDatabase.getInstance().reference.child("users").child(uid)

        userReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    // User data exists in the database
                    val userData = snapshot.value as? Map<String, Any>

                    // Now you can access the additional information and update your UI
                    userData?.let {
                        val secondsValue = it["totalSecondsPlayed"] as? Long ?: 0
                        val pointsValue = it["totalPoints"] as? Long ?: 0
                        val gamesPlayedValue = it["gamesPlayed"] as? Long ?: 0
                        val recordValue = it["recordTime"] as? Long ?: 0

                        // Update your UI with the fetched information

                        seconds.text = getString(R.string.user_info_1, secondsValue)
                        points.text = getString(R.string.user_info_2, pointsValue)
                        totalGames.text = getString(R.string.user_info_3, gamesPlayedValue)
                        record.text = getString(R.string.user_info_4, recordValue)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }
}