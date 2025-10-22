package app.pluct.ui.components

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/**
 * Pluct-Error-Notification-Test - Test component for error notifications
 * Simple test component to trigger error notifications for testing
 */
@Composable
fun PluctErrorNotificationTest(
    errorNotificationManager: ErrorNotificationManager,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Error Notification Test",
            style = MaterialTheme.typography.headlineSmall
        )
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    Log.d("PluctErrorNotificationTest", "Triggering network error")
                    errorNotificationManager.showNetworkError()
                }
            ) {
                Text("Network Error")
            }
            
            Button(
                onClick = {
                    Log.d("PluctErrorNotificationTest", "Triggering validation error")
                    errorNotificationManager.showValidationError("Invalid URL format")
                }
            ) {
                Text("Validation Error")
            }
        }
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    Log.d("PluctErrorNotificationTest", "Triggering API error")
                    errorNotificationManager.showApiError("API request failed")
                }
            ) {
                Text("API Error")
            }
            
            Button(
                onClick = {
                    Log.d("PluctErrorNotificationTest", "Triggering timeout error")
                    errorNotificationManager.showTimeoutError()
                }
            ) {
                Text("Timeout Error")
            }
        }
        
        Button(
            onClick = {
                Log.d("PluctErrorNotificationTest", "Dismissing error")
                errorNotificationManager.dismiss()
            }
        ) {
            Text("Dismiss Error")
        }
    }
}
