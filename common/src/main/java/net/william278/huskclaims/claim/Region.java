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

import com.google.common.collect.Maps;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.william278.huskclaims.highlighter.Highlightable;
import net.william278.huskclaims.position.BlockPosition;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

import static net.william278.huskclaims.highlighter.Highlightable.HighlightType.getClaimType;

/**
 * A rectangular region defined by two {@link Point} points
 *
 * @see Point
 * @since 1.0
 */
@Getter
@NoArgsConstructor
public class Region {

    @Expose
    @SerializedName("near_corner")
    private Point nearCorner;
    @Expose
    @SerializedName("far_corner")
    private Point farCorner;

    private static final int STEP = 10;

    private Region(@NotNull BlockPosition pos1, @NotNull BlockPosition pos2) {
        final List<Point> corners = getQuadCorners(pos1, pos2);
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
                Point.at(position.getBlockX() - radius, position.getBlockZ() - radius),
                Point.at(position.getBlockX() + radius, position.getBlockZ() + radius)
        );
    }

    /**
     * Get a list of four {@link Point corner points} that form the quad region
     *
     * @param pos1 The first position
     * @param pos2 The second position
     * @return A list of four sorted normalized {@link Point corner points} that form the square region
     * @since 1.0
     */
    @NotNull
    private static List<Point> getQuadCorners(@NotNull BlockPosition pos1, @NotNull BlockPosition pos2) {
        return List.of(
                Point.at(Math.min(pos1.getBlockX(), pos2.getBlockX()), Math.min(pos1.getBlockZ(), pos2.getBlockZ())),
                Point.at(Math.max(pos1.getBlockX(), pos2.getBlockX()), Math.min(pos1.getBlockZ(), pos2.getBlockZ())),
                Point.at(Math.min(pos1.getBlockX(), pos2.getBlockX()), Math.max(pos1.getBlockZ(), pos2.getBlockZ())),
                Point.at(Math.max(pos1.getBlockX(), pos2.getBlockX()), Math.max(pos1.getBlockZ(), pos2.getBlockZ()))
        );
    }

    /**
     * Get a list of the four {@link Point corner points} that form the quad region
     *
     * @return A list of the four {@link Point corner points} that form the quad region
     * @since 1.0
     */
    @NotNull
    public List<Point> getCorners() {
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
     * Returns if this region overlaps with another region
     *
     * @param region The region to check if it overlaps with
     * @return Whether the regions are overlapping
     * @since 1.0
     */
    public boolean overlaps(@NotNull Region region) {
        return (// X overlap
                nearCorner.getBlockX() <= region.getFarCorner().getBlockX() &&
                        farCorner.getBlockX() >= region.getNearCorner().getBlockX()) &&
                // Z overlap
                (nearCorner.getBlockZ() <= region.getFarCorner().getBlockZ() &&
                        farCorner.getBlockZ() >= region.getNearCorner().getBlockZ());
    }

    /**
     * Get the index of the corner that was clicked
     *
     * @param position the position that was clicked
     * @return the index of the corner that was clicked or -1 if no corner was clicked
     * @since 1.0
     */
    public int getClickedCorner(@NotNull Point position) {
        final List<Point> corners = getCorners();
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
    public Region getResized(int cornerIndex, @NotNull Point newCornerPosition) throws IllegalArgumentException {
        if (cornerIndex < 0 || cornerIndex > 3) {
            throw new IllegalArgumentException("Corner index must be between 0 and 3");
        }

        final List<Point> corners = getCorners();
        final Point oldCornerPosition = corners.get(cornerIndex);
        final int widthChange = newCornerPosition.getBlockX() - oldCornerPosition.getBlockX();
        final int heightChange = newCornerPosition.getBlockZ() - oldCornerPosition.getBlockZ();
        return from(
                Point.at(corners.get(0).getBlockX() + (cornerIndex == 0 || cornerIndex == 2 ? widthChange : 0),
                        corners.get(0).getBlockZ() + (cornerIndex == 0 || cornerIndex == 1 ? heightChange : 0)),
                Point.at(corners.get(3).getBlockX() + (cornerIndex == 1 || cornerIndex == 3 ? widthChange : 0),
                        corners.get(3).getBlockZ() + (cornerIndex == 2 || cornerIndex == 3 ? heightChange : 0))
        );
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Region region) {
            return region.nearCorner.equals(nearCorner)
                    && region.farCorner.equals(farCorner);
        }
        return false;
    }

    @NotNull
    public Map<Point, Highlightable.HighlightType> getHighlightPoints(boolean isChild, boolean isAdmin) {
        final Map<Point, Highlightable.HighlightType> positions = Maps.newHashMap();

        // X boundaries
        final Highlightable.HighlightType edge = getClaimType(isChild, isAdmin, false);
        addEdgePoints(positions, edge);

        // Add corners
        final Highlightable.HighlightType corner = getClaimType(isChild, isAdmin, true);
        getCorners().forEach((c) -> positions.put(c, corner));

        return positions;
    }

    private void addEdgePoints(@NotNull Map<Point, Highlightable.HighlightType> positions,
                               @NotNull Highlightable.HighlightType type) {
        // X boundaries
        for (int x = nearCorner.getBlockX() + STEP; x < farCorner.getBlockX(); x += STEP) {
            positions.put(Point.at(x, nearCorner.getBlockZ()), type);
            positions.put(Point.at(x, farCorner.getBlockZ()), type);
        }
        // Z boundaries
        for (int z = nearCorner.getBlockZ() + STEP; z < farCorner.getBlockZ(); z += STEP) {
            positions.put(Point.at(nearCorner.getBlockX(), z), type);
            positions.put(Point.at(farCorner.getBlockX(), z), type);
        }

        // L-shaped corners (Z/X axis)
        if (Math.abs(farCorner.getBlockZ() - nearCorner.getBlockZ()) > 2) {
            positions.put(Point.at(nearCorner.getBlockX(), nearCorner.getBlockZ() + 1), type);
            positions.put(Point.at(farCorner.getBlockX(), nearCorner.getBlockZ() + 1), type);
            positions.put(Point.at(nearCorner.getBlockX(), farCorner.getBlockZ() - 1), type);
            positions.put(Point.at(farCorner.getBlockX(), farCorner.getBlockZ() - 1), type);
        }
        if (Math.abs(farCorner.getBlockX() - nearCorner.getBlockX()) > 2) {
            positions.put(Point.at(nearCorner.getBlockX() + 1, nearCorner.getBlockZ()), type);
            positions.put(Point.at(farCorner.getBlockX() - 1, nearCorner.getBlockZ()), type);
            positions.put(Point.at(nearCorner.getBlockX() + 1, farCorner.getBlockZ()), type);
            positions.put(Point.at(farCorner.getBlockX() - 1, farCorner.getBlockZ()), type);
        }
    }

    /**
     * Returns the length of the smallest edge of this region
     *
     * @return the length of the smallest edge of this region
     * @since 1.0
     */
    public int getShortestEdge() {
        return Math.min(
                Math.abs(farCorner.getBlockX() - nearCorner.getBlockX()),
                Math.abs(farCorner.getBlockZ() - nearCorner.getBlockZ())
        );
    }

    /**
     * Returns the length of the longest edge of this region
     *
     * @return the length of the longest edge of this region
     * @since 1.0
     */
    public int getLongestEdge() {
        return Math.max(
                Math.abs(farCorner.getBlockX() - nearCorner.getBlockX()),
                Math.abs(farCorner.getBlockZ() - nearCorner.getBlockZ())
        );
    }

    /**
     * {@link BlockPosition} implementation representing a (corner) Block of a {@link Region}
     *
     * @since 1.0
     */
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Point implements BlockPosition {

        @Expose
        private int x;
        @Expose
        private int z;

        @NotNull
        public static Point at(int x, int z) {
            return new Point(x, z);
        }

        @NotNull
        public static Point wrap(@NotNull BlockPosition blockPosition) {
            return new Point(blockPosition.getBlockX(), blockPosition.getBlockZ());
        }

        @NotNull
        public Point plus(int x, int z) {
            return new Point(this.x + x, this.z + z);
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
            if (obj instanceof Point point) {
                return point.x == x && point.z == z;
            }
            return false;
        }
    }
}
