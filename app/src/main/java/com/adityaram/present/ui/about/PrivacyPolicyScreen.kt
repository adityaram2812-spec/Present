package com.adityaram.present.ui.about

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adityaram.present.ui.theme.LocalPresentColors
import com.adityaram.present.ui.theme.Typography
import com.adityaram.present.ui.theme.Dimens
import com.adityaram.present.ui.components.PresentTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    val colors = LocalPresentColors.current
    val scrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize().background(colors.background).statusBarsPadding()) {
        Column(modifier = Modifier.fillMaxSize()) {
            PresentTopBar(
                title = "Privacy Policy",
                onBack = onBack
            )
            
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = Dimens.screenHorizontalPadding)
                    .padding(bottom = Dimens.spacing40)
                    .padding(top = Dimens.spacing16)
            ) {
            Text(
                text = "Last updated: October 2026",
                style = Typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = colors.mutedText,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            PolicySection("1. Overview", "Present is designed as a local-first utility. We believe in keeping your academic tracking strictly on your own device boundaries unless you explicitly request cloud enhancements.")
            
            PolicySection("2. Data stored locally", "All core data—including your subjects, timetable slots, default thresholds, academic preferences, and manually marked attendance transactions—is stored locally in a secure SQLite database on your device.")
            
            PolicySection("3. Account information", "Present offers an optional Google Sign-In system architecture. If you choose to authenticate, Firebase Authentication securely manages your session state. A synchronized metadata copy of your provider details (Name, Email, Profile Picture URL) is retained in a secure private Firestore document.")
            
            PolicySection("4. Timetable import", "Timetable importing is a cloud-based premium feature. When explicitly initiated, Present securely transmits the provided image payload to our backend server for OCR geometry extraction.")
            
            PolicySection("5. What is sent to cloud services", "Only images selected explicitly during the Timetable Import flow, alongside your Authentication token (for quota tracking), are sent to cloud infrastructure. Present utilizes Firebase App Check leveraging Play Integrity to prevent API abuse.")
            
            PolicySection("6. Authentication", "Authentication is powered entirely through Firebase Identity infrastructure adhering to all Google policies regarding token lifecycles and retention.")
            
            PolicySection("7. Attendance data", "Present does NOT upload, sync, backup, read, or distribute your daily attendance logging to the cloud. The backend has strictly zero visibility into your classes or attendance logic.")
            
            PolicySection("8. Data retention", "Locally stored data is fully destroyed when you clear app data or uninstall Present. For authenticated cloud footprints, you may delete your account metadata interacting securely through Firebase Auth flows.")
            
            PolicySection("9. Third-party services", "We utilize Google Firebase (Auth, App Check, Firestore) to facilitate connectivity quotas, and Render computing platforms for processing OCR payloads.")
            
            PolicySection("10. Security", "All data in transit relies on TLS encryption. Additionally, authenticated paths enforce App Check attestation preventing unauthorized execution of external cloud workflows.")
            
            PolicySection("11. User control", "You remain strictly in control of application state. You can utilize the entirety of Present’s attendance logic anonymously with local isolation.")
            
            PolicySection("12. Contact", "If you require further data disclosure, please reach out via the Help & feedback channels. (TODO: Insert legal contact alias)")
        }
    }
}
}

@Composable
fun PolicySection(title: String, body: String) {
    val colors = LocalPresentColors.current
    Column(modifier = Modifier.padding(bottom = 24.dp)) {
        Text(
            text = title,
            style = Typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp),
            color = colors.primaryText,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Text(
            text = body,
            style = Typography.bodyMedium,
            color = colors.secondaryText,
            lineHeight = 22.sp
        )
    }
}
