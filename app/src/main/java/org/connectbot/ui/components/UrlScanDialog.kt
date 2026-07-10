/*
 * ConnectBot: simple, powerful, open-source SSH client for Android
 * Copyright 2025-2026 Kenny Root
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

package org.connectbot.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.connectbot.R
import org.connectbot.ui.common.InputFieldShape

@Composable
fun UrlScanDialog(
    urls: List<String>,
    onDismiss: () -> Unit,
    onUrlClick: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.console_menu_urlscan), style = MaterialTheme.typography.titleMedium) },
        text = {
            if (urls.isEmpty()) {
                Text(
                    text = stringResource(R.string.empty_urls_message),
                    modifier = Modifier.padding(16.dp),
                )
            } else {
                LazyColumn {
                    items(urls) { url ->
                        Card(
                            shape = InputFieldShape,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                .clickable {
                                    onUrlClick(url)
                                    onDismiss()
                                },
                        ) {
                            Text(
                                text = url,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                        HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.button_close))
            }
        },
    )
}
