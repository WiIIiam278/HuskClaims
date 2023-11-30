/*
 * This file is part of HuskClaims, licensed under the Apache License 2.0.
 *
 *  Copyright (c) William278 <will27528@gmail.com>
 *  Copyright (c) contributors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package net.william278.huskclaims.claim;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.william278.cloplib.operation.OperationPosition;
import net.william278.huskclaims.position.CoordinatePoint;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * A square region defined by two {@link Corner} points
 *
 * @see Corner
 * @since 1.0
 */
@Getter
@NoArgsConstructor
public class Region {

    @Expose
    @SerializedName("near_corner")
    private Corner nearCorner;
    @Expose
    @SerializedName("far_corner")
    private Corner farCorner;

    private Region(@NotNull CoordinatePoint pos1, @NotNull CoordinatePoint pos2) {
        final List<Corner> corners = getSquareCorners(pos1, pos2);
        this.nearCorner = corners.get(0);
        this.farCorner = corners.get(3);
    }

    /**
     * Get a list of four {@link Corner corner points} that form the square region
     *
     * @param pos1 The first position
     * @param pos2 The second position
     * @return A list of four sorted normalized {@link Corner corner points} that form the square region
     */
    @NotNull
    private static List<Corner> getSquareCorners(@NotNull CoordinatePoint pos1, @NotNull CoordinatePoint pos2) {
        return List.of(
                Corner.at(Math.min(pos1.getBlockX(), pos2.getBlockX()), Math.min(pos1.getBlockZ(), pos2.getBlockZ())),
                Corner.at(Math.max(pos1.getBlockX(), pos2.getBlockX()), Math.min(pos1.getBlockZ(), pos2.getBlockZ())),
                Corner.at(Math.min(pos1.getBlockX(), pos2.getBlockX()), Math.max(pos1.getBlockZ(), pos2.getBlockZ())),
                Corner.at(Math.max(pos1.getBlockX(), pos2.getBlockX()), Math.max(pos1.getBlockZ(), pos2.getBlockZ()))
        );
    }

    /**
     * Get the surface area of this region
     *
     * @return The surface area of this region
     * @since 1.0
     */
    public int getSurfaceArea() {
        return nearCorner.getSurfaceArea(farCorner);
    }

    /**
     * Returns if this region contains a position
     *
     * @param position The position to check
     * @return Whether the position is contained in this region
     * @since 1.0
     */
    public boolean contains(@NotNull CoordinatePoint position) {
        return position.getBlockX() >= nearCorner.getBlockX() && position.getBlockX() <= farCorner.getBlockX()
                && position.getBlockZ() >= nearCorner.getBlockZ() && position.getBlockZ() <= farCorner.getBlockZ();
    }

    /**
     * Returns if this region intersects with another region
     *
     * @param region The region to check
     * @return Whether the regions intersect
     * @since 1.0
     */
    public boolean intersects(@NotNull Region region) {
        return region.contains(nearCorner) || region.contains(farCorner)
                || contains(region.getNearCorner()) || contains(region.getFarCorner());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Region region) {
            return region.nearCorner.equals(nearCorner)
                    && region.farCorner.equals(farCorner);
        }
        return false;
    }

    /**
     * {@link CoordinatePoint} implementation representing the corner of a {@link Region}
     *
     * @since 1.0
     */
    public static class Corner implements CoordinatePoint {

        @Expose
        private int x;
        @Expose
        private int z;

        @SuppressWarnings("unused")
        private Corner() {
        }

        private Corner(int x, int z) {
            this.x = x;
            this.z = z;
        }

        @NotNull
        public static Corner at(int x, int z) {
            return new Corner(x, z);
        }

        @NotNull
        public static Corner wrap(@NotNull OperationPosition position) {
            return at((int) position.getX(), (int) position.getZ());
        }

        @Override
        public int getBlockX() {
            return x;
        }

        @Override
        public int getBlockZ() {
            return z;
        }
    }
}
