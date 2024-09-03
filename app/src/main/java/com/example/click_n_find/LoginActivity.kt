package com.example.click_n_find

import android.os.Bundle
import android.widget.Button
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import android.content.Intent
import android.widget.Toast
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import android.net.Uri;
import android.widget.VideoView;

@Suppress("NAME_SHADOWING")
class LoginActivity : BaseActivity() {
    private lateinit var googleAuth: Button
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var videoView: VideoView
    private val RC_SIGN_IN = 20
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        googleAuth = findViewById(R.id.buttonAuth)
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        videoView = findViewById(R.id.videoView)

        initializeGoogleSignIn()

        googleAuth.setOnClickListener {
            googleSignIn()
        }

        if(auth.currentUser != null) run {
            val intent = Intent(this, MenuActivity::class.java)
            startActivity(intent)
            finish()
        }

        // Set up VideoView
        setupVideoView()
    }


    private fun googleSignIn() {
        val intent = googleSignInClient.signInIntent
        startActivityForResult(intent,RC_SIGN_IN)
    }


    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode==RC_SIGN_IN){
            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
            try{
                val account: GoogleSignInAccount = task.getResult(ApiException::class.java)
                firebaseAuth(account.idToken)
            }
            catch (_: Exception){

            }
        }
    }

    private fun firebaseAuth(idToken: String?) {
        val cred: AuthCredential = GoogleAuthProvider.getCredential(idToken, null)

        auth.signInWithCredential(cred)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user: FirebaseUser? = auth.currentUser

                    val map: HashMap<String, Any> = HashMap()
                    user?.let {
                        map["name"] = it.displayName ?: ""
                        map["profileImage"] = it.photoUrl?.toString() ?: ""
                    }

                    val userReference = database.reference.child("users").child(user?.uid ?: "")

                    // Check if the user already exists in the database
                    userReference.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (!snapshot.exists()) {
                                // User is new, add extra fields
                                map["gamesPlayed"] = 0
                                map["recordTime"] = 0
                                map["totalSecondsPlayed"] = 0
                                map["totalPoints"] = 0
                            }

                            // Update the specific fields without affecting other fields
                            map.forEach { (key, value) ->
                                userReference.child(key).setValue(value)
                            }

                            val intent = Intent(this@LoginActivity, MenuActivity::class.java)
                            startActivity(intent)
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Toast.makeText(this@LoginActivity, "Database error", Toast.LENGTH_SHORT).show()
                        }
                    })
                } else {
                    Toast.makeText(this, "Something went wrong", Toast.LENGTH_SHORT).show()
                }
            }
    }
    private fun setupVideoView() {
        val videoUri = Uri.parse("android.resource://" + packageName + "/raw/video")
        videoView.setVideoURI(videoUri)
        videoView.setOnPreparedListener { mp -> mp.isLooping = true }
        videoView.start()
    }
}