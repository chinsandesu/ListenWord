package com.yourcompany.worklisten.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavController
import com.yourcompany.worklisten.R
import kotlinx.coroutines.delay

class SplashScreen {
    companion object {
        const val SPLASH_DURATION = 1000L // 1 second
    }
}

@Composable
fun SplashScreen(navController: NavController) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.app_icon),
            contentDescription = "App Icon",
            modifier = Modifier.fillMaxSize(0.5f)
        )
    }

    LaunchedEffect(Unit) {
        delay(SplashScreen.SPLASH_DURATION)
        navController.navigate("main") {
            popUpTo("splash") { inclusive = true }
        }
    }
}