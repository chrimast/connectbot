/*
 * ConnectBot: simple, powerful, open-source SSH client for Android
 * Copyright 2026 Kenny Root
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.connectbot.ui.screens.presetscripts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.preference.PreferenceManager
import org.connectbot.R
import org.connectbot.data.PresetScript
import org.connectbot.data.PresetScriptRepository
import org.connectbot.ui.common.InputFieldShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresetScriptListScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val repository = remember {
        PresetScriptRepository(PreferenceManager.getDefaultSharedPreferences(context))
    }
    var scripts by remember { mutableStateOf(repository.getScripts()) }
    var editingScript by remember { mutableStateOf<PresetScript?>(null) }
    var showEditor by remember { mutableStateOf(false) }

    fun refreshScripts() {
        scripts = repository.getScripts()
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.preset_scripts_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.button_navigate_up),
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingScript = null
                    showEditor = true
                },
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = stringResource(R.string.button_add),
                )
            }
        },
    ) { padding ->
        if (scripts.isEmpty()) {
            Text(
                text = stringResource(R.string.preset_scripts_empty),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp),
            ) {
                items(scripts, key = { it.id }) { script ->
                    PresetScriptRow(
                        script = script,
                        onEdit = {
                            editingScript = script
                            showEditor = true
                        },
                        onDelete = {
                            repository.deleteScript(script.id)
                            refreshScripts()
                        },
                    )
                }
            }
        }
    }

    if (showEditor) {
        PresetScriptEditorDialog(
            script = editingScript,
            onDismiss = { showEditor = false },
            onSave = { script ->
                repository.upsertScript(script)
                refreshScripts()
                showEditor = false
            },
        )
    }
}

@Composable
private fun PresetScriptRow(
    script: PresetScript,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onEdit),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = script.name,
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = script.script,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.button_remove),
                )
            }
        }
    }
}

@Composable
private fun PresetScriptEditorDialog(
    script: PresetScript?,
    onDismiss: () -> Unit,
    onSave: (PresetScript) -> Unit,
) {
    var name by remember(script) { mutableStateOf(script?.name.orEmpty()) }
    var body by remember(script) { mutableStateOf(script?.script.orEmpty()) }
    val canSave = name.isNotBlank() && body.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (script == null) {
                        R.string.preset_script_add_title
                    } else {
                        R.string.preset_script_edit_title
                    },
                ),
                style = MaterialTheme.typography.titleMedium,
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    shape = InputFieldShape,
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.preset_script_name_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    shape = InputFieldShape,
                    value = body,
                    onValueChange = { body = it },
                    label = { Text(stringResource(R.string.preset_script_body_label)) },
                    minLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSave,
                onClick = {
                    val savedScript = script?.copy(name = name.trim(), script = body)
                        ?: PresetScript(name = name.trim(), script = body)
                    onSave(savedScript)
                },
            ) {
                Text(stringResource(R.string.preset_script_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.button_cancel))
            }
        },
    )
}
