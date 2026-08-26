package com.inialpha.plantpoint.data.navigation

import kotlin.math.abs

/**
 * Converts logical planting-grid coordinates (row, column) into geographic points.
 *
 * Convention: the starting point established by the farmer is always (row = 0, column = 0).
 * Increasing row moves geographic NORTH; increasing column moves geographic EAST. This mirrors
 * how the grid is drawn on screen (north-up, row axis vertical, column axis horizontal) so grid-
 * relative directions and compass directions never disagree.
 *
 * Every point is computed directly from the fixed origin (0,0) using [PlantingPointCalculator],
 * not by chaining from the previous point's noisy GPS fix. This keeps the grid's logical shape
 * stable even though smartphone GNSS readings drift from sample to sample.
 */
object PlantingGridEngine {

    /**
     * Geographic coordinate of an arbitrary (row, column) grid cell, anchored at [originLatitude]/
     * [originLongitude] (the row=0, column=0 starting point) and spaced [spacingMeters] apart.
     */
    fun pointFor(
        originLatitude: Double,
        originLongitude: Double,
        spacingMeters: Double,
        row: Int,
        column: Int
    ): Pair<Double, Double> {
        if (row == 0 && column == 0) return originLatitude to originLongitude

        val afterColumn = if (column != 0) {
            val bearing = if (column > 0) CardinalDirection.EAST.bearingDegrees else CardinalDirection.WEST.bearingDegrees
            PlantingPointCalculator.destination(originLatitude, originLongitude, abs(column) * spacingMeters, bearing)
        } else {
            originLatitude to originLongitude
        }

        return if (row != 0) {
            val bearing = if (row > 0) CardinalDirection.NORTH.bearingDegrees else CardinalDirection.SOUTH.bearingDegrees
            PlantingPointCalculator.destination(afterColumn.first, afterColumn.second, abs(row) * spacingMeters, bearing)
        } else {
            afterColumn
        }
    }

    /** The 8 grid cells reachable in one step from ([row], [column]), each tagged with its direction. */
    fun neighbors(row: Int, column: Int): List<GridNeighbor> = listOf(
        GridNeighbor(row + 1, column, CardinalDirection.NORTH),
        GridNeighbor(row + 1, column + 1, CardinalDirection.NORTHEAST),
        GridNeighbor(row, column + 1, CardinalDirection.EAST),
        GridNeighbor(row - 1, column + 1, CardinalDirection.SOUTHEAST),
        GridNeighbor(row - 1, column, CardinalDirection.SOUTH),
        GridNeighbor(row - 1, column - 1, CardinalDirection.SOUTHWEST),
        GridNeighbor(row, column - 1, CardinalDirection.WEST),
        GridNeighbor(row + 1, column - 1, CardinalDirection.NORTHWEST)
    )
}

/** A single grid cell adjacent to a reference cell, and the compass direction to reach it. */
data class GridNeighbor(val row: Int, val column: Int, val direction: CardinalDirection)
