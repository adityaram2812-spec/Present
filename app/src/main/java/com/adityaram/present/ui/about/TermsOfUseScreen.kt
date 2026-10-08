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
fun TermsOfUseScreen(onBack: () -> Unit) {
    val colors = LocalPresentColors.current
    val scrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize().background(colors.background).statusBarsPadding()) {
        Column(modifier = Modifier.fillMaxSize()) {
            PresentTopBar(
                title = "Terms of Use",
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

            PolicySection("1. Acceptance of terms", "By accessing or utilizing Present, you agree to these Terms of Use outlining appropriate application interaction architectures and functional limitations.")
            
            PolicySection("2. Use of Present", "Present provides software tools intended exclusively for academic recording, attendance simulations, and schedule tracking logic.")
            
            PolicySection("3. Account responsibility", "If opting into cloud-based mechanisms, you are strictly responsible for maintaining your authentication scope and any content passed onto the extraction engine.")
            
            PolicySection("4. Timetable import", "Timetable extraction consumes deterministic quotas tracked securely through backend transaction layers. Abuse, bypass attempts, or injection scripts querying the App Check endpoints are highly prohibited.")
            
            PolicySection("5. Accuracy of timetable extraction", "Present incorporates heuristic geometry and generative inference to parse raw pictures. You explicitly acknowledge you must fundamentally review the extraction blueprint before committing to the Room database. We are immune to algorithmic inaccuracies.")
            
            PolicySection("6. User responsibility for attendance records", "The tracking application is designed strictly as a local reference tool. WE ACCEPT NO RESPONSIBILITY IF PRESENT REPORTS A SAFE ATTENDANCE THRESHOLD THAT DIVERGES FROM YOUR ACADEMIC INSTITUTION'S OFFICIAL REGULATION SYSTEM. Your academic success relies on institutional verification.")
            
            PolicySection("7. Service availability", "The backend proxy for timetable imports has no guaranteed uptime SLA. Network disruption or token revocation may pause cloud features independently of the local app.")
            
            PolicySection("8. Intellectual property", "The design aesthetic, client codebase, and backend architectures retain ownership. Any embedded components retain rights per their open-source licensing constraints.")
            
            PolicySection("9. Third-party services", "This app proxies requests to cloud vendors to fulfill extraction capabilities.")
            
            PolicySection("10. Changes to the service", "We reserve full rights to adjust heuristic limitations horizontally globally as system architecture necessitates without explicit upfront broadcast alerts.")
            
            PolicySection("11. Disclaimer", "Present is distributed on an 'AS IS' foundation free of expressed or implied hardware warranties. The system bears zero indemnification.")
            
            PolicySection("12. Contact", "Direct legal/abuse inquiries to the relevant administrative channels via the in-app Help panel. (TODO: Add registered representative address)")
        }
    }
}
}
