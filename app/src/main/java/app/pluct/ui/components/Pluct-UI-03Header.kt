package app.pluct.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Pluct-UI-03Header - Modern header component with credit balance and settings
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 * Provides proper content descriptions for UI testing
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluctHeader(
    creditBalance: Int,
    isCreditBalanceLoading: Boolean,
    creditBalanceError: String?,
    onRefreshCreditBalance: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Pluct",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.semantics {
                        contentDescription = "Pluct app title"
                    }
                )
                
                // Credit Balance Display
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .semantics {
                            contentDescription = "Credit balance display showing $creditBalance credits"
                        }
                ) {
                    Text(
                        text = "💎",
                        fontSize = 18.sp,
                        modifier = Modifier.semantics {
                            contentDescription = "Diamond icon representing credits"
                        }
                    )
                    
                    Spacer(modifier = Modifier.width(4.dp))
                    
                    if (isCreditBalanceLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(16.dp)
                                .semantics {
                                    contentDescription = "Loading credit balance"
                                }
                        )
                    } else if (creditBalanceError != null) {
                        Text(
                            text = "Error",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 14.sp,
                            modifier = Modifier.semantics {
                                contentDescription = "Credit balance error: $creditBalanceError"
                            }
                        )
                    } else {
                        Text(
                            text = "$creditBalance",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.semantics {
                                contentDescription = "Credit balance: $creditBalance credits"
                            }
                        )
                    }
                }
            }
        },
        actions = {
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier.semantics {
                    contentDescription = "Settings button"
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    modifier = Modifier.semantics {
                        contentDescription = "Settings icon"
                    }
                )
            }
        },
        modifier = modifier.semantics {
            contentDescription = "App header with credit balance and settings"
        }
    )
}

/**
 * Alternative header with clickable credit balance for refresh
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluctHeaderWithRefreshableBalance(
    creditBalance: Int,
    isCreditBalanceLoading: Boolean,
    creditBalanceError: String?,
    onRefreshCreditBalance: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Pluct",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.semantics {
                        contentDescription = "Pluct app title"
                    }
                )
                
                // Clickable Credit Balance Display
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { onRefreshCreditBalance() }
                        .semantics {
                            contentDescription = "Credit balance display showing $creditBalance credits - tap to refresh"
                        }
                ) {
                    Text(
                        text = "💎",
                        fontSize = 18.sp,
                        modifier = Modifier.semantics {
                            contentDescription = "Diamond icon representing credits"
                        }
                    )
                    
                    Spacer(modifier = Modifier.width(4.dp))
                    
                    if (isCreditBalanceLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(16.dp)
                                .semantics {
                                    contentDescription = "Loading credit balance"
                                }
                        )
                    } else if (creditBalanceError != null) {
                        Text(
                            text = "Error",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 14.sp,
                            modifier = Modifier.semantics {
                                contentDescription = "Credit balance error: $creditBalanceError"
                            }
                        )
                    } else {
                        Text(
                            text = "$creditBalance",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.semantics {
                                contentDescription = "Credit balance: $creditBalance credits"
                            }
                        )
                    }
                }
            }
        },
        actions = {
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier.semantics {
                    contentDescription = "Settings button"
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    modifier = Modifier.semantics {
                        contentDescription = "Settings icon"
                    }
                )
            }
        },
        modifier = modifier.semantics {
            contentDescription = "App header with refreshable credit balance and settings"
        }
    )
}
