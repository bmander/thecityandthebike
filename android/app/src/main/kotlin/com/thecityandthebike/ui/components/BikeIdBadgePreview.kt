package com.thecityandthebike.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.thecityandthebike.ui.theme.TheCityAndTheBikeTheme

@Preview(name = "Light", showBackground = true)
@Composable
private fun BikeIdBadgeLightPreview() {
    TheCityAndTheBikeTheme(darkTheme = false, dynamicColor = false) {
        Surface {
            BadgeSamples()
        }
    }
}

@Preview(name = "Dark", showBackground = true)
@Composable
private fun BikeIdBadgeDarkPreview() {
    TheCityAndTheBikeTheme(darkTheme = true, dynamicColor = false) {
        Surface {
            BadgeSamples()
        }
    }
}

@Composable
private fun BadgeSamples() {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        BikeIdBadge(provider = "citi", bikeQrId = "48302")
        BikeIdBadge(provider = "divvy", bikeQrId = "C00917")
        BikeIdBadge(provider = "lyft", bikeQrId = "A1B2C3D4E5")
        BikeIdBadge(provider = null, bikeQrId = "72419")
    }
}
