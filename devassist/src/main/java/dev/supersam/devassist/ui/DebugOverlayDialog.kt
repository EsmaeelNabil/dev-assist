package dev.supersam.devassist.ui

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.DialogFragment
import dev.supersam.devassist.api.DebugAction
import dev.supersam.devassist.api.DebugInfoProvider
import dev.supersam.devassist.api.DebugOverlayRegistry
import dev.supersam.devassist.features.CollapsibleInfoSection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class DebugOverlayDialog : DialogFragment() {

    companion object {
        const val TAG = "DebugOverlayDialog"
    }

    private val dialogScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                DebugOverlayScreen(
                    providers = DebugOverlayRegistry.infoProviders,
                    actions = DebugOverlayRegistry.actions,
                    onDismiss = { dismissAllowingStateLoss() },
                    dialogScope = dialogScope
                )
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)

        dialog.setOnShowListener {
            dialog.window?.setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
            )
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        }
        return dialog
    }

    override fun getTheme(): Int {

        return androidx.appcompat.R.style.Theme_AppCompat_Light_NoActionBar
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)


    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugOverlayScreen(
    providers: List<DebugInfoProvider>,
    actions: List<DebugAction>,
    onDismiss: () -> Unit,
    dialogScope: CoroutineScope
) {
    val context = LocalContext.current
    var showSnackbarMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(showSnackbarMessage) {
        showSnackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            showSnackbarMessage = null
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Debug Overlay") },
                actions = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            item {
                Text(
                    "Information",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 8.dp, top = 8.dp)
                )
            }
            items(providers) { provider ->

                CollapsibleInfoSection(
                    title = provider.title

                ) {

                    provider.Content(context = context)
                }
            }


            item { Spacer(modifier = Modifier.height(16.dp)) }


            item {
                Text(
                    "Actions",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            items(actions) { action ->
                DebugActionButton(
                    action,
                    dialogScope,
                    snackbarHostState
                )
            }


            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
fun DebugActionButton(
    action: DebugAction,
    scope: CoroutineScope,
    snackbarHostState: SnackbarHostState
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Button(
        onClick = {
            if (!isLoading) {
                isLoading = true
                scope.launch {
                    var message: String = ""
                    try {
                        action.onAction(context)
                        message = "${action.title} executed."
                    } catch (e: Exception) {
                        Log.e("DebugAction", "Action '${action.title}' failed", e)
                        message =
                            "Error executing ${action.title}: ${e.message?.take(100)}"
                    } finally {
                        isLoading = false

                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(message)
                        }
                    }
                }
            }
        },
        modifier = Modifier.fillMaxWidth(),
        enabled = !isLoading
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(action.title)
    }
}
