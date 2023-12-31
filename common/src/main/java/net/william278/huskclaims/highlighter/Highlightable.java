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

package net.william278.huskclaims.highlighter;

import net.william278.huskclaims.claim.ClaimWorld;
import net.william278.huskclaims.claim.Region;
import net.william278.huskclaims.position.BlockPosition;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

/**
 * Represents an object that can be highlighted
 */
public interface Highlightable {

    /**
     * Get the positions to be highlighted
     *
     * @param world       the {@link ClaimWorld} to get the positions for
     * @param showOverlap whether to show overlap
     * @param viewer      the {@link BlockPosition} of the viewer
     * @param range       the range to get the positions for
     * @return a {@link List} of {@link BlockPosition}s
     * @since 1.0
     */
    @NotNull
    Map<Region.Point, Type> getHighlightPoints(@NotNull ClaimWorld world, boolean showOverlap,
                                               @NotNull BlockPosition viewer, long range);

    /**
     * Represents different types of highlight node blocks
     *
     * @since 1.0
     */
    enum Type {
        CORNER,
        EDGE,
        CHILD_CORNER,
        CHILD_EDGE,
        ADMIN_CORNER,
        ADMIN_EDGE,
        OVERLAP_CORNER, // Used when highlighting overlapping claims
        OVERLAP_EDGE,
        SELECTION; // Used when selecting the first corner of a claim region

        @NotNull
        public static Highlightable.Type getClaimType(boolean overlap, boolean child, boolean admin, boolean corner) {
            if (overlap) {
                return corner ? OVERLAP_CORNER : OVERLAP_EDGE;
            }
            if (child) {
                return corner ? CHILD_CORNER : CHILD_EDGE;
            }
            if (admin) {
                return corner ? ADMIN_CORNER : ADMIN_EDGE;
            }
            return corner ? CORNER : EDGE;
        }
    }

}
