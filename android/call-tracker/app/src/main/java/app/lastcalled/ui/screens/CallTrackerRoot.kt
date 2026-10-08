package app.lastcalled.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.lastcalled.domain.ContactSummary
import app.lastcalled.ui.CallTrackerViewModel

private enum class AppScreen { HOME, DETAIL, SETTINGS }

@Composable
fun CallTrackerRoot(viewModel: CallTrackerViewModel) {
    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current
    var permissionRevision by remember { mutableIntStateOf(0) }
    var skipContactsStep by rememberSaveable { mutableStateOf(false) }
    var screen by rememberSaveable { mutableStateOf(AppScreen.HOME) }
    var selectedPerson by remember { mutableStateOf<ContactSummary?>(null) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissionRevision++
                viewModel.onAppResumed()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            viewModel.onAppResumed()
        }
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val hasCallLogAccess = remember(permissionRevision) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) ==
            PackageManager.PERMISSION_GRANTED
    }
    val hasContactsAccess = remember(permissionRevision) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED
    }

    val callLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        permissionRevision++
        if (granted) viewModel.refresh()
    }
    val contactsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        permissionRevision++
        if (granted) {
            skipContactsStep = false
            viewModel.refresh()
        }
    }

    fun openPermissionSettings() {
        context.startActivity(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:${context.packageName}"),
            ),
        )
    }

    fun requestPermission(
        permission: String,
        prefKey: String,
        launch: (String) -> Unit,
    ) {
        val preferences = context.getSharedPreferences("permission_prompts", 0)
        val alreadyAsked = preferences.getBoolean(prefKey, false)
        val shouldExplain = activity?.shouldShowRequestPermissionRationale(permission) == true
        if (alreadyAsked && !shouldExplain) {
            openPermissionSettings()
        } else {
            preferences.edit().putBoolean(prefKey, true).apply()
            launch(permission)
        }
    }

    BackHandler(enabled = screen != AppScreen.HOME) {
        screen = if (screen == AppScreen.DETAIL) AppScreen.HOME else AppScreen.HOME
        viewModel.closeContact()
    }

    if (!hasCallLogAccess || (!hasContactsAccess && !skipContactsStep)) {
        PermissionGate(
            isCallLogStep = !hasCallLogAccess,
            permanentlyDenied = remember(permissionRevision) {
                val preferenceKey = if (!hasCallLogAccess) "call_log_requested" else "contacts_requested"
                val permission = if (!hasCallLogAccess) Manifest.permission.READ_CALL_LOG
                else Manifest.permission.READ_CONTACTS
                context.getSharedPreferences("permission_prompts", 0).getBoolean(preferenceKey, false) &&
                    activity?.shouldShowRequestPermissionRationale(permission) != true
            },
            onRequestCallLog = {
                requestPermission(
                    Manifest.permission.READ_CALL_LOG,
                    "call_log_requested",
                    { permission -> callLauncher.launch(permission) },
                )
            },
            onRequestContacts = {
                requestPermission(
                    Manifest.permission.READ_CONTACTS,
                    "contacts_requested",
                    { permission -> contactsLauncher.launch(permission) },
                )
            },
            onOpenSettings = ::openPermissionSettings,
            onContinueWithoutContacts = { skipContactsStep = true },
        )
        return
    }

    when (screen) {
        AppScreen.HOME -> HomeScreen(
            viewModel = viewModel,
            hasContactsAccess = hasContactsAccess,
            onOpenSettings = { screen = AppScreen.SETTINGS },
            onOpenPerson = { person ->
                selectedPerson = person
                viewModel.openContact(person.contactId)
                screen = AppScreen.DETAIL
            },
            onManagePermissions = ::openPermissionSettings,
        )
        AppScreen.DETAIL -> PersonDetailScreen(
            viewModel = viewModel,
            initialPerson = selectedPerson,
            onBack = {
                screen = AppScreen.HOME
                viewModel.closeContact()
            },
        )
        AppScreen.SETTINGS -> SettingsScreen(
            viewModel = viewModel,
            hasCallLogAccess = hasCallLogAccess,
            hasContactsAccess = hasContactsAccess,
            onBack = { screen = AppScreen.HOME },
            onManagePermissions = ::openPermissionSettings,
        )
    }
}
