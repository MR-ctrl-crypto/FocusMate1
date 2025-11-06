package com.example.focusmate

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class AuthActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // This activity will use the same layout that MainActivity was using before,
        // which contains the NavHostFragment.
        setContentView(R.layout.activity_auth)
    }
}
