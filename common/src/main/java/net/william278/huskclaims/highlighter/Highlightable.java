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

import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.claim.ClaimWorld;
import net.william278.huskclaims.claim.Region;
import net.william278.huskclaims.position.BlockPosition;
import net.william278.huskclaims.position.Position;
import net.william278.huskclaims.util.BlockProvider;
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
     * @return a {@link List} of {@link BlockPosition}s
     * @since 1.0
     */
    @NotNull
    Map<Region.Point, HighlightType> getHighlightPoints(@NotNull ClaimWorld world, boolean showOverlap);

    @NotNull
    default BlockProvider.MaterialBlock getBlockFor(@NotNull ClaimWorld world, @NotNull Position position,
                                                    @NotNull HuskClaims plugin, boolean showOverlap) {
        return plugin.getBlockFor(plugin.getSettings()
                .getClaims().getBlockHighlighterTypes()
                .getOrDefault(
                        getHighlightPoints(world, showOverlap).entrySet().stream()
                                .filter(e -> e.getKey().equals(Region.Point.wrap(position))).map(Map.Entry::getValue)
                                .findFirst().orElse(HighlightType.SELECTION),
                        "minecraft:yellow_concrete"
                ));
    }

    enum HighlightType {
        CORNER,
        EDGE,
        CHILD_CORNER,
        CHILD_EDGE,
        ADMIN_CORNER,
        ADMIN_EDGE,
        OVERLAP_CORNER,
        OVERLAP_EDGE,
        SELECTION;

        @NotNull
        public static HighlightType getClaimType(boolean overlap, boolean child, boolean admin, boolean corner) {
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
