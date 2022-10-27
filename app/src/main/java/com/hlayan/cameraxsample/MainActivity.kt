package com.hlayan.cameraxsample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.hlayan.cameraxsample.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        val transaction = supportFragmentManager.beginTransaction()
        transaction.add(binding.container.id, CapturePhotoFragment())
        transaction.commit()
    }
}