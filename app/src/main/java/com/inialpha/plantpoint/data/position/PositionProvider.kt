package com.inialpha.plantpoint.data.position

import com.inialpha.plantpoint.data.location.LocationSnapshot
import com.inialpha.plantpoint.data.sensors.OrientationSnapshot
import kotlinx.coroutines.flow.StateFlow

/**
 * Abstraction over whatever supplies position + heading to the planting grid.
 *
 * Today [SmartphonePositionProvider] backs this with the phone's own GNSS chip and
 * rotation-vector sensor. The planting grid, navigation, and persistence layers only
 * depend on this interface, so a future high-precision source (external RTK/GNSS
 * receiver, Bluetooth measuring device, UWB anchor, etc.) can implement the same
 * contract and be swapped in without changes to the grid or UI code.
 */
interface PositionProvider {
    /** Latest known position, or null if none has been obtained yet. */
    val location: StateFlow<LocationSnapshot?>

    /** Latest known device/receiver heading, or null if unavailable. */
    val orientation: StateFlow<OrientationSnapshot?>

    /** Whether the underlying positioning source is currently enabled/reachable. */
    val isAvailable: StateFlow<Boolean>

    /** Whether a heading source is present at all (independent of whether it has reported yet). */
    val hasHeadingSensor: Boolean

    fun start()
    fun stop()
    fun isLocationEnabled(): Boolean
}
