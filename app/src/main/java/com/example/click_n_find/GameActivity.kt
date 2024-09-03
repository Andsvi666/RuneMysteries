package com.example.click_n_find

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.SystemClock
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.TextView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class GameActivity : BaseActivity() {
    private lateinit var buttonReturn: Button
    private lateinit var currentTime: TextView
    private lateinit var handler: Handler
    private var startTime: Long = 0
    private var isRunning: Boolean = false
    private lateinit var mainGridLayout: GridLayout
    private val runeImages = intArrayOf(
        R.drawable.rune_1,
        R.drawable.rune_2,
        R.drawable.rune_3,
        R.drawable.rune_4,
        R.drawable.rune_5,
        R.drawable.rune_6,
        R.drawable.rune_7,
        R.drawable.rune_8,
        R.drawable.rune_9,
        R.drawable.rune_10,
    )
    private var sequenceOfRunes: List<Int> = List(20) { 0 }
    private var revealedIndices = mutableListOf<Int>()
    private var matchedIndices = mutableListOf<Int>()
    private var isProcessingClick = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        //Return button
        buttonReturn = findViewById(R.id.buttonReturnToMenu)
        buttonReturn.setOnClickListener{
            val intent = Intent(this, MenuActivity::class.java)
            startActivity(intent)
        }

        //Dynamic timer
        currentTime = findViewById(R.id.gameCurrentTime)

        // Set random 2 set of 10 sequence
        setUpRandomSequence()
        Log.d("SequenceOfCards", sequenceOfRunes.toString())

        // Setup Grid
        mainGridLayout = findViewById(R.id.mainGridLayout)
        placeClosedRunes()

        // Start the timer automatically
        startTimer()
    }

    private fun setUpRandomSequence(){
        // Create two lists containing numbers from 1 to 10
        val firstSet = (1..10).toList()
        val secondSet = (1..10).toList()

        // Shuffle each list to randomize the order of numbers
        val random = java.util.Random()
        sequenceOfRunes = firstSet + secondSet
        sequenceOfRunes = sequenceOfRunes.shuffled(random)
    }

    private fun placeClosedRunes() {
        for (index in sequenceOfRunes.indices) {
            // Create ImageView for closed rune
            val imageView = ImageView(this)
            imageView.setImageResource(R.drawable.rune_backside) // Replace with your closed rune image resource

            // Add ImageView to GridLayout
            val params = GridLayout.LayoutParams().apply {
                width = 0 // Adjust width as needed
                height = 0 // Adjust height as needed
                rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f) // Stretch to fill row
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f) // Stretch to fill column
                setMargins(30, 10, 30, 10) // Set margins as needed
            }
            imageView.layoutParams = params
            mainGridLayout.addView(imageView)

            // Add click listener to reveal rune image
            imageView.setOnClickListener {
                revealRune(index)
            }
        }
    }

    private fun revealRune(index: Int) {
        if (!isProcessingClick && !revealedIndices.contains(index) && !matchedIndices.contains(index)) {
            isProcessingClick = true
            val runeID = sequenceOfRunes[index]
            val runeImageResource = runeImages[runeID - 1]
            val imageView = mainGridLayout.getChildAt(index) as ImageView
            imageView.setImageResource(runeImageResource)

            revealedIndices.add(index)

            if (revealedIndices.size == 2) {
                mainGridLayout.isEnabled = false // Disable clicks on the grid
                handler.postDelayed({ checkForMatch() }, 100) // Delay for 0.1 second
            } else {
                isProcessingClick = false // Reset the flag if only one image is revealed
            }
        }
    }

    private fun checkForMatch() {
        val firstIndex = revealedIndices[0]
        val secondIndex = revealedIndices[1]
        val firstRuneID = sequenceOfRunes[firstIndex]
        val secondRuneID = sequenceOfRunes[secondIndex]

        if (firstRuneID == secondRuneID) {
            matchedIndices.addAll(revealedIndices)
            if (matchedIndices.size == sequenceOfRunes.size) {
                // Game finished
                finishGame()
            }
        } else {
            // If the runes don't match, hide them after a short delay
            handler.postDelayed({
                hideRune(firstIndex)
                hideRune(secondIndex)
                revealedIndices.clear()
                mainGridLayout.isEnabled = true // Re-enable clicks on the grid
                isProcessingClick = false // Reset the flag
            }, 100) // Delay for 0.1 second
        }

        // Clear revealedIndices after processing
        revealedIndices.clear()

        // Reset the processing click flag
        isProcessingClick = false
    }

    private fun hideRune(index: Int) {
        val imageView = mainGridLayout.getChildAt(index) as ImageView
        imageView.setImageResource(R.drawable.rune_backside)
    }

    private fun finishGame() {
        stopTimer()
        val elapsedTime = SystemClock.elapsedRealtime() - startTime
        val score = calculateScore(elapsedTime)
        updatePlayerInfo(score, elapsedTime)
        displayPopout(score, elapsedTime)
    }

    private fun calculateScore(elapsedTime: Long): Long {
        // Default score for completing the game in 2 minutes
        val seconds = elapsedTime / 1000
        var score: Long = 120

        // Calculate the score based on the elapsed time
        score = score - seconds

        // If the calculated score is less than 10, give a minimum score of 10
        if (score < 10) {
            score = 10
        }

        return score
    }

    private fun updatePlayerInfo(score: Long, time: Long) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        userId?.let { id ->
            val databaseRef = FirebaseDatabase.getInstance().reference.child("users").child(id)

            // Update experience and gamesPlayed
            databaseRef.child("totalPoints").addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val currentPoints = dataSnapshot.getValue(Long::class.java) ?: 0
                    databaseRef.child("totalPoints").setValue(currentPoints + score)
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Handle error
                }
            })

            databaseRef.child("gamesPlayed").addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val gamesPlayed = dataSnapshot.getValue(Long::class.java) ?: 0
                    databaseRef.child("gamesPlayed").setValue(gamesPlayed + 1)
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Handle error
                }
            })

            databaseRef.child("totalSecondsPlayed").addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val secondsPlayed = dataSnapshot.getValue(Long::class.java) ?: 0
                    databaseRef.child("totalSecondsPlayed").setValue(secondsPlayed + (time / 1000)) // Convert milliseconds to seconds
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Handle error
                }
            })

            // Update recordTime if necessary
            databaseRef.child("recordTime").addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val currentRecordTime = dataSnapshot.getValue(Long::class.java) ?: Long.MAX_VALUE
                    if (currentRecordTime == 0L || currentRecordTime > time / 1000) { // Convert milliseconds to seconds
                        databaseRef.child("recordTime").setValue(time / 1000) // Convert milliseconds to seconds
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Handle error
                }
            })
        }
    }

    private fun displayPopout(score: Long, time: Long) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.results_popup_layout, null)

        val scoreTextView: TextView = dialogView.findViewById(R.id.scoreTextView)
        val timeTextView: TextView = dialogView.findViewById(R.id.timeTextView)
        val returnButton: Button = dialogView.findViewById(R.id.buttonReturnToMenu)

        scoreTextView.text = getString(R.string.text_current_score, score)

        // Extract seconds and tenths-
        val seconds = time / 1000
        val tenths = (time % 1000) / 100
        timeTextView.text = getString(R.string.text_current_time, seconds, tenths)

        val builder = AlertDialog.Builder(this)
            .setView(dialogView)

        val dialog = builder.create()
        dialog.show()

        returnButton.setOnClickListener {
            dialog.dismiss()
            // Handle button click, for example, return to menu activity
            val intent = Intent(this, MenuActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun startTimer() {
        isRunning = true
        startTime = SystemClock.elapsedRealtime()
        handler = Handler()
        handler.postDelayed(updateTimerRunnable, 100) // Update every 100 milliseconds
    }

    private val updateTimerRunnable = object : Runnable {
        override fun run() {
            val elapsedTime = SystemClock.elapsedRealtime() - startTime
            val seconds = elapsedTime / 1000
            val tenths = (elapsedTime % 1000) / 100
            updateTimerText(seconds, tenths)
            handler.postDelayed(this, 100) // Schedule next update in 100 milliseconds
        }
    }

    private fun updateTimerText(seconds: Long, tenths: Long) {
        val formattedTime = getString(R.string.text_current_time, seconds, tenths)
        currentTime.text = formattedTime
    }

    private fun stopTimer() {
        isRunning = false
        handler.removeCallbacks(updateTimerRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateTimerRunnable)
    }
}
