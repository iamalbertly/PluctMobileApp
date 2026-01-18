package app.pluct.ui.components

import android.content.Intent
import android.net.Uri
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
import app.pluct.data.preferences.PluctUserPreferences
import app.pluct.data.preferences.PluctUserPreferencesInlineHint
import app.pluct.services.PluctCoreAPIUnifiedService
import kotlinx.coroutines.launch

/**
 * Pluct-UI-Component-07Onboarding-01Tutorial-01Flow
 * Multi-step onboarding tutorial that guides users through the TikTok share flow
 * Follows naming convention: [Project]-[UI]-[Component]-[Onboarding]-[Tutorial]-[Sequence][Flow]
 */

private const val TAG = "OnboardingTutorial"

/**
 * Main tutorial flow composable
 * Shows 3 steps: How It Works → Visual Instructions → Open TikTok
 */
@Composable
fun PluctUIComponent07Onboarding01Tutorial01Flow(
    onComplete: () -> Unit,
    onSkip: () -> Unit,
    apiService: PluctCoreAPIUnifiedService? = null,
    onBalanceRefresh: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val prefs = remember { PluctUserPreferences(context) }
    val scope = rememberCoroutineScope()
    var currentStep by remember { mutableStateOf(0) }

    Log.d(TAG, "Onboarding tutorial started, step: $currentStep")

    AlertDialog(
        onDismissRequest = { /* Prevent accidental dismiss */ },
        title = null,
        text = {
            when (currentStep) {
                0 -> TutorialStep1HowItWorks()
                1 -> TutorialStep2VisualInstructions()
                2 -> TutorialStep3OpenTikTok()
            }
        },
        confirmButton = {
            when (currentStep) {
                0 -> {
                    Button(
                        onClick = {
                            Log.d(TAG, "Onboarding step 1 completed")
                            currentStep = 1
                        },
                        modifier = Modifier.semantics {
                            testTag = "onboarding_next_button"
                            contentDescription = "Next step"
                        }
                    ) {
                        Text("Next")
                    }
                }
                1 -> {
                    Button(
                        onClick = {
                            Log.d(TAG, "Onboarding step 2 completed")
                            currentStep = 2
                        },
                        modifier = Modifier.semantics {
                            testTag = "onboarding_got_it_button"
                            contentDescription = "Got it"
                        }
                    ) {
                        Text("Got It")
                    }
                }
                2 -> {
                    val isTikTokInstalled = remember {
                        try {
                            // Use getLaunchIntentForPackage - more reliable than getPackageInfo
                            // Check multiple possible TikTok package names
                            val packageNames = listOf(
                                "com.zhiliaoapp.musically",  // Standard TikTok
                                "com.ss.android.ugc.aweme"   // Alternative package name
                            )
                            packageNames.any { packageName ->
                                try {
                                    val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
                                    if (launchIntent != null) {
                                        Log.d(TAG, "TikTok detected via getLaunchIntentForPackage: $packageName")
                                        true
                                    } else {
                                        false
                                    }
                                } catch (e: Exception) {
                                    Log.d(TAG, "TikTok check failed for $packageName: ${e.message}")
                                    false
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error checking TikTok installation: ${e.message}")
                            false
                        }
                    }
                    Button(
                        onClick = {
                            Log.d(TAG, "Onboarding tutorial completed - opening TikTok")
                            prefs.markOnboardingTutorialSeen()
                            
                            // Trigger balance refresh to surface welcome bonus
                            scope.launch {
                                try {
                                    Log.d(TAG, "Tutorial completed - triggering balance refresh for potential reward")
                                    onBalanceRefresh?.invoke()
                                    
                                    // Show celebration toast
                                    android.widget.Toast.makeText(
                                        context,
                                        "🎉 Tutorial complete! Check your credits",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                } catch (e: Exception) {
                                    Log.e(TAG, "Failed to refresh balance after tutorial: ${e.message}")
                                    // Show optimistic celebration even if refresh fails
                                    android.widget.Toast.makeText(
                                        context,
                                        "Tutorial complete! Reward pending...",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                            
                            launchTikTok(context)
                            onComplete()
                        },
                        modifier = Modifier.semantics {
                            testTag = "onboarding_open_tiktok_button"
                            contentDescription = if (isTikTokInstalled) "Open TikTok" else "Get TikTok"
                        }
                    ) {
                        Text(if (isTikTokInstalled) "Open TikTok" else "Get TikTok")
                    }
                }
            }
        },
        dismissButton = {
            if (currentStep == 2) {
                TextButton(
                    onClick = {
                        Log.d(TAG, "Onboarding tutorial skipped")
                        prefs.markOnboardingTutorialSeen()
                        
                        // Enable inline hint for users who skip
                        PluctUserPreferencesInlineHint.setInlineHintEnabled(context, true)
                        Log.d(TAG, "Inline hint enabled for skipped tutorial")
                        
                        onSkip()
                    },
                    modifier = Modifier.semantics {
                        testTag = "onboarding_skip_button"
                        contentDescription = "I'll Figure It Out"
                    }
                ) {
                    Text("I'll Figure It Out")
                }
            }
        },
        modifier = Modifier.semantics {
            testTag = "onboarding_tutorial_dialog"
            contentDescription = "Onboarding tutorial"
        }
    )
}

/**
 * Step 1: How It Works - Explains the 3-tap flow
 */
@Composable
private fun TutorialStep1HowItWorks() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .semantics {
                testTag = "onboarding_step_1"
                contentDescription = "Tutorial step 1 - How it works"
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Get Transcripts in 3 Taps",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Step indicators
        TutorialStepItem(number = "1", text = "Find a TikTok video")
        TutorialStepItem(number = "2", text = "Tap Share, then Pluct")
        TutorialStepItem(number = "3", text = "Get your transcript instantly")

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Your 3 free transcriptions are ready to use!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Step 2: Visual Instructions - Shows where the share button is
 */
@Composable
private fun TutorialStep2VisualInstructions() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .semantics {
                testTag = "onboarding_step_2"
                contentDescription = "Tutorial step 2 - Visual instructions"
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Find the Share Button",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Visual representation using text/emoji
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "In TikTok, look for the curved arrow",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )

                // Arrow icon representation
                Text(
                    text = "\u27A4", // Right arrow
                    fontSize = 48.sp
                )

                Text(
                    text = "on the right side of any video",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Then scroll down and tap \"Pluct\"",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Step 3: Open TikTok - CTA to launch TikTok and practice
 */
@Composable
private fun TutorialStep3OpenTikTok() {
    val context = LocalContext.current
    val isTikTokInstalled = remember {
        try {
            // Use getLaunchIntentForPackage - more reliable than getPackageInfo
            // Check multiple possible TikTok package names
            val packageNames = listOf(
                "com.zhiliaoapp.musically",  // Standard TikTok
                "com.ss.android.ugc.aweme"   // Alternative package name
            )
            packageNames.any { packageName ->
                try {
                    val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
                    if (launchIntent != null) {
                        Log.d("OnboardingTutorial", "TikTok detected via getLaunchIntentForPackage: $packageName")
                        true
                    } else {
                        false
                    }
                } catch (e: Exception) {
                    Log.d("OnboardingTutorial", "TikTok check failed for $packageName: ${e.message}")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e("OnboardingTutorial", "Error checking TikTok installation: ${e.message}")
            false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .semantics {
                testTag = "onboarding_step_3"
                contentDescription = "Tutorial step 3 - Open TikTok"
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Ready to Try?",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (isTikTokInstalled) {
            Text(
                text = "Open TikTok and share any video with Pluct to get your first transcript!",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "TikTok not installed",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "You can paste TikTok links directly into the app, or install TikTok for easier sharing.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Reminder about credits
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Text(
                text = "3 free transcriptions ready!",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(12.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Individual step item with number indicator
 */
@Composable
private fun TutorialStepItem(number: String, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        // Number badge
        Surface(
            modifier = Modifier.size(32.dp),
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.primary
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = number,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

/**
 * Launch TikTok app or Play Store if not installed
 * Uses getLaunchIntentForPackage for reliable detection and launching
 */
private fun launchTikTok(context: android.content.Context) {
    Log.d(TAG, "TikTok launch intent triggered")
    
    // Try multiple TikTok package names using getLaunchIntentForPackage (most reliable method)
    val packageNames = listOf(
        "com.zhiliaoapp.musically",  // Standard TikTok
        "com.ss.android.ugc.aweme"   // Alternative package name
    )
    
    var launched = false
    for (packageName in packageNames) {
        try {
            // Use getLaunchIntentForPackage - this is the most reliable way to check and launch
            val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                Log.d(TAG, "Launching TikTok app with package: $packageName")
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                launched = true
                break
            } else {
                Log.d(TAG, "No launch intent found for package: $packageName")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch TikTok with package $packageName: ${e.message}", e)
        }
    }
    
    if (!launched) {
        // TikTok not installed - open Play Store
        Log.d(TAG, "TikTok not found via getLaunchIntentForPackage, opening Play Store")
        try {
            val playStoreIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("market://details?id=com.zhiliaoapp.musically")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(playStoreIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open Play Store: ${e.message}", e)
            // Fallback to web Play Store
            try {
                val webIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=com.zhiliaoapp.musically")
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
            } catch (e2: Exception) {
                Log.e(TAG, "Failed to open web Play Store: ${e2.message}", e2)
            }
        }
    }
}
