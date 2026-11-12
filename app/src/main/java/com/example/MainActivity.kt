package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.MainViewModel
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.ParentDashboardScreen
import com.example.ui.screens.TeacherDashboardScreen
import com.example.ui.screens.WelcomePortalScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SchoolAppRoot()
                }
            }
        }
    }
}

@Composable
fun SchoolAppRoot(viewModel: MainViewModel = viewModel()) {
    val context = LocalContext.current
    val currentPortal by viewModel.currentPortal.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()

    // Display any toast messages
    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    // Back handling: If on secondary portal, return to Home
    if (currentPortal != "HOME") {
        BackHandler {
            viewModel.selectPortal("HOME")
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (currentPortal) {
            "HOME" -> WelcomePortalScreen(
                viewModel = viewModel,
                onNavigateToAdmin = { viewModel.selectPortal("ADMIN") },
                onNavigateToTeacher = { viewModel.selectPortal("TEACHER") },
                onNavigateToParent = { viewModel.selectPortal("PARENT") }
            )
            "ADMIN" -> AdminDashboardScreen(
                viewModel = viewModel,
                onBack = { viewModel.selectPortal("HOME") }
            )
            "TEACHER" -> TeacherDashboardScreen(
                viewModel = viewModel,
                onBack = { viewModel.selectPortal("HOME") }
            )
            "PARENT" -> ParentDashboardScreen(
                viewModel = viewModel,
                onBack = { viewModel.selectPortal("HOME") }
            )
            else -> WelcomePortalScreen(
                viewModel = viewModel,
                onNavigateToAdmin = { viewModel.selectPortal("ADMIN") },
                onNavigateToTeacher = { viewModel.selectPortal("TEACHER") },
                onNavigateToParent = { viewModel.selectPortal("PARENT") }
            )
        }
    }
}
