package com.inialpha.plantpoint

import android.location.Location
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.unit.dp

// Phase 4 navigation implementation retained; distance is normalized to Double
// at the calculation boundary because the surrounding planting-navigation model
// uses Double for geographic distances.
