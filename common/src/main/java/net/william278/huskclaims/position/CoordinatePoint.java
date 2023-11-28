package net.william278.huskclaims.position;

import org.jetbrains.annotations.NotNull;

public interface CoordinatePoint {

    int getBlockX();

    int getBlockZ();

    default int getSurfaceArea(@NotNull CoordinatePoint other) {
        return Math.abs(getBlockX() - other.getBlockX()) * Math.abs(getBlockZ() - other.getBlockZ());
    }

}
