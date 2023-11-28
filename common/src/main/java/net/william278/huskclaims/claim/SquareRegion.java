package net.william278.huskclaims.claim;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import net.william278.huskclaims.position.CoordinatePoint;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SquareRegion {

    @Expose
    @SerializedName("near_corner")
    private ClaimCorner nearCorner;
    @Expose
    @SerializedName("far_corner")
    private ClaimCorner farCorner;

    private SquareRegion(@NotNull CoordinatePoint pos1, @NotNull CoordinatePoint pos2) {
        final List<ClaimCorner> corners = getSortedCorners(pos1, pos2);
        this.nearCorner = corners.get(0);
        this.farCorner = corners.get(3);
    }

    @NotNull
    private static List<ClaimCorner> getSortedCorners(@NotNull CoordinatePoint pos1, @NotNull CoordinatePoint pos2) {
        return List.of(
                ClaimCorner.at(Math.min(pos1.getBlockX(), pos2.getBlockX()), Math.min(pos1.getBlockZ(), pos2.getBlockZ())),
                ClaimCorner.at(Math.max(pos1.getBlockX(), pos2.getBlockX()), Math.min(pos1.getBlockZ(), pos2.getBlockZ())),
                ClaimCorner.at(Math.min(pos1.getBlockX(), pos2.getBlockX()), Math.max(pos1.getBlockZ(), pos2.getBlockZ())),
                ClaimCorner.at(Math.max(pos1.getBlockX(), pos2.getBlockX()), Math.max(pos1.getBlockZ(), pos2.getBlockZ()))
        );
    }

    @NotNull
    public ClaimCorner getNearCorner() {
        return nearCorner;
    }

    @NotNull
    public ClaimCorner getFarCorner() {
        return farCorner;
    }

    public int getSurfaceArea() {
        return nearCorner.getSurfaceArea(farCorner);
    }

    public boolean contains(@NotNull CoordinatePoint position) {
        return position.getBlockX() >= nearCorner.getBlockX() && position.getBlockX() <= farCorner.getBlockX()
                && position.getBlockZ() >= nearCorner.getBlockZ() && position.getBlockZ() <= farCorner.getBlockZ();
    }

    public boolean overlaps(@NotNull SquareRegion region) {
        // Returns if this claim overlaps with another claim, by its near and far corners
        // todo
        return false;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof SquareRegion region) {
            return region.nearCorner.equals(nearCorner)
                    && region.farCorner.equals(farCorner);
        }
        return false;
    }

}
