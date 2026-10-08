package com.adityaram.present.ui.about

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adityaram.present.ui.theme.LocalPresentColors
import com.adityaram.present.ui.theme.Typography
import com.adityaram.present.ui.theme.Dimens
import com.adityaram.present.ui.components.PresentTopBar

data class OssLibrary(
    val name: String,
    val version: String,
    val license: String,
    val url: String? = null
)

private val ossLibraries = listOf(
    OssLibrary("AndroidX Core KTX", "1.13.1", "Apache License 2.0"),
    OssLibrary("AndroidX Lifecycle Runtime", "2.8.5", "Apache License 2.0"),
    OssLibrary("Jetpack Compose", "1.6.0+", "Apache License 2.0"),
    OssLibrary("Compose Navigation", "2.8.0", "Apache License 2.0"),
    OssLibrary("Firebase BoM", "33.1.2", "Apache License 2.0"),
    OssLibrary("Firebase Authentication", "Included via BoM", "Apache License 2.0"),
    OssLibrary("Firebase Firestore", "Included via BoM", "Apache License 2.0"),
    OssLibrary("Firebase App Check", "Included via BoM", "Apache License 2.0"),
    OssLibrary("Play Services Auth", "21.2.0", "Apache License 2.0"),
    OssLibrary("OkHttp", "4.12.0", "Apache License 2.0", "https://square.github.io/okhttp/"),
    OssLibrary("Room Database", "2.6.1", "Apache License 2.0"),
    OssLibrary("Kotlin Coroutines", "1.7.3", "Apache License 2.0"),
    OssLibrary("Gson", "2.10.1", "Apache License 2.0"),
    OssLibrary("KSP Symbol Processing", "1.9.0", "Apache License 2.0")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpenSourceLicensesScreen(onBack: () -> Unit) {
    val colors = LocalPresentColors.current

    Box(modifier = Modifier.fillMaxSize().background(colors.background).statusBarsPadding()) {
        Column(modifier = Modifier.fillMaxSize()) {
            PresentTopBar(
                title = "Open-source licenses",
                onBack = onBack
            )
            
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = Dimens.screenHorizontalPadding),
            contentPadding = PaddingValues(bottom = Dimens.spacing40, top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(ossLibraries) { lib ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.surface)
                        .padding(16.dp)
                ) {
                    Text(
                        text = lib.name,
                        style = Typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = colors.primaryText
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = "Version: ${lib.version}",
                        style = Typography.bodySmall,
                        color = colors.secondaryText
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = "License: ${lib.license}",
                        style = Typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.accent
                    )
                    
                    if (lib.url != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = lib.url,
                            style = Typography.labelSmall,
                            color = colors.mutedText
                        )
                    }
                }
            }
        }
    }
}
}
