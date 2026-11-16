package com.bookappp.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.bookappp.app.ui.navigation.BookNavHost
import com.bookappp.app.ui.theme.BookApppTheme
import dagger.hilt.android.AndroidEntryPoint

// AppCompatActivity: Readium's navigator fragments need a FragmentActivity host,
// and AppCompatActivity is the Hilt-supported concrete implementation.
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BookApppTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    BookNavHost()
                }
            }
        }
    }
}
