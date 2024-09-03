package com.example.click_n_find

import android.content.Intent
import android.os.Bundle
import android.widget.Button

class InfoActivity : BaseActivity() {
    private lateinit var buttonReturn: Button
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_info)

        buttonReturn = findViewById(R.id.buttonReturnToMenu)

        buttonReturn.setOnClickListener{
            val intent = Intent(this, MenuActivity::class.java)
            startActivity(intent)
        }
    }
}