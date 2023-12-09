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
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.william278.huskclaims.position.BlockPosition;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * A rectangular region defined by two {@link Corner} points
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

    private Region(@NotNull BlockPosition pos1, @NotNull BlockPosition pos2) {
        final List<Corner> corners = getQuadCorners(pos1, pos2);
        this.nearCorner = corners.get(0);
        this.farCorner = corners.get(3);
    }

    /**
     * Create a region from two {@link BlockPosition} corner positions
     *
     * @param pos1 The first position
     * @param pos2 The second position
     * @return A {@link Region} defined by the two positions
     * @since 1.0
     */
    @NotNull
    public static Region from(@NotNull BlockPosition pos1, @NotNull BlockPosition pos2) {
        return new Region(pos1, pos2);
    }

    /**
     * Create a region around a {@link BlockPosition} with a given radius
     *
     * @param position The position to create the region around
     * @param radius   The radius of the region
     * @return A {@link Region} defined by the position and radius
     * @since 1.0
     */
    @NotNull
    public static Region around(@NotNull BlockPosition position, int radius) {
        return from(
                Corner.at(position.getBlockX() - radius, position.getBlockZ() - radius),
                Corner.at(position.getBlockX() + radius, position.getBlockZ() + radius)
        );
    }

    /**
     * Get a list of four {@link Corner corner points} that form the quad region
     *
     * @param pos1 The first position
     * @param pos2 The second position
     * @return A list of four sorted normalized {@link Corner corner points} that form the square region
     * @since 1.0
     */
    @NotNull
    private static List<Corner> getQuadCorners(@NotNull BlockPosition pos1, @NotNull BlockPosition pos2) {
        return List.of(
                Corner.at(Math.min(pos1.getBlockX(), pos2.getBlockX()), Math.min(pos1.getBlockZ(), pos2.getBlockZ())),
                Corner.at(Math.max(pos1.getBlockX(), pos2.getBlockX()), Math.min(pos1.getBlockZ(), pos2.getBlockZ())),
                Corner.at(Math.min(pos1.getBlockX(), pos2.getBlockX()), Math.max(pos1.getBlockZ(), pos2.getBlockZ())),
                Corner.at(Math.max(pos1.getBlockX(), pos2.getBlockX()), Math.max(pos1.getBlockZ(), pos2.getBlockZ()))
        );
    }

    /**
     * Get a list of the four {@link Corner corner points} that form the quad region
     *
     * @return A list of the four {@link Corner corner points} that form the quad region
     * @since 1.0
     */
    @NotNull
    public List<Corner> getCorners() {
        return getQuadCorners(nearCorner, farCorner);
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
    public boolean contains(@NotNull BlockPosition position) {
        return position.getBlockX() >= nearCorner.getBlockX() && position.getBlockX() <= farCorner.getBlockX()
                && position.getBlockZ() >= nearCorner.getBlockZ() && position.getBlockZ() <= farCorner.getBlockZ();
    }

    /**
     * Returns if this region fully encloses another region
     *
     * @param region The region to check
     * @return Whether this region fully encloses the other region
     * @since 1.0
     */
    public boolean fullyEncloses(@NotNull Region region) {
        return contains(region.getNearCorner()) && contains(region.getFarCorner());
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

    /**
     * Get the index of the corner that was clicked
     *
     * @param position the position that was clicked
     * @return the index of the corner that was clicked or -1 if no corner was clicked
     * @since 1.0
     */
    public int getClickedCorner(@NotNull Corner position) {
        final List<Corner> corners = getCorners();
        for (int i = 0; i < 4; i++) {
            if (corners.get(i).equals(position)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Get a new {@link Region} that is the result of performing a resize operation of this region,
     * by moving one of its corners
     *
     * @param cornerIndex       index of the corner of this region {@code 0 <= cornerIndex <= 3}
     * @param newCornerPosition the new position of the corner
     * @return the resized region
     * @throws IllegalArgumentException if the corner index is not between 0 and 3
     */
    @NotNull
    public Region getResized(int cornerIndex, @NotNull Corner newCornerPosition) throws IllegalArgumentException {
        if (cornerIndex < 0 || cornerIndex > 3) {
            throw new IllegalArgumentException("Corner index must be between 0 and 3");
        }

        // Get the diagonally opposite corner of the corner being moved
        final List<Corner> corners = getCorners();
        final Corner oppositeCorner = corners.get((cornerIndex + 2) % 4);

        // Get the new region from the new corner position and the opposite corner
        return Region.from(newCornerPosition, oppositeCorner);
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
     * {@link BlockPosition} implementation representing the corner of a {@link Region}
     *
     * @since 1.0
     */
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Corner implements BlockPosition {

        @Expose
        private int x;
        @Expose
        private int z;

        @NotNull
        public static Corner at(int x, int z) {
            return new Corner(x, z);
        }

        @NotNull
        public static Corner wrap(@NotNull BlockPosition blockPosition) {
            return new Corner(blockPosition.getBlockX(), blockPosition.getBlockZ());
        }

        @Override
        public int getBlockX() {
            return x;
        }

        @Override
        public int getBlockZ() {
            return z;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Corner corner) {
                return corner.x == x && corner.z == z;
            }
            return false;
        }
    }
}
