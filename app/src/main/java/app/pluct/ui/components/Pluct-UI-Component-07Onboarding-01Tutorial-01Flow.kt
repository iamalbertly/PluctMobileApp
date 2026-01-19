package app.pluct.ui.components

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.pluct.core.platform.PluctCorePlatform01TikTok01Detector
import app.pluct.data.preferences.PluctUserPreferences
import app.pluct.data.preferences.PluctUserPreferencesInlineHint
import app.pluct.services.PluctCoreAPIUnifiedService
import kotlinx.coroutines.launch

/**
 * Pluct-UI-Component-07Onboarding-01Tutorial-01Flow
 * Multi-step onboarding tutorial for TikTok share flow.
 * 6 scope layers: Project, UI, Component, Onboarding, Tutorial, Flow
 *
 * REFACTORED: Uses PluctCorePlatform01TikTok01Detector (single source of truth)
 * to eliminate 3x duplicate TikTok detection logic.
 */

private const val TAG = "OnboardingTutorial"

/**
 * Main tutorial flow - 3 steps: How It Works → Visual Instructions → Open TikTok
 */
@Composable
fun PluctUIComponent07Onboarding01Tutorial01Flow(
    onComplete: () -> Unit,
    onSkip: () -> Unit,
    @Suppress("UNUSED_PARAMETER") apiService: PluctCoreAPIUnifiedService? = null,
    onBalanceRefresh: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val prefs = remember { PluctUserPreferences(context) }
    val scope = rememberCoroutineScope()
    var currentStep by remember { mutableStateOf(0) }

    // Single TikTok detection using consolidated utility
    val isTikTokInstalled = remember { PluctCorePlatform01TikTok01Detector.isInstalled(context) }

    Log.d(TAG, "Onboarding tutorial started, step: $currentStep")

    AlertDialog(
        onDismissRequest = { /* Prevent accidental dismiss */ },
        title = null,
        text = {
            when (currentStep) {
                0 -> TutorialStep1HowItWorks()
                1 -> TutorialStep2VisualInstructions()
                2 -> TutorialStep3OpenTikTok(isTikTokInstalled)
            }
        },
        confirmButton = {
            when (currentStep) {
                0 -> Button(
                    onClick = { currentStep = 1 },
                    modifier = Modifier.semantics { testTag = "onboarding_next_button" }
                ) { Text("Next") }

                1 -> Button(
                    onClick = { currentStep = 2 },
                    modifier = Modifier.semantics { testTag = "onboarding_got_it_button" }
                ) { Text("Got It") }

                2 -> Button(
                    onClick = {
                        Log.d(TAG, "Tutorial completed - launching TikTok")
                        prefs.markOnboardingTutorialSeen()
                        scope.launch {
                            onBalanceRefresh?.invoke()
                            android.widget.Toast.makeText(context, "Tutorial complete!", android.widget.Toast.LENGTH_SHORT).show()
                        }
                        PluctCorePlatform01TikTok01Detector.launchOrInstall(context)
                        onComplete()
                    },
                    modifier = Modifier.semantics { testTag = "onboarding_open_tiktok_button" }
                ) { Text(if (isTikTokInstalled) "Open TikTok" else "Get TikTok") }
            }
        },
        dismissButton = {
            if (currentStep == 2) {
                TextButton(
                    onClick = {
                        Log.d(TAG, "Tutorial skipped")
                        prefs.markOnboardingTutorialSeen()
                        PluctUserPreferencesInlineHint.setInlineHintEnabled(context, true)
                        onSkip()
                    },
                    modifier = Modifier.semantics { testTag = "onboarding_skip_button" }
                ) { Text("I'll Figure It Out") }
            }
        },
        modifier = Modifier.semantics { testTag = "onboarding_tutorial_dialog" }
    )
}

@Composable
private fun TutorialStep1HowItWorks() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(8.dp).semantics { testTag = "onboarding_step_1" },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Get Transcripts in 3 Taps", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(8.dp))
        TutorialStepItem("1", "Find a TikTok video")
        TutorialStepItem("2", "Tap Share, then Pluct")
        TutorialStepItem("3", "Get your transcript instantly")
        Spacer(modifier = Modifier.height(8.dp))
        Text("Your 3 free transcriptions are ready to use!", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
    }
}

@Composable
private fun TutorialStep2VisualInstructions() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(8.dp).semantics { testTag = "onboarding_step_2" },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Find the Share Button", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(8.dp))
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("In TikTok, look for the curved arrow", style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
                Text("\u27A4", fontSize = 48.sp)
                Text("on the right side of any video", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text("Then scroll down and tap \"Pluct\"", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
    }
}

@Composable
private fun TutorialStep3OpenTikTok(isTikTokInstalled: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(8.dp).semantics { testTag = "onboarding_step_3" },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Ready to Try?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(8.dp))
        if (isTikTokInstalled) {
            Text("Open TikTok and share any video with Pluct to get your first transcript!", style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("TikTok not installed", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
                Text("You can paste TikTok links directly, or install TikTok for easier sharing.", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Text("3 free transcriptions ready!", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(12.dp), textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun TutorialStepItem(number: String, text: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Start) {
        Surface(modifier = Modifier.size(32.dp), shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.primary) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text(number, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}
