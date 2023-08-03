package com.kynetics.android_i2c_example_app

import android.os.Bundle
import android.os.PersistableBundle
import android.text.method.LinkMovementMethod
import androidx.appcompat.app.AppCompatActivity
import com.kynetics.android_i2c_example_app.databinding.ActivityAboutBinding

class AboutActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAboutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.aboutText.movementMethod = LinkMovementMethod.getInstance()
    }

}