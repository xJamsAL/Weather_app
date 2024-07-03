package com.example.weatherapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.weatherapp.fragment.MainFragment


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        supportFragmentManager
            .beginTransaction().replace(R.id.placeholder, MainFragment.newInstance())
            .commit()
    }
}