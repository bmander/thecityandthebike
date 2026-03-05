package com.thecityandthebike.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.thecityandthebike.data.network.LocalNetworkBannerShowing
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.thecityandthebike.data.model.dto.LeaderboardEntry
import com.thecityandthebike.ui.components.LoadingIndicator
import com.thecityandthebike.ui.viewmodel.LeaderboardPeriod
import com.thecityandthebike.ui.viewmodel.LeaderboardState

@Composable
fun LeaderboardContent(
    state: LeaderboardState,
    onPeriodSelected: (LeaderboardPeriod) -> Unit,
    onUserClick: (String) -> Unit,
    onClearError: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Period sub-tabs
        val periods = LeaderboardPeriod.entries
        val selectedIndex = periods.indexOf(state.selectedPeriod)
        TabRow(selectedTabIndex = selectedIndex) {
            periods.forEachIndexed { index, period ->
                Tab(
                    selected = index == selectedIndex,
                    onClick = { onPeriodSelected(period) },
                    text = { Text(period.displayName) }
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                state.isLoading -> {
                    LoadingIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                state.error != null && !LocalNetworkBannerShowing.current -> {
                    Snackbar(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp),
                        action = {
                            TextButton(onClick = onClearError) {
                                Text("Dismiss")
                            }
                        }
                    ) {
                        Text(state.error)
                    }
                }
                state.entries.isEmpty() -> {
                    Text(
                        text = "No submissions yet",
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                else -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(state.entries, key = { it.userId }) { entry ->
                            LeaderboardRow(
                                entry = entry,
                                onClick = { onUserClick(entry.userId) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LeaderboardRow(
    entry: LeaderboardEntry,
    onClick: () -> Unit
) {
    val isTopThree = entry.rank <= 3
    val textColor = if (isTopThree) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    val fontWeight = if (isTopThree) FontWeight.Bold else FontWeight.Normal
    val textStyle = if (isTopThree) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "#${entry.rank}",
                style = textStyle,
                fontWeight = fontWeight,
                color = textColor
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = entry.username,
                style = textStyle,
                fontWeight = fontWeight,
                color = textColor
            )
        }
        Text(
            text = "${entry.score}",
            style = textStyle,
            fontWeight = fontWeight,
            color = textColor
        )
    }
}
