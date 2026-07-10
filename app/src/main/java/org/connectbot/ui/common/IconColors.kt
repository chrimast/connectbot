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

package org.connectbot.ui.common

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.svg.SvgDecoder
import org.connectbot.R
import org.connectbot.ui.screens.hostlist.ConnectionState

/**
 * Color data for visual identification of hosts/profiles.
 * Country codes are the primary key for new entries.
 * English names and hex values are kept for backward compatibility with old database entries.
 */
data class ColorOption(
    val countryCode: String,   // "cn" — PRIMARY DB key
    val englishName: String,   // "China" — legacy fallback
    val hexValue: String,      // "#DE2910" — for ShortcutIconGenerator + old DB compat
    val localizedName: String, // populated by getIconColors() from string resources
    val flagEmoji: String,     // "🇨🇳"
)

private val iconColorsData = listOf(
    ColorOption("cn", "China", "#DE2910", "", "🇨🇳"),
    ColorOption("us", "United States", "#3C3B6E", "", "🇺🇸"),
    ColorOption("hk", "Hong Kong", "#DE2910", "", "🇭🇰"),
    ColorOption("jp", "Japan", "#BC002D", "", "🇯🇵"),
    ColorOption("sg", "Singapore", "#ED2939", "", "🇸🇬"),
    ColorOption("tw", "Taiwan", "#FE0000", "", "🇹🇼"),
    ColorOption("de", "Germany", "#DD0000", "", "🇩🇪"),
    ColorOption("ru", "Russia", "#0039A6", "", "🇷🇺"),
    ColorOption("nl", "Netherlands", "#21468B", "", "🇳🇱"),
    ColorOption("fr", "France", "#002395", "", "🇫🇷"),
    ColorOption("gb", "United Kingdom", "#012169", "", "🇬🇧"),
    ColorOption("in", "India", "#FF9933", "", "🇮🇳"),
    ColorOption("xx", "Other", "#9E9E9E", "", "🏳️"),
)

/**
 * 13 country-based icon colors for visual identification of hosts/profiles.
 */
@Composable
fun getIconColors(): List<ColorOption> = iconColorsData.map {
    val resId = when (it.countryCode) {
        "cn" -> R.string.country_cn
        "us" -> R.string.country_us
        "hk" -> R.string.country_hk
        "jp" -> R.string.country_jp
        "sg" -> R.string.country_sg
        "tw" -> R.string.country_tw
        "de" -> R.string.country_de
        "ru" -> R.string.country_ru
        "nl" -> R.string.country_nl
        "fr" -> R.string.country_fr
        "gb" -> R.string.country_gb
        "in" -> R.string.country_in
        "xx" -> R.string.country_xx
        else -> R.string.country_xx
    }
    it.copy(localizedName = stringResource(resId))
}

// Match by countryCode → hexValue → englishName → null
fun findColorOption(colorValue: String?): ColorOption? {
    if (colorValue.isNullOrBlank()) return null
    return iconColorsData.find { it.countryCode == colorValue }
        ?: iconColorsData.find { it.hexValue.equals(colorValue, ignoreCase = true) }
        ?: iconColorsData.find { it.englishName.equals(colorValue, ignoreCase = true) }
}

// Country code → hex, or pass-through if already hex (old DB entries)
fun resolveColorToHex(colorValue: String?): String? {
    if (colorValue.isNullOrBlank()) return null
    // If it looks like a hex code, return as-is
    if (colorValue.startsWith("#")) return colorValue
    return iconColorsData.find { it.countryCode == colorValue }?.hexValue ?: colorValue
}

// Match by countryCode → hexValue → englishName → "🏳️" fallback
fun getFlagEmojiForColor(colorValue: String?): String {
    if (colorValue.isNullOrBlank()) return "🏳️"
    return iconColorsData.find { it.countryCode == colorValue }?.flagEmoji
        ?: iconColorsData.find { it.hexValue.equals(colorValue, ignoreCase = true) }?.flagEmoji
        ?: iconColorsData.find { it.englishName.equals(colorValue, ignoreCase = true) }?.flagEmoji
        ?: "🏳️"
}

/**
 * A circular host icon rendering the national flag SVG matching the host's
 * assigned colour, surrounded by a coloured border reflecting the connection state.
 */
@Composable
fun FlagIcon(
    colorValue: String?,
    borderColor: Color,
    connectionState: ConnectionState,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val countryCode = findColorOption(colorValue)?.countryCode ?: "xx"

    val flagResId = context.resources.getIdentifier(
        "flag_$countryCode", "raw", context.packageName,
    )
    val resId = if (flagResId != 0) flagResId else
        context.resources.getIdentifier("flag_xx", "raw", context.packageName)

    val contentDescription = when (connectionState) {
        ConnectionState.CONNECTED -> stringResource(R.string.image_description_connected)
        ConnectionState.DISCONNECTED -> stringResource(R.string.image_description_disconnected)
        ConnectionState.UNKNOWN -> null
    }

    Box(
        modifier = modifier.size(40.dp),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(resId)
                .decoderFactory(SvgDecoder.Factory())
                .build(),
            contentDescription = contentDescription,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .border(width = 3.dp, color = borderColor, shape = CircleShape),
            contentScale = ContentScale.Crop,
        )
    }
}
