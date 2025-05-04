package com.example.skycast.view

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.skycast.R
import com.example.skycast.viewmodel.MainViewModel

@Composable
fun LanguageSelectionDialog(
    viewModel: MainViewModel,
    onLanguageChange: (String) -> Unit // Callback to trigger activity restart
) {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }
    var selectedLanguageCode by remember { mutableStateOf(viewModel.language) }
    val languages = listOf(
        "en" to context.getString(R.string.english),
        "hi" to context.getString(R.string.hindi),
        "pa" to context.getString(R.string.punjabi),
        "mr" to context.getString(R.string.marathi),
        "gu" to context.getString(R.string.gujarati),
        "bn" to context.getString(R.string.bengali),
        "ta" to context.getString(R.string.tamil),
        "te" to context.getString(R.string.telugu),
        "ml" to context.getString(R.string.malayalam),
        "kn" to context.getString(R.string.kannada)
    )

    // Button to open the dialog
    Button(
        onClick = { showDialog = true },
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = context.getString(R.string.change_language) }
    ) {
        Text(context.getString(R.string.change_language))
    }

    // Dialog for language selection
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = {
                Text(
                    text = context.getString(R.string.language_selection),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.semantics { contentDescription = context.getString(R.string.language_selection) }
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .semantics { contentDescription = context.getString(R.string.language_selection_dialog) }
                ) {
                    languages.forEach { (code, name) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .semantics { contentDescription = "${context.getString(R.string.language_label)}: $name" },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedLanguageCode == code,
                                onClick = { selectedLanguageCode = code },
                                modifier = Modifier.semantics { contentDescription = "${context.getString(R.string.language_label)}: $name" }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = name,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.semantics { contentDescription = name }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setLanguage(context, selectedLanguageCode)
                        showDialog = false // Dismiss dialog before restart
                        onLanguageChange(selectedLanguageCode) // Trigger activity restart
                    },
                    modifier = Modifier.semantics { contentDescription = context.getString(R.string.set_language) }
                ) {
                    Text(context.getString(R.string.set_language))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDialog = false },
                    modifier = Modifier.semantics { contentDescription = context.getString(R.string.cancel) }
                ) {
                    Text(context.getString(R.string.cancel))
                }
            },
            modifier = Modifier.semantics { contentDescription = context.getString(R.string.language_selection_dialog) }
        )
    }
}