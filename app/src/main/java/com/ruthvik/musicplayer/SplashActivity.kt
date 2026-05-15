package com.ruthvik.musicplayer

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ruthvik.musicplayer.databinding.ActivitySplashBinding

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupDarkEdgeToEdge()
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.main.applySystemBarInsets()

        binding.btnStart.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
    }
}