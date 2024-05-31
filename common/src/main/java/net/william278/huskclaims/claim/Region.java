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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.william278.huskclaims.highlighter.Highlightable;
import net.william278.huskclaims.position.BlockPosition;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static net.william278.huskclaims.highlighter.Highlightable.Type.getClaimType;

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
     * Get the center of this region
     *
     * @return The center of this region
     * @since 1.0
     */
    @NotNull
    public Point getCenter() {
        return Point.at(
                (nearCorner.getBlockX() + farCorner.getBlockX()) / 2,
                (nearCorner.getBlockZ() + farCorner.getBlockZ()) / 2
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

    @NotNull
    public Region getResized(int extendNorth, int extendEast, int extendSouth, int extendWest) {
        return from(
                // Near corner: closest to 0, 0 (modify west/north)
                Point.at(nearCorner.getBlockX() - extendWest, nearCorner.getBlockZ() - extendNorth),
                // Far corner: furthest from 0, 0 (modify east/south)
                Point.at(farCorner.getBlockX() + extendSouth, farCorner.getBlockZ() + extendEast)
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
    public Map<Point, Highlightable.Type> getHighlightPoints(boolean overlap, boolean isChild, boolean isAdmin,
                                                             @NotNull BlockPosition viewer, long range) {
        final Map<Point, Highlightable.Type> positions = Maps.newHashMap();

        // Add border points
        final Highlightable.Type border = getClaimType(overlap, isChild, isAdmin, false);
        addEdgePoints(positions, border, viewer, range);
        addLShapedCorners(positions, border, viewer, range);

        // Add corner points
        final Highlightable.Type corner = getClaimType(overlap, isChild, isAdmin, true);
        getCorners().stream().filter(c -> c.distanceFrom(viewer) <= range).forEach((c) -> positions.put(c, corner));

        return positions;
    }

    // X and Z edges
    private void addEdgePoints(@NotNull Map<Point, Highlightable.Type> positions, @NotNull Highlightable.Type type,
                               @NotNull BlockPosition viewer, long range) {
        final List<Point> edgePoints = Lists.newArrayList();
        // X edges
        for (int x = nearCorner.getBlockX() + STEP; x < farCorner.getBlockX(); x += STEP) {
            edgePoints.add(Point.at(x, nearCorner.getBlockZ()));
            edgePoints.add(Point.at(x, farCorner.getBlockZ()));
        }

        // Z edges
        for (int z = nearCorner.getBlockZ() + STEP; z < farCorner.getBlockZ(); z += STEP) {
            edgePoints.add(Point.at(nearCorner.getBlockX(), z));
            edgePoints.add(Point.at(farCorner.getBlockX(), z));
        }
        edgePoints.stream().filter(c -> c.distanceFrom(viewer) <= range).forEach((c) -> positions.put(c, type));
    }

    // L-shaped corners
    private void addLShapedCorners(@NotNull Map<Point, Highlightable.Type> positions, @NotNull Highlightable.Type type,
                                   @NotNull BlockPosition viewer, long range) {
        final List<Point> cornerPoints = Lists.newArrayList();
        if (Math.abs(farCorner.getBlockZ() - nearCorner.getBlockZ()) > 2) {
            cornerPoints.add(Point.at(nearCorner.getBlockX(), nearCorner.getBlockZ() + 1));
            cornerPoints.add(Point.at(farCorner.getBlockX(), nearCorner.getBlockZ() + 1));
            cornerPoints.add(Point.at(nearCorner.getBlockX(), farCorner.getBlockZ() - 1));
            cornerPoints.add(Point.at(farCorner.getBlockX(), farCorner.getBlockZ() - 1));
        }
        if (Math.abs(farCorner.getBlockX() - nearCorner.getBlockX()) > 2) {
            cornerPoints.add(Point.at(nearCorner.getBlockX() + 1, nearCorner.getBlockZ()));
            cornerPoints.add(Point.at(farCorner.getBlockX() - 1, nearCorner.getBlockZ()));
            cornerPoints.add(Point.at(nearCorner.getBlockX() + 1, farCorner.getBlockZ()));
            cornerPoints.add(Point.at(farCorner.getBlockX() - 1, farCorner.getBlockZ()));
        }
        cornerPoints.stream().filter(c -> c.distanceFrom(viewer) <= range).forEach((c) -> positions.put(c, type));
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
     * Returns a list of integer arrays representing the chunks within the region.
     *
     * @return The list of integer arrays representing the chunks
     */
    @NotNull
    public Set<int[]> getChunks() {
        final int chunkX1 = getNearCorner().getBlockX() >> 4;
        final int chunkZ1 = getNearCorner().getBlockZ() >> 4;
        final int chunkX2 = getFarCorner().getBlockX() >> 4;
        final int chunkZ2 = getFarCorner().getBlockZ() >> 4;
        final int chunkXMin = Math.min(chunkX1, chunkX2);
        final int chunkXMax = Math.max(chunkX1, chunkX2);
        final int chunkZMin = Math.min(chunkZ1, chunkZ2);
        final int chunkZMax = Math.max(chunkZ1, chunkZ2);

        final Set<int[]> chunks = new HashSet<>();
        for (int x = chunkXMin; x <= chunkXMax; x++) {
            for (int z = chunkZMin; z <= chunkZMax; z++) {
                chunks.add(new int[]{x, z});
            }
        }

        return chunks;
    }

    /**
     * {@link BlockPosition} implementation representing a (corner) Block of a {@link Region}
     *
     * @since 1.0
     */
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Point implements BlockPosition {

        // We restrict the distance to +/-32000000 blocks (maximum Minecraft world size) to prevent abuse
        private static final int RANGE = 32000000;

        @Expose
        private int x;
        @Expose
        private int z;

        private Point(int x, int z) {
            if (Math.abs(x) > RANGE || Math.abs(z) > RANGE) {
                throw new IllegalArgumentException("Point coords (%s, %s) out of range (+/-%s)".formatted(x, z, RANGE));
            }
            this.x = x;
            this.z = z;
        }

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
        @Range(from = -RANGE, to = RANGE)
        public int getBlockX() {
            return x;
        }

        @Override
        @Range(from = -RANGE, to = RANGE)
        public int getBlockZ() {
            return z;
        }

        @Override
        public long getLongChunkCoords() {
            final int chunkX = x >> 4;
            final int chunkZ = z >> 4;
            return ((long) chunkX << 32) | (chunkZ & 0xffffffffL);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Point point) {
                return point.x == x && point.z == z;
            }
            return false;
        }

        // Check if a point, when modified by numberBy on either axis, is out of the supported range
        public static boolean isOutOfRange(@NotNull BlockPosition position, int dx, int dz) {
            return Math.abs(position.getBlockX()) + Math.abs(dx) > RANGE
                    || Math.abs(position.getBlockZ()) + Math.abs(dz) > RANGE;
        }

    }
}
