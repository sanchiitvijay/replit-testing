package app.lastcalled

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import app.lastcalled.ui.CallTrackerViewModel
import app.lastcalled.ui.screens.CallTrackerRoot
import app.lastcalled.ui.theme.LastCalledTheme

class MainActivity : ComponentActivity() {
    private val viewModel: CallTrackerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LastCalledTheme {
                CallTrackerRoot(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            viewModel.onAppResumed()
        }
    }
}
