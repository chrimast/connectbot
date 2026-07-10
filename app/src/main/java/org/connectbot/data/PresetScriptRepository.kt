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

package org.connectbot.data

import android.content.SharedPreferences
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class PresetScript(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val script: String,
)

class PresetScriptRepository(
    private val prefs: SharedPreferences,
) {
    fun getScripts(): List<PresetScript> {
        val json = prefs.getString(PREF_PRESET_SCRIPTS, null) ?: return DEFAULT_SCRIPTS
        return runCatching { parseScripts(json) }.getOrDefault(DEFAULT_SCRIPTS)
    }

    fun saveScripts(scripts: List<PresetScript>) {
        prefs.edit { putString(PREF_PRESET_SCRIPTS, scripts.toJsonString()) }
    }

    fun upsertScript(script: PresetScript) {
        val scripts = getScripts().toMutableList()
        val index = scripts.indexOfFirst { it.id == script.id }
        if (index == -1) {
            scripts.add(script)
        } else {
            scripts[index] = script
        }
        saveScripts(scripts)
    }

    fun deleteScript(scriptId: String) {
        saveScripts(getScripts().filterNot { it.id == scriptId })
    }

    private fun parseScripts(json: String): List<PresetScript> {
        val array = JSONArray(json)
        return List(array.length()) { index ->
            val item = array.getJSONObject(index)
            PresetScript(
                id = item.getString("id"),
                name = item.getString("name"),
                script = item.getString("script"),
            )
        }.filter { it.name.isNotBlank() && it.script.isNotBlank() }
    }

    private fun List<PresetScript>.toJsonString(): String {
        val array = JSONArray()
        forEach { script ->
            array.put(
                JSONObject()
                    .put("id", script.id)
                    .put("name", script.name)
                    .put("script", script.script),
            )
        }
        return array.toString()
    }

    companion object {
        private const val PREF_PRESET_SCRIPTS = "preset_scripts"

        val DEFAULT_SCRIPTS = listOf(
            PresetScript(id = "default-pwd", name = "查看当前目录", script = "pwd\n"),
            PresetScript(id = "default-ls", name = "列出文件", script = "ls -la\n"),
            PresetScript(id = "default-df", name = "查看磁盘空间", script = "df -h\n"),
        )
    }
}
